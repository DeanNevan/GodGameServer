package server;

import com.alibaba.fastjson.JSONObject;
import connection.redis.RedisConnection;
import redis.clients.jedis.Jedis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class RedisMatchManagerWorker {
    private volatile static RedisMatchManagerWorker singleton;
    public static RedisMatchManagerWorker getSingleton() {
        if (singleton == null) {
            synchronized (RedisMatchManagerWorker.class) {
                if (singleton == null) {
                    singleton = new RedisMatchManagerWorker();
                }
            }
        }
        return singleton;
    }

    public void init(){
    }

    public List<String> getRunningServers(){
        Jedis jedis = RedisConnection.getSingleton().getJedis();
        List<String> list = jedis.lrange("server:running_servers", 0, -1);
        RedisConnection.getSingleton().close(jedis);
        return list;
    }

}
