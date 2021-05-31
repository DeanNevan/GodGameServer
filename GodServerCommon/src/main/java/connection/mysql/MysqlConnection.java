package connection.mysql;

import org.slf4j.Logger;

import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class MysqlConnection {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private String URL = "jdbc:mysql://59.110.53.159:3306?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private ThreadHeartBeat threadHeartBeat;
    private int HEART_BEAT_PERIOD = 30000;
    private String heartBeatSql = "SELECT heart_beat FROM heart_beat";
    private String heartBeatDB = "heart_beat";

    public String getHeartBeatDB() {
        return heartBeatDB;
    }

    public void setHeartBeatDB(String heartBeatDB) {
        this.heartBeatDB = heartBeatDB;
    }

    // 数据库的用户名与密码，需要根据自己的设置
    private String USER = "";
    private String PASS = "";

    private volatile static MysqlConnection singleton;
    public static MysqlConnection getSingleton() {
        if (singleton == null) {
            synchronized (MysqlConnection.class) {
                if (singleton == null) {
                    singleton = new MysqlConnection();
                }
            }
        }
        return singleton;
    }

    public Connection connection = null;
    Statement heartBeatStatement = null;
    Properties properties;

    private Logger logger;

    private String propertiesPath = "/mysql.properties";

    public void init(Logger logger, String targetPropertiesPath){
        getSingleton().logger = logger;
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream(targetPropertiesPath);
            if (is == null || targetPropertiesPath.equals("")) is = this.getClass().getResourceAsStream(propertiesPath);

            this.properties = new Properties();
            this.properties.load(is);
            URL = properties.getProperty("url") != null ? properties.getProperty("url") : URL;
            USER = properties.getProperty("user") != null ? properties.getProperty("user") : USER;
            PASS = properties.getProperty("pass") != null ? properties.getProperty("pass") : PASS;
            HEART_BEAT_PERIOD = properties.getProperty("heart_beat_period") != null ? Integer.parseInt(properties.getProperty("heart_beat_period")) : HEART_BEAT_PERIOD;

            _init();

        } catch(Exception se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }// 处理 Class.forName 错误
    }

    public void init(Logger logger){
        getSingleton().logger = logger;
        _init();
    }

    private void _init() {
        // 注册 JDBC 驱动
        try {
            Class.forName(JDBC_DRIVER);

            // 打开链接
            logger.debug(String.format("%s连接Mysql数据库...", USER));
            connection = DriverManager.getConnection(URL, USER, PASS);
//            logger.debug(String.format("%s实例化Statement对象....", USER));
//            statement = connection.createStatement();
//            logger.debug(String.format("%s实例化Statement对象....", USER));
//            logger.debug("登录服务器使用user_login_db数据库...");
//            statement.executeUpdate("Use user_login_db");
            if (!heartBeatDB.equals("")) {
                heartBeatStatement = connection.createStatement();
                threadHeartBeat = new ThreadHeartBeat();
                threadHeartBeat.HEART_BEAT_PERIOD = HEART_BEAT_PERIOD;
                threadHeartBeat.heartBeatDB = heartBeatDB;
                threadHeartBeat.start();
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    //让构造函数为 private，这样该类就不会被实例化
    private MysqlConnection() {
    }

    public void close() {
        try {
            if (connection !=null) {
                connection.close();
                logger.debug(String.format("%s与Mysql数据库连接关闭...", USER));
            }
            if (threadHeartBeat != null){
                threadHeartBeat.willStop();
                threadHeartBeat.interrupt();
            }
        } catch (SQLException e){
            System.err.println(e);
        }
    }

    private void sendMeaninglessSelect(){

    }

    class ThreadHeartBeat extends Thread{
        private int HEART_BEAT_PERIOD = 30000;
        private boolean stopFlag = false;
        String heartBeatDB = "";
        public void run(){
            try {
                heartBeatStatement.executeUpdate(String.format("USE %s", heartBeatDB));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            while (!stopFlag){
                try {
                    if (connection != null && !connection.isClosed()){
                        ResultSet rs = heartBeatStatement.executeQuery(heartBeatSql);
                        logger.debug("Mysql心跳");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                try {
                    sleep(HEART_BEAT_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void willStop(){
            stopFlag = true;
        }

    }

}
