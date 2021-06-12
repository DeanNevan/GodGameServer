package server;

import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.mysql.MysqlConnection;
import connection.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;

public class Server extends ServerAgent implements IServer {
    public Logger logger = LoggerFactory.getLogger(Server.class);
    @Override
    public void startServer() {
        Runtime.getRuntime().addShutdownHook(this);
        RedisConnection.getSingleton().init(logger, "");
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        ServerAgentTool.updateServerAgentIDXWithRedis(jedis, this);
        ServerAgentTool.updateServerAgentToRedis(jedis, this);
        RedisConnection.getSingleton().close(jedis);

        MysqlConnection.getSingleton().init(logger, "");
        logger.debug(String.format("%s启动 服务器ID：%s", serverName, getServerID()));
        setServerState(SERVER_STATE.ACTIVE);
        ServerAgentTool.updateServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
    }

    @Override
    public void closeServer(){
        setServerState(SERVER_STATE.STOPPING);
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        ServerAgentTool.updateServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
        ServerAgentTool.removeServerAgentToRedis(RedisConnection.getSingleton().getJedis(), this);
        RedisConnection.getSingleton().close(jedis);
        logger.debug(String.format("%s关闭 服务器ID：%s", serverName, getServerID()));
        MysqlConnection.getSingleton().close();
        try {
            MQConsumerTool.getInstance().close();
            MQProducerTool.getInstance().close();
        } catch (JMSException e) {
            logger.error(e.toString());
        }
    }

    public void run(){
        closeServer();
    }

}
