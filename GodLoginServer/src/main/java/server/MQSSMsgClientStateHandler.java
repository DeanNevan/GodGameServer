package server;

import com.google.protobuf.InvalidProtocolBufferException;
import connection.activemq.MQConsumerTool;
import connection.activemq.MQMessageParser;
import connection.activemq.MQProducerTool;
import protobuf.SSMessage;
import protobuf.SSMessageClientState;
import protobuf.SSMessageClientStateClose;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class MQSSMsgClientStateHandler extends MsgHandler {
    private volatile static MQSSMsgClientStateHandler singleton;
    public static MQSSMsgClientStateHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSSMsgClientStateHandler.class) {
                if (singleton == null) {
                    singleton = new MQSSMsgClientStateHandler();
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
            MQConsumerTool.getInstance().addQueueOrTopic("SSMessage_ClientState", 0, new MessageListener() {
                        public void onMessage(Message message) {
                            handle(message);
                        }
                    }, String.format("target_server_id='%s' or target_server_id='%s'", server.getServerID(), "anyone")
            );
            MQProducerTool.getInstance().addDestination("SSMessage_ClientState", 0, 1);
        }
        catch (JMSException e) {
            server.logger.error(e.toString());
        }
    }

    private MQSSMsgClientStateHandler(){
        msgHandlerName = "客户端状态处理器";
    }

    public void handle(Message msg) {
        if (msg instanceof BytesMessage) {
            try {
                BytesMessage byteMsg = (BytesMessage) msg;
                SSMessage.Request request = (SSMessage.Request) MQMessageParser.parseMessageToProtobuf(byteMsg, SSMessage.Request.parser());
                assert request != null;
                server.logger.debug(String.format("服务器ID:%s %s 处理消息：%s", server.getServerID(), msgHandlerName, request.toString()));
                SSMessageClientState.Request clientStateRequest = SSMessageClientState.Request.parseFrom(request.getContent());
                String clientID = clientStateRequest.getClientId();
                switch (clientStateRequest.getType()){
                    case CLOSE:
                        SSMessageClientStateClose.Msg clientStateClose = SSMessageClientStateClose.Msg.parseFrom(clientStateRequest.getContent());
                        switch (clientStateClose.getType()){
                            case NORMAL:
                                break;
                            case HEART_BEAT_TIMEOUT:
                                break;
                            case FORCE:
                                break;
                        }
                        RedisLoginClientsWorker.getSingleton().removeLoginClient(clientID);
                        break;
                    case UNKNOWN:
                        break;
                }

            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }





}
