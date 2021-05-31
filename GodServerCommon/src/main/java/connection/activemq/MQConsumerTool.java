package connection.activemq;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class MQConsumerTool implements MessageListener, ExceptionListener {
    private static MQConsumerTool instance;
    private String user;
    private String password;
    private String url;
    private String topicNames;
    private final String URL;
    private final String QUEUENAMES;
    private final String USERNAME;
    private final String PASSWORD;
    private final String MQ_PROPERTIES_FILE_PATH;
    private ActiveMQConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    public static Boolean isconnectioned = false;
    private static Map<String, Destination> destinationMap = new HashMap();
    private static Map<String, MessageConsumer> consumerMap = new HashMap();
    Properties properties;

    public static synchronized MQConsumerTool getInstance() {
        if (instance == null) {
            instance = new MQConsumerTool();
        }

        return instance;
    }

    private MQConsumerTool() {
        this.user = ActiveMQConnection.DEFAULT_USER;
        this.password = ActiveMQConnection.DEFAULT_PASSWORD;
        this.url = "tcp://59.110.53.159:8161";
        this.URL = "url";
        this.QUEUENAMES = "queues";
        this.USERNAME = "admin";
        this.PASSWORD = "admin";
        this.MQ_PROPERTIES_FILE_PATH = "/activemq.properties";
        this.connectionFactory = null;
        this.connection = null;
        this.session = null;
        this.properties = null;
        this.init();
    }

    private void init() {
        InputStream is;

        try {
            is = this.getClass().getResourceAsStream("/activemq.properties");
            this.properties = new Properties();
            this.properties.load(is);
            this.url = this.properties.getProperty("url");
            this.topicNames = this.properties.getProperty("queues");
            this.user = this.properties.getProperty("username");
            this.password = this.properties.getProperty("password");
            this.connectionFactory = new ActiveMQConnectionFactory(this.user, this.password, this.url);
            this.connection = this.connectionFactory.createConnection();
            this.session = this.connection.createSession(false, 1);
            this.connection.start();
            if (this.topicNames != null && this.topicNames.length() > 0) {
                String[] topics = this.topicNames.split(";");

                for(int i = 0; i < topics.length; ++i) {
                    String queueName = this.properties.getProperty("queue_name_" + topics[i]);
                    int queueType = this.getIntegerValue("queue_type_" + topics[i]);
                    this.getIntegerValue("queue_mode_" + topics[i]);
                    this.addQueueOrTopic(queueName, queueType, new MQListener(queueName));
                }
            }
        } catch (JMSException var7) {
            var7.printStackTrace();
        } catch (Exception var8) {
            var8.printStackTrace();
        }

    }

    public String getValue(String key) {
        String value = "";
        if (this.properties.containsKey(key)) {
            value = this.properties.getProperty(key);
        }

        return value;
    }

    public int getIntegerValue(String key) {
        int value = 0;
        if (this.properties.containsKey(key)) {
            value = Integer.parseInt(this.properties.getProperty(key));
        }

        return value;
    }

    public void addQueueOrTopic(String destinationName, int queueOrTopic, MessageListener listener, String selector) throws JMSException {
        if (queueOrTopic == 1) {
            this.addQueue(destinationName, listener, selector);
        } else {
            this.addTopic(destinationName, listener, selector);
        }

    }

    public void addQueueOrTopic(String destinationName, int queueOrTopic, MessageListener listener) throws JMSException {
        if (queueOrTopic == 1) {
            this.addQueue(destinationName, listener);
        } else {
            this.addTopic(destinationName, listener);
        }

    }

    private void addQueue(String queueName, MessageListener listener, String selector) throws JMSException {
        Queue queue = this.session.createQueue(queueName);
        this.addDestination(queue, queueName, listener, selector);
    }

    private void addTopic(String topicName, MessageListener listener, String selector) throws JMSException {
        Topic topic = this.session.createTopic(topicName);
        this.addDestination(topic, topicName, listener, selector);
    }

    private void addQueue(String queueName, MessageListener listener) throws JMSException {
        Queue queue = this.session.createQueue(queueName);
        this.addDestination(queue, queueName, listener);
    }

    private void addTopic(String topicName, MessageListener listener) throws JMSException {
        Topic topic = this.session.createTopic(topicName);
        this.addDestination(topic, topicName, listener);
    }

    private void addDestination(Destination destination, String destinationName, MessageListener listener) throws JMSException {
        MessageConsumer consumer = this.session.createConsumer(destination);
        if (listener == null) {
            consumer.setMessageListener(this);
        } else {
            consumer.setMessageListener(listener);
        }

        destinationMap.put(destinationName, destination);
        consumerMap.put(destinationName, consumer);
    }

    private void addDestination(Destination destination, String destinationName, MessageListener listener, String selector) throws JMSException {
        MessageConsumer consumer = this.session.createConsumer(destination, selector);
        if (listener == null) {
            consumer.setMessageListener(this);
        } else {
            consumer.setMessageListener(listener);
        }

        destinationMap.put(destinationName, destination);
        consumerMap.put(destinationName, consumer);
    }

    public MessageConsumer getConsumer(String destinationName) {
        return (MessageConsumer)consumerMap.get(destinationName);
    }

    public void consumeMessage(String destinationName) throws JMSException, Exception {
        MessageConsumer consumer = (MessageConsumer)consumerMap.get(destinationName);
        this.connection.setExceptionListener(this);
        isconnectioned = true;
        System.out.println("Consumer:->Begin listening...");
    }

    public void close() throws JMSException {
        System.out.println("Consumer:->Closing connection");
        Iterator var2 = consumerMap.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, MessageConsumer> entry = (Map.Entry)var2.next();
            MessageConsumer pb = (MessageConsumer)entry.getValue();
            pb.close();
        }

        if (destinationMap != null && destinationMap.size() > 0) {
            destinationMap.clear();
            destinationMap = null;
        }

        if (consumerMap != null && consumerMap.size() > 0) {
            consumerMap.clear();
            consumerMap = null;
        }

        if (this.session != null) {
            this.session.close();
        }

        if (this.connection != null) {
            this.connection.close();
        }

    }

    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage)message;
                String msg = txtMsg.getText();
                System.out.println("Consumer:->Received: " + msg);
            } else {
                System.out.println("Consumer:->Received: " + message);
            }
        } catch (JMSException var4) {
            var4.printStackTrace();
        }

    }

    public void onException(JMSException arg0) {
        isconnectioned = false;
    }
}
