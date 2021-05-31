package server;

import com.alibaba.fastjson.JSONObject;
import redis.clients.jedis.Jedis;

import java.util.*;

public class ServerAgentTool {

    private static final int MAX_SERVER_AGENT_IDX = 1000;

    public static void updateServerAgentIDXWithRedis(Jedis jedis, ServerAgent serverAgent){
        Set set = jedis.hkeys("server:running_servers");
        ServerAgentType targetAgentType = serverAgent.serverAgentType;
        ArrayList<Integer> usedID = new ArrayList<>();
        Iterator iter = set.iterator();
        ServerAgent newServerAgent = new ServerAgent();
        while (iter.hasNext()){
            String serverID = (String) iter.next();
            newServerAgent.parseID(serverID);
            if (newServerAgent.serverAgentType == targetAgentType){
                usedID.add(newServerAgent.serverIDX);
            }
        }
        int availableIDX = -1;
        for (int i = 0; i < MAX_SERVER_AGENT_IDX; i++){
            if (!usedID.contains(i)){
                availableIDX = i;
                break;
            }
        }
        serverAgent.serverIDX = availableIDX;
    }

    public static void updateServerAgentToRedis(Jedis jedis, ServerAgent serverAgent){
        JSONObject jsonObject = new JSONObject();
        serverAgent.parseToJSONObject(jsonObject);
        String jsonString = jsonObject.toJSONString();
        jedis.hset("server:running_servers", serverAgent.getServerID(), jsonString);
    }

    public static void removeServerAgentToRedis(Jedis jedis, ServerAgent serverAgent){
        jedis.hdel("server:running_servers", serverAgent.getServerID());
    }

    public static Vector<ServerAgent> getRunningServerAgents(Jedis jedis){
        Vector<ServerAgent> vector = new Vector<>();
        Map<String, String> map = new HashMap<>();
        map = jedis.hgetAll("server:running_servers");
        Iterator iter = map.entrySet().iterator();

        while (iter.hasNext()){
            ServerAgent serverAgent = new ServerAgent();
            Map.Entry entry = (Map.Entry) iter.next();
            String id = (String) entry.getKey();
            String json = (String) entry.getValue();
            serverAgent.parseFromJSONString(json);
            vector.add(serverAgent);
        }
        return vector;
    }

}
