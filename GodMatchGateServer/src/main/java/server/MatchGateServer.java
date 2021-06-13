package server;

import connection.redis.RedisConnection;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

public class MatchGateServer extends Server {
    // UDP服务监听的数据通道
    public static Channel channel;
    public static ChannelHandlerContext ctx;

    private volatile static MatchGateServer singleton;
    public static MatchGateServer getSingleton() {
        if (singleton == null) {
            synchronized (MatchGateServer.class) {
                if (singleton == null) {
                    singleton = new MatchGateServer();
                }
            }
        }
        return singleton;
    }

    private MatchGateServer(){
        serverName = "比赛网关服务器";
    }

    public void startServer() {
        serverAgentType = ServerAgentType.MATCH_GATE_SERVER;
        ip = "127.0.0.1";
        boolean result = getSingleton().selectAvailablePort();
        if (!result){
            logger.debug(String.format("%s启动失败 服务器ID：%s addr:%s", serverName, getServerID(), getAddr()));
            closeServer();
            return;
        }
        super.startServer();
    }

    public static void main(String[] args){

        MatchGateServer.getSingleton().startServer();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        try {
            //通过NioDatagramChannel创建Channel，并设置Socket参数支持广播
            //UDP相对于TCP不需要在客户端和服务端建立实际的连接，因此不需要为连接（ChannelPipeline）设置handler
            Bootstrap b = new Bootstrap();
            b.group(bossGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new MatchGateServerInitializer());
            b.bind(getSingleton().port).sync().channel().closeFuture().await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
        }


    }

    private Properties properties;
    boolean selectAvailablePort(){
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream("/match_gate_server_config.properties");
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
