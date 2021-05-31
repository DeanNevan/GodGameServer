package server;

import com.google.protobuf.InvalidProtocolBufferException;
import connection.activemq.MQConsumerTool;
import connection.activemq.MQMessageParser;
import connection.activemq.MQProducerTool;
import protobuf.*;
import client.LoginClient;
import db.LoginMysqlWorker;
import util.PasswordTool;
import util.RandomCodeGenerator;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class MQSCMsgReqLoginHandler extends MsgHandler {
    private volatile static MQSCMsgReqLoginHandler singleton;
    public static MQSCMsgReqLoginHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSCMsgReqLoginHandler.class) {
                if (singleton == null) {
                    singleton = new MQSCMsgReqLoginHandler();
                }
            }
        }
        return singleton;
    }

    Server server;

    public void init(Server server){
        this.server = server;
        server.logger.debug(String.format("服务器ID:%s %s 初始化", server.getServerID(), msgHandlerName));
        try {
            MQConsumerTool.getInstance().addQueueOrTopic("SCMessageRequest_Login", 1, new MessageListener() {
                        public void onMessage(Message message) {
                            handle(message);
                        }
                    }, String.format("target_server_id='%s' or target_server_id='%s'", server.getServerID(), "anyone")
            );
            MQProducerTool.getInstance().addDestination("SCMessageResponse", 1, 1);
        }
        catch (JMSException e) {
            server.logger.error(e.toString());
        }
    }

    private MQSCMsgReqLoginHandler(){
        msgHandlerName = "登录请求处理器";
    }

    public void handle(Message msg) {
        if (msg instanceof BytesMessage) {
            BytesMessage byteMsg = (BytesMessage)msg;
            SCMessage.Request request = (SCMessage.Request) MQMessageParser.parseMessageToProtobuf(byteMsg, SCMessage.Request.parser());
            assert request != null;
            server.logger.debug(String.format("服务器ID:%s %s 处理消息：%s", server.getServerID(), msgHandlerName, request.toString()));
            SCMessageLogin.Request loginRequest = null;
            try {
                loginRequest = SCMessageLogin.Request.parseFrom(request.getContent());
            } catch (InvalidProtocolBufferException e) {
                server.logger.error(String.valueOf(e));
            }

            LoginClient loginClient;
            SCMessageLogin.Response.Builder loginResponseBuilder;
            SCMessage.Response.Builder responseBuilder = SCMessage.Response.newBuilder();
            String verificationCode = "1111";
            byte[] decodedBase64;
            byte[] decryptedMD5Password;
            int DBResuslt;

            switch (loginRequest.getDataType()){
                case PRE_REGISTER:
                    SCMessageLogin.RequestPreRegister requestPreRegister = loginRequest.getRequestPreRegister();

                    loginClient = new LoginClient(request.getClientId(), request.getGateServerId());

                    loginClient.setUserName(requestPreRegister.getUserName());
                    loginClient.setIvAscii(requestPreRegister.getIvAscii());
                    verificationCode = RandomCodeGenerator.generate(LoginServerConfig.VERIFICATION_CODE_LENGTH);
                    loginClient.preRegister(verificationCode);


                    server.logger.debug(String.format("登录实体id:%s 用户请求注册 用户名:%s，验证码:%s", loginClient.getId(), loginClient.getUserName(), loginClient.getVerificationCode()));

                    loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                    loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.PRE_REGISTER);
                    SCMessageLogin.ResponsePreRegister.Builder responsePreRegisterBuilder = SCMessageLogin.ResponsePreRegister.newBuilder();
                    responsePreRegisterBuilder.setResult(true).setWords("开始注册").setVerificationCode(verificationCode);
                    loginResponseBuilder.setResponsePreRegister(responsePreRegisterBuilder.build());

                    break;

                case REGISTER:
                    SCMessageLogin.RequestRegister requestRegister = loginRequest.getRequestRegister();

                    loginClient = RedisLoginClientsWorker.getSingleton().getLoginClientViaID(request.getClientId());
                    if (loginClient == null){
                        server.logger.debug(String.format("错误：用户注册但用户实体不存在或错误id"));
                        return;
                    }
                    if (loginClient.getLoginState() != LoginClient.LOGIN_STATE.PRE_REGISTER) {
                        server.logger.debug(String.format("登录实体id:%s 错误：用户注册但用户实体状态异常", loginClient.getId()));
                        return;
                    }
                    loginClient.setUserName(requestRegister.getUserName());
                    loginClient.register(requestRegister.getUserEncryptedPassword());
                    loginClient.setDeviceID(requestRegister.getUserDevideId());

                    server.logger.debug(String.format("登录实体id:%s 用户注册", loginClient.getId()));
                    server.logger.debug(String.format("登录实体id:%s 用户名:%s", loginClient.getId(), loginClient.getUserName()));

                    decryptedMD5Password = PasswordTool.decryptPassword(loginClient.getEncryptedPassword(), loginClient.getVerificationCode(), loginClient.getIvAscii());

                    String randomCode = RandomCodeGenerator.generate(LoginServerConfig.RANDOM_CODE_LENGTH);

                    String encryptedFinalResult = PasswordTool.encryptWithRandomCode(decryptedMD5Password, randomCode);
                    //String encryptedStringFinalResultBase64 = Base64.getEncoder().encodeToString(encryptedFinalResult.getBytes(StandardCharsets.UTF_8));

                    DBResuslt = LoginMysqlWorker.getSingleton().register(loginClient.getUserName(), randomCode, encryptedFinalResult, requestRegister.getUserDevideId());

                    if (DBResuslt < 0){
                        loginClient.preRegister(loginClient.getVerificationCode());
                        server.logger.debug(String.format("登录实体id:%s 错误：用户注册失败，错误代码：%s", loginClient.getId(), DBResuslt));

                        loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                        loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.REGISTER);
                        SCMessageLogin.ResponseRegister.Builder responseRegisterBuilder = SCMessageLogin.ResponseRegister.newBuilder();
                        responseRegisterBuilder.setResult(false).setWords(String.format("注册失败，代码：%s", DBResuslt));
                        loginResponseBuilder.setResponseRegister(responseRegisterBuilder.build());
                    }
                    else{
                        server.logger.debug(String.format("登录实体id:%s 用户注册成功", loginClient.getId()));

                        loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                        loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.REGISTER);
                        SCMessageLogin.ResponseRegister.Builder responseRegisterBuilder = SCMessageLogin.ResponseRegister.newBuilder();
                        responseRegisterBuilder.setResult(true).setWords("注册成功");
                        loginResponseBuilder.setResponseRegister(responseRegisterBuilder.build());
                    }
                    break;

                case PRE_LOGIN:
                    SCMessageLogin.RequestPreLogin requestPreLogin = loginRequest.getRequestPreLogin();

                    loginClient = new LoginClient(request.getClientId(), request.getGateServerId());
