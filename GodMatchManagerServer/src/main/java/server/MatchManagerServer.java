package server;

import connection.activemq.MQConsumerTool;
import connection.activemq.MQProducerTool;
import connection.mysql.MysqlConnection;
import connection.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MatchManagerServer extends Server {

    HashMap<String, ServerAgent> matchGateServerAgents = new HashMap<>();

    private volatile static MatchManagerServer singleton;
    public static MatchManagerServer getSingleton() {
        if (singleton == null) {
            synchronized (MatchManagerServer.class) {
                if (singleton == null) {
                    singleton = new MatchManagerServer();
                }
            }
        }
        return singleton;
    }

    private MatchManagerServer(){
        serverName = "比赛管理服务器";
    }

    public void startServer() {
        serverAgentType = ServerAgentType.MATCH_MANAGER_SERVER;
        ip = "127.0.0.1";
        port = -1;
        super.startServer();
        updateMatchGateServers();
    }

    public static void main(String[] args){
        MatchManagerServer.getSingleton().startServer();
    }

    public void updateMatchGateServers(){
        matchGateServerAgents.clear();
        List<String> list = RedisMatchManagerWorker.getSingleton().getRunningServers();
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            String string = (String) iter.next();
            ServerAgent serverAgent = new ServerAgent();
            serverAgent.parseFromJSONString(string);
            if (serverAgent.getServerAgentType() == ServerAgentType.MATCH_GATE_SERVER){
                matchGateServerAgents.put(serverAgent.getServerID(), serverAgent);
            }
        }
    }
}
