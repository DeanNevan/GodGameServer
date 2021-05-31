package server;

import com.alibaba.fastjson.JSONObject;
import connection.redis.RedisConnection;
import redis.clients.jedis.Jedis;
import client.LoginClient;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class RedisLoginClientsWorker {
    private volatile static RedisLoginClientsWorker singleton;
    public static RedisLoginClientsWorker getSingleton() {
        if (singleton == null) {
            synchronized (RedisLoginClientsWorker.class) {
                if (singleton == null) {
                    singleton = new RedisLoginClientsWorker();
                }
            }
        }
        return singleton;
    }

    public void init(){
    }

    public Vector<Integer> getLoginClientsIDVector(){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        Set list = jedis.hkeys("client:login_clients");
        Vector<Integer> intList = new Vector<>();
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            intList.add((int) iter.next());
        }
        return intList;
    }

    public boolean isLoginClientIDExists(String id){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        boolean result = jedis.hexists("client:login_clients", id);
        return result;
    }

    public LoginClient getLoginClientViaID(String id){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        String jsonString = jedis.hget("client:login_clients", id);
        if (jsonString == null) return null;
        LoginClient loginClient = new LoginClient("", "");
       loginClient.parseFromJSONString(jsonString);
        assert id.equals(loginClient.getId());
        return loginClient;
    }

    public void updateLoginClient(LoginClient loginClient){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        String jsonString = jedis.hget("client:login_clients", loginClient.getId());
        JSONObject object = JSONObject.parseObject(jsonString);
        if (object == null) object = new JSONObject();
        loginClient.parseToJSONObject(object);
        String string = object.toJSONString();
        jedis.hset("client:login_clients", loginClient.getId(), string);
    }

    public void removeLoginClient(LoginClient loginClient){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        jedis.hdel("client:login_clients", String.valueOf(loginClient.getId()));
    }

    public void removeLoginClient(String id){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        LoginClient loginClient = getLoginClientViaID(id);
        if (loginClient == null) return;
        jedis.hdel("client:login_clients", String.valueOf(loginClient.getId()));
    }

}