//                    if (userLoginClient == null){
//                        server.logger.debug(String.format("错误：用户请求登录但用户实体不存在"));
//                        return;
//                    }
                    loginClient.setUserName(requestPreLogin.getUserName());
                    loginClient.setIvAscii(requestPreLogin.getIvAscii());
                    verificationCode = RandomCodeGenerator.generate(LoginServerConfig.VERIFICATION_CODE_LENGTH);
                    loginClient.preLogin(verificationCode);

                    server.logger.debug(String.format("登录实体id:%s 用户请求登录 用户名:%s，验证码:%s", loginClient.getId(), loginClient.getUserName(), loginClient.getVerificationCode()));

                    loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                    loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.PRE_LOGIN);
                    SCMessageLogin.ResponsePreLogin.Builder responsePreLoginBuilder = SCMessageLogin.ResponsePreLogin.newBuilder();
                    responsePreLoginBuilder.setResult(true).setWords("开始登录").setVerificationCode(verificationCode);
                    loginResponseBuilder.setResponsePreLogin(responsePreLoginBuilder.build());
                    break;

                case LOGIN:
                    SCMessageLogin.RequestLogin requestLogin = loginRequest.getRequestLogin();

                    loginClient = RedisLoginClientsWorker.getSingleton().getLoginClientViaID(request.getClientId());
                    if (loginClient == null){
                        server.logger.debug(String.format("错误：用户登录但用户实体不存在或错误id"));
                        return;
                    }
                    if (loginClient.getLoginState() != LoginClient.LOGIN_STATE.PRE_LOGIN) {
                        server.logger.debug(String.format("登录实体id:%s 错误：用户登录但用户实体状态异常", loginClient.getId()));
                        return;
                    }
                    loginClient.setUserName(requestLogin.getUserName());
                    loginClient.login(requestLogin.getUserEncryptedPassword());
                    loginClient.setDeviceID(requestLogin.getUserDevideId());

                    server.logger.debug(String.format("登录实体id:%s 用户登录 用户名:%s", loginClient.getId(), loginClient.getUserName()));

                    decryptedMD5Password = PasswordTool.decryptPassword(loginClient.getEncryptedPassword(), loginClient.getVerificationCode(), loginClient.getIvAscii());

                    DBResuslt = LoginMysqlWorker.getSingleton().login(loginClient.getUserName(), decryptedMD5Password, loginClient.getDeviceID());

                    if (DBResuslt < 0){
                        loginClient.preLogin(loginClient.getVerificationCode());
                        server.logger.debug(String.format("登录实体id:%s 错误：用户登录失败，错误代码：%s", loginClient.getId(), DBResuslt));

                        loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                        loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.LOGIN);
                        SCMessageLogin.ResponseLogin.Builder responseLoginBuilder = SCMessageLogin.ResponseLogin.newBuilder();
                        responseLoginBuilder.setResult(false).setWords(String.format("登录失败，代码：%s", DBResuslt));
                        loginResponseBuilder.setResponseLogin(responseLoginBuilder.build());
                    }
                    else{
                        server.logger.debug(String.format("登录实体id:%s 用户登录成功", loginClient.getId()));

                        loginResponseBuilder = SCMessageLogin.Response.newBuilder();
                        loginResponseBuilder.setDataType(SCMessageLogin.Response.ResponseType.LOGIN);
                        SCMessageLogin.ResponseLogin.Builder responseLoginBuilder = SCMessageLogin.ResponseLogin.newBuilder();
                        responseLoginBuilder.setResult(true).setWords("登录成功");
                        loginResponseBuilder.setResponseLogin(responseLoginBuilder.build());

                        loginClientAuthenticated(loginClient);
                        RedisLoginClientsWorker.getSingleton().removeLoginClient(loginClient);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + loginRequest.getDataType());
            }

            assert loginResponseBuilder != null;
            assert loginClient != null;
            RedisLoginClientsWorker.getSingleton().updateLoginClient(loginClient);

            SCMessageLogin.Response tempResponse = loginResponseBuilder.build();
            responseBuilder.setContent(tempResponse.toByteString());
            responseBuilder.setType(SCMessage.Type.LOGIN);
            responseBuilder.setClientId(request.getClientId());
            responseBuilder.setGateServerId(request.getGateServerId());
            responseBuilder.addAllPassedServersId(request.getPassedServersIdList());
            responseBuilder.addPassedServersId(server.getServerID());

            try {
                MQProducerTool.getInstance().sendBuilder("SCMessageResponse", responseBuilder, "target_server_id", request.getGateServerId());
            } catch (Exception e) {
                server.logger.error(e.toString());
            }

        } else {
            server.logger.debug("Consumer received:" + msg.toString());
        }
    }

    public void loginClientAuthenticated(LoginClient loginClient){
        loginClient.setAuthenticated(true);
        SSMessage.Request.Builder builder1 = SSMessage.Request.newBuilder();
        builder1.addPassedServersId(server.getServerID());
        builder1.setType(SSMessage.Type.CLIENT_STATE_CHANGED);
        builder1.setServerFromId(server.getServerID());


        SSMessageClientState.Request.Builder builder2 = SSMessageClientState.Request.newBuilder();
        builder2.setClientId(loginClient.getId());
        builder2.setType(SSMessageClientState.Type.AUTH);

        SSMessageClientStateAuth.Msg.Builder builder3 = SSMessageClientStateAuth.Msg.newBuilder();
        builder3.setType(SSMessageClientStateAuth.Type.SUCCESS);
        builder3.setUserName(loginClient.getUserName());

        builder2.setContent(builder3.build().toByteString());
        builder1.setContent(builder2.build().toByteString());

        builder1.setTimestamp(System.currentTimeMillis());

        try {
            MQProducerTool.getInstance().sendBuilder("SSMessage_ClientState", builder1, "target_server_id", loginClient.getGateServerID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
