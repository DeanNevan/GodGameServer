package server;


import client.GateServerClient;
import client.GateServerClientHeartBeatManager;
import client.pool.GateServerClientPool;
import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.redis.RedisConnection;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.SSMessageClientStateClose;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class GateServer extends Server{
    private volatile static GateServer singleton;

    public static GateServer getSingleton() {
        if (singleton == null) {
            synchronized (GateServer.class) {
                if (singleton == null) {
                    singleton = new GateServer();
                }
            }
        }
        return singleton;
    }

    public void startServer() {
        serverAgentType = ServerAgentType.GATE_SERVER;
        ip = "127.0.0.1";
        boolean result = getSingleton().selectAvailablePort();
        if (!result){
            logger.debug(String.format("%s启动失败 服务器ID：%s addr:%s", serverName, getServerID(), getAddr()));
            closeServer();
            return;
        }

        MQSCMsgReqHeartBeatHandler.getSingleton().init(this);
        MQSCMsgResHandler.getSingleton().init(this);
        MQSCMsgReqHandler.getSingleton().init(this);
        MQSSMsgClientStateHandler.getSingleton().init(this);
        GateServerClientPool.getSingleton().init(this);
        GateServerClientHeartBeatManager.getSingleton().startMonitor();

        super.startServer();


    }

    public static void main(String[] args) throws Exception{
        getSingleton().startServer();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup wokerGroup = new NioEventLoopGroup();

        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,wokerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new GateServerInitializer());

            ChannelFuture channelFuture = serverBootstrap.bind(getSingleton().port).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            wokerGroup.shutdownGracefully();
        }
    }

    public void run() {
        closeServer();
    }

    public void closeServer(){
        setServerState(SERVER_STATE.STOPPING);
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        ServerAgentTool.updateServerAgentToRedis(jedis, this);
        ServerAgentTool.removeServerAgentToRedis(jedis, this);
        RedisConnection.getSingleton().close(jedis);
        Iterator iter = GateServerClientPool.getSingleton().getClients().entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String clientID = (String) entry.getKey();
            GateServerClient gateServerClient = (GateServerClient) entry.getValue();
            GateServerClientPool.getSingleton().removeClientViaID(clientID, SSMessageClientStateClose.Type.FORCE);
            RedisGateClientsWorker.getSingleton().removeClient(gateServerClient);
            RedisGateClientsWorker.getSingleton().updateAbandonedClient(gateServerClient);
        }

        logger.debug(String.format("%s关闭 服务器ID：%s", serverName, getServerID()));
        try {
            MQConsumerTool.getInstance().close();
            MQProducerTool.getInstance().close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private GateServer(){
        serverName = "网关服务器";
    }

    private Properties properties;
    boolean selectAvailablePort(){
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream("/gate_server_config.properties");
            this.properties = new Properties();
            this.properties.load(is);
            //@Value("#{'${my.config.values}'.split(',')}");
            String[] ports = properties.getProperty("ports").split(",");
            int availablePort = -1;
            assert ports.length > 0;
            Vector<ServerAgent> vector = ServerAgentTool.getRunningServerAgents(RedisConnection.getSingleton().getJedis());
            Vector<Integer> vectorUsedPorts = new Vector<>();
            Iterator iter = vector.iterator();
            while (iter.hasNext()) {
                ServerAgent serverAgent = (ServerAgent) iter.next();
                if (serverAgent.ip.equals(getSingleton().ip)) vectorUsedPorts.add(serverAgent.port);
            }

            for (String scanningPort : ports){
                if (!vectorUsedPorts.contains(Integer.parseInt(scanningPort))){
                    availablePort = Integer.parseInt(scanningPort);
                    break;
                }
            }
            if (availablePort != -1) {
                port = availablePort;
                return true;
            }
            else return false;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


}
