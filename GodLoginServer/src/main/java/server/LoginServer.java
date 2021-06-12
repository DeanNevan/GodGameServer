package server;

import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.mysql.MysqlConnection;
import connection.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;

public class LoginServer extends Server {
    private volatile static LoginServer singleton;
    public static LoginServer getSingleton() {
        if (singleton == null) {
            synchronized (LoginServer.class) {
                if (singleton == null) {
                    singleton = new LoginServer();
                }
            }
        }
        return singleton;
    }

    private LoginServer(){
        serverName = "登录服务器";
    }

    public void startServer() {
        serverAgentType = ServerAgentType.LOGIN_SERVER;
        ip = "127.0.0.1";
        port = -1;
        MQSCMsgReqLoginHandler.getSingleton().init(this);
        MQSSMsgClientStateHandler.getSingleton().init(this);

        super.startServer();
        // 添加程序关闭监听线程
        //MysqlConnection.getSingleton().setHeartBeatDB("user_login_db");
    }

    public static void main(String[] args){
        LoginServer.getSingleton().startServer();
    }

}
