package server;

import client.GateServerClient;
import client.pool.GateServerClientPool;
import connection.activemq.MQConsumerTool;
import org.apache.activemq.command.ActiveMQBytesMessage;
import protobuf.SCMessage;
import connection.activemq.MQMessageParser;
import util.ResponseWriter;

import javax.jms.*;

public class MQSCMsgResHandler extends MsgHandler{
    private volatile static MQSCMsgResHandler singleton;
    public static MQSCMsgResHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSCMsgResHandler.class) {
                if (singleton == null) {
                    singleton = new MQSCMsgResHandler();
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
            MQConsumerTool.getInstance().addQueueOrTopic("SCMessageResponse", 1, new MessageListener() {
                        public void onMessage(Message message) {
                            //System.out.println(handledCommandsID.size());
                            handle(message);
                        }
                    }, String.format("target_server_id='%s' or target_server_id='%s'", server.getServerID(), "anyone")
            );
        }
        catch (JMSException e) {
            server.logger.error(e.toString());
        }
    }

    public MQSCMsgResHandler(){
        msgHandlerName = "回复处理器";
    }

    public void handle(Message msg) {
        if (msg instanceof ActiveMQBytesMessage) {
            ActiveMQBytesMessage bytesMsg = (ActiveMQBytesMessage) msg;

            //if (handledCommandsID.contains(bytesMsg.getCommandId())) return;

            //handledCommandsID.add(bytesMsg.getCommandId());

            SCMessage.Response response = (SCMessage.Response) MQMessageParser.parseMessageToProtobuf(bytesMsg, SCMessage.Response.parser());
            assert response != null;

            String clientId = response.getClientId();
            response.toBuilder().addPassedServersId(server.getServerID()).build();
            GateServerClient gateServerClient = GateServerClientPool.getSingleton().getClientViaID(clientId);
            if (gateServerClient != null){
                response = response.toBuilder().addPassedServersId(server.getServerID()).build();
                ResponseWriter.writeResponse(gateServerClient.getCtx(), response);

                if (response.toByteArray().length < 500){
                    server.logger.debug(String.format("服务器ID:%s %s 回复消息：%s", server.getServerID(), msgHandlerName, response.toString()));
                }
                    server.logger.debug(String.format("服务器ID:%s %s 回复消息长度：%d", server.getServerID(), msgHandlerName, response.toByteArray().length));

            }
        }
    }
}
