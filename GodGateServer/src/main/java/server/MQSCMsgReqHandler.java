package server;

import connection.activemq.MQProducerTool;
import protobuf.SCMessage;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

public class MQSCMsgReqHandler extends MsgHandler{

    static final String[] topics = {
            "SCMessageRequest_HeartBeat",
            "SCMessageRequest_Login",
            "SCMessageRequest_Diy"
    };

    Map<SCMessage.Type, String> requestType2topic = createMap();
    private static Map<SCMessage.Type, String> createMap() {
        Map<SCMessage.Type, String> myMap = new HashMap<SCMessage.Type, String>();
        myMap.put(SCMessage.Type.HEART_BEAT, "SCMessageRequest_HeartBeat");
        myMap.put(SCMessage.Type.LOGIN, "SCMessageRequest_Login");
        myMap.put(SCMessage.Type.DIY, "SCMessageRequest_Diy");
        return myMap;
    }


    private volatile static MQSCMsgReqHandler singleton;

    public static MQSCMsgReqHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSCMsgReqHandler.class) {
                if (singleton == null) {
                    singleton = new MQSCMsgReqHandler();
                }
            }
        }
        return singleton;
    }

    Server server;
    public void init(Server server) {
        this.server = server;
        server.logger.debug(String.format("服务器ID:%s %s 初始化",server.getServerID(), msgHandlerName));
        try {
            for (String topic : topics) {
                MQProducerTool.getInstance().addDestination(topic, 1, 1);
            }
        } catch (JMSException e) {
            GateServer.getSingleton().logger.error(e.toString());
        }
    }

    private MQSCMsgReqHandler() {
        msgHandlerName = "消息分发器";
    }

    public void distribute(SCMessage.Request msg) {
        server.logger.debug(String.format("服务器ID:%s %s 处理消息：%s",server.getServerID(), msgHandlerName, msg.toString()));
        try {
            //发送消息
            // 创建队列目标,并标识队列名称，消费者根据队列名称接收数据
            String topicString = requestType2topic.get(msg.getType());
            if (topicString == null) return;

            SCMessage.Request.Builder builder = msg.toBuilder();
            builder.setGateServerId(server.getServerID());
            builder.addPassedServersId(server.getServerID());
            MQProducerTool.getInstance().sendBuilder(topicString, builder, "target_server_id", "anyone");
        } catch (Exception e) {
            GateServer.getSingleton().logger.error(e.toString());
        }
    }

}
