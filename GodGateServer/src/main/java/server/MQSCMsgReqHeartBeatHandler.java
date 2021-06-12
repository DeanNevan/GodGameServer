package server;

import client.GateServerClient;
import client.GateServerClientHeartBeatManager;
import client.pool.GateServerClientPool;
import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import protobuf.SCMessage;
import connection.activemq.MQMessageParser;

import javax.jms.*;

public class MQSCMsgReqHeartBeatHandler extends MsgHandler{
    private volatile static MQSCMsgReqHeartBeatHandler singleton;
    public static MQSCMsgReqHeartBeatHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSCMsgReqHeartBeatHandler.class) {
                if (singleton == null) {
                    singleton = new MQSCMsgReqHeartBeatHandler();
                }
            }
        }
        return singleton;
    }

    private Server server;

    public void init(Server server){
        this.server = server;
        server.logger.debug(String.format("服务器ID:%s %s 初始化", server.getServerID(), msgHandlerName));
        try {
//            ConsumerTool.getInstance().addQueueOrTopic("SCMessageRequest_HeartBeat", 0, new MessageListener() {
//                    public void onMessage(Message message) {
//                        handle(message);
//                    }
//                }, String.format("target_server_id='%s' or target_server_id='%s'", server.getServerID(), "anyone")
//            );
            MQProducerTool.getInstance().addDestination("SCMessageResponse", 1, 1);
        }
        catch (JMSException e) {
            GateServer.getSingleton().logger.error(e.toString());
        }
    }

    private MQSCMsgReqHeartBeatHandler(){
        msgHandlerName = "心跳处理器";
    }

//    public void handle(Message msg) {
//        if (msg instanceof BytesMessage) {
//            BytesMessage byteMsg = (BytesMessage)msg;
//            SCMessage.Request request = (SCMessage.Request) MessageParser.parseMessageToProtobuf(byteMsg, SCMessage.Request.parser());
//            assert request != null;
//            GateServer.getSingleton().logger.debug(String.format("服务器ID:%s %s 处理消息：%s", server.getServerID(), msgHandlerName, request.toString()));
//            if (request.getTip().equals("ping")){
//                String clientId = request.getClientId();
//                GateServerClient gateServerClient = GateServerClientPool.getSingleton().getClientViaID(clientId);
//                GateServerClientHeartBeatManager.getSingleton().clientHeartBeat(clientId);
//
//                SCMessage.Response.Builder responseBuilder = SCMessage.Response.newBuilder();
//                responseBuilder.setClientId(clientId);
//                responseBuilder.setGateServerId(request.getGateServerId());
//                responseBuilder.setType(SCMessage.Type.HEART_BEAT);
//                responseBuilder.setTip("pong");
//
//                SCMessage.Response response = responseBuilder.build();
//
//                try {
//                    ProducerTool.getInstance().sendBuilder("SCMessageResponse", response.toBuilder(), "target_server_id", request.getGateServerId());
//                } catch (Exception e) {
//                    GateServer.getSingleton().logger.error(e.toString());
//                }
//            }
//        } else {
//            System.out.println("Consumer:->Received: " + msg);
//        }
//    }

    public void response(SCMessage.Request request) {
        server.logger.debug(String.format("服务器ID:%s %s 处理消息：%s", server.getServerID(), msgHandlerName, request.toString()));
        if (request.getTip().equals("ping")){
            String clientId = request.getClientId();
            GateServerClient gateServerClient = GateServerClientPool.getSingleton().getClientViaID(clientId);
            GateServerClientHeartBeatManager.getSingleton().clientHeartBeat(clientId);

            SCMessage.Response.Builder responseBuilder = SCMessage.Response.newBuilder();
            responseBuilder.addPassedServersId(server.getServerID());
            responseBuilder.setClientId(clientId);
            responseBuilder.setGateServerId(request.getGateServerId());
            responseBuilder.setType(SCMessage.Type.HEART_BEAT);
            responseBuilder.setTip("pong");
            responseBuilder.setTimestamp(System.currentTimeMillis());
            responseBuilder.setRequestId(request.getRequestId());

            //SCMessage.Response response = responseBuilder.build();

            try {
                MQProducerTool.getInstance().sendBuilder("SCMessageResponse", responseBuilder, "target_server_id", request.getGateServerId());
            } catch (Exception e) {
                GateServer.getSingleton().logger.error(e.toString());
            }
        }
    }

}
