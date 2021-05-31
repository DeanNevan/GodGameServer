package server;

import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.mysql.MysqlConnection;
import connection.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

public class DiyServer extends Server {
    public Logger logger = LoggerFactory.getLogger(DiyServer.class);

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
        Runtime.getRuntime().addShutdownHook(this);
        serverAgentType = ServerAgentType.DIY_SERVER;
        ip = "127.0.0.1";
        port = -1;
        RedisConnection.getSingleton().init(logger, "");
        ServerAgentTool.updateServerAgentIDXWithRedis(RedisConnection.getSingleton().getJedis(), this);
        ServerAgentTool.updateServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);

        //init handler
        MQSCMsgDiyHandler.getSingleton().init(this);

        // 添加程序关闭监听线程
        //MysqlConnection.getSingleton().setHeartBeatDB("user_login_db");
        MysqlConnection.getSingleton().init(logger, "");
        logger.debug(String.format("%s启动 服务器ID：%s", serverName, getServerID()));
        setServerState(SERVER_STATE.ACTIVE);
        ServerAgentTool.updateServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
    }

    public static void main(String[] args){
        DiyServer.getSingleton().startServer();
    }

    public void run(){
        setServerState(SERVER_STATE.STOPPING);
        ServerAgentTool.updateServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
        logger.debug(String.format("%s关闭 服务器ID：%s", serverName, getServerID()));
        ServerAgentTool.removeServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
        MysqlConnection.getSingleton().close();
        try {
            MQConsumerTool.getInstance().close();
            MQProducerTool.getInstance().close();
        } catch (JMSException e) {
            DiyServer.getSingleton().logger.error(e.toString());
        }
    }

}
