package server;

import client.GateServerClient;
import com.alibaba.fastjson.JSONObject;
import connection.redis.RedisConnection;
import redis.clients.jedis.Jedis;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class RedisGateClientsWorker {
    private volatile static RedisGateClientsWorker singleton;
    public static RedisGateClientsWorker getSingleton() {
        if (singleton == null) {
            synchronized (RedisGateClientsWorker.class) {
                if (singleton == null) {
                    singleton = new RedisGateClientsWorker();
                }
            }
        }
        return singleton;
    }

    public void init(){
    }

    public Vector<String> getActiveClientsIDVector(){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        Set list = jedis.hkeys("client:clients");
        Vector<String> intList = new Vector<>();
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            intList.add((String) iter.next());
        }
        return intList;
    }

    public boolean isActiveClientIDExists(String id){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        boolean result = jedis.hexists("client:clients", id);
        return result;
    }

    public GateServerClient getClientViaID(String id){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        String jsonString = jedis.hget("client:clients", id);
        if (jsonString == null) return null;
        GateServerClient gateServerClient = new GateServerClient("", "");
        gateServerClient.parseFromJSONString(jsonString);
        return gateServerClient;
    }

    public void updateAbandonedClient(GateServerClient gateServerClient){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        String jsonString = jedis.hget("client:abandoned_clients", gateServerClient.getId());
        JSONObject object = JSONObject.parseObject(jsonString);
        if (object == null) object = new JSONObject();
        gateServerClient.parseToJSONObject(object);
        String string = object.toJSONString();
        jedis.hset("client:abandoned_clients", gateServerClient.getId(), string);
    }

    public void updateClient(GateServerClient gateServerClient){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        String jsonString = jedis.hget("client:clients", gateServerClient.getId());
        JSONObject object = JSONObject.parseObject(jsonString);
        if (object == null) object = new JSONObject();
        gateServerClient.parseToJSONObject(object);
        String string = object.toJSONString();
        jedis.hset("client:clients", gateServerClient.getId(), string);
    }

    public void removeClient(GateServerClient gateServerClient){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        jedis.hdel("client:clients", String.valueOf(gateServerClient.getId()));
    }
}
