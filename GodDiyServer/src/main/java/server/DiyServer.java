package server;

import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.mysql.MysqlConnection;
import connection.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;

public class DiyServer extends Server {

    private volatile static DiyServer singleton;
    public static DiyServer getSingleton() {
        if (singleton == null) {
            synchronized (DiyServer.class) {
                if (singleton == null) {
                    singleton = new DiyServer();
                }
            }
        }
        return singleton;
    }

    private DiyServer(){
        serverName = "DIY服务器";
    }

    public void startServer() {
        serverAgentType = ServerAgentType.DIY_SERVER;
        ip = "127.0.0.1";
        port = -1;

        //init handler
        MQSCMsgDiyHandler.getSingleton().init(this);

        super.startServer();
    }

    public static void main(String[] args){
        DiyServer.getSingleton().startServer();
    }

}
