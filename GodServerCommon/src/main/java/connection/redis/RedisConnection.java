package connection.redis;

import org.apache.activemq.ActiveMQConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;

public class RedisConnection {
    private volatile static RedisConnection singleton;
    public static synchronized RedisConnection getSingleton() {
        if (singleton == null) {
            synchronized (RedisConnection.class) {
                if (singleton == null) {
                    singleton = new RedisConnection();
                }
            }
        }
        return singleton;
    }

    private String propertiesPath = "/redis.properties";
    private Logger logger;

    Properties properties;

    //服务器IP地址
    private String IP = "127.0.0.1";
    //端口
    private int PORT = 6379;
    //密码
    private String AUTH = "password";
    //连接实例的最大连接数
    private int MAX_ACTIVE = 1024;
    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private int MAX_IDLE = 200;
    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException
    private int MAX_WAIT = 10000;
    //连接超时的时间　　
    private int TIMEOUT = 10000;
    // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private boolean TEST_ON_BORROW = true;

    private JedisPool jedisPool = null;
    //数据库模式是16个数据库 0~15
    public int DEFAULT_DATABASE = 0;

    public void init(Logger logger, String targetPropertiesPath){
        getSingleton().logger = logger;
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream(targetPropertiesPath);
            if (is == null || targetPropertiesPath.equals("")) is = this.getClass().getResourceAsStream(propertiesPath);
            this.properties = new Properties();
            this.properties.load(is);
            IP = properties.getProperty("ip") != null ? properties.getProperty("ip") : IP;
            PORT = properties.getProperty("port") != null ? Integer.parseInt(properties.getProperty("port")) : PORT;
            AUTH = properties.getProperty("auth") != null ? properties.getProperty("auth") : AUTH;
            MAX_ACTIVE = properties.getProperty("max_active") != null ? Integer.parseInt(properties.getProperty("max_active")) : MAX_ACTIVE;
            MAX_IDLE = properties.getProperty("max_idle") != null ? Integer.parseInt(properties.getProperty("max_idle")) : MAX_IDLE;
            MAX_WAIT = properties.getProperty("max_wait") != null ? Integer.parseInt(properties.getProperty("max_wait")) : MAX_WAIT;
            TIMEOUT = properties.getProperty("timeout") != null ? Integer.parseInt(properties.getProperty("timeout")) : TIMEOUT;
            TEST_ON_BORROW = properties.getProperty("test_on_borrow") != null ? Boolean.parseBoolean(properties.getProperty("test_on_borrow")) : TEST_ON_BORROW;
            DEFAULT_DATABASE = properties.getProperty("default_database") != null ? Integer.parseInt(properties.getProperty("default_database")) : DEFAULT_DATABASE;
            _init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(Logger logger){
        getSingleton().logger = logger;
        _init();
    }

    private void _init(){
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, IP, PORT, TIMEOUT, AUTH, DEFAULT_DATABASE);

            logger.debug("===Redis成功连接===");
            logger.debug(String.format("Redis IP:%s PORT:%d", IP, PORT));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RedisConnection(){}

    /**
     * 获取Jedis实例
     */
    public synchronized Jedis getJedis() {
        try {
            if (getSingleton().jedisPool != null) {
                Jedis resource = getSingleton().jedisPool.getResource();
                //System.out.println("redis--服务正在运行: "+resource.ping());
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 释放jedis资源
     * @param jedis
     */
    public void close(final Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
}