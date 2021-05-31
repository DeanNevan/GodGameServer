package connection.activemq;

import com.google.protobuf.AbstractMessage;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.jms.Queue;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class MQProducerTool {
    private static MQProducerTool instance;
    private final String URL = "url";
    private final String QUEUENAMES = "queues";
    private final String USERNAME = "username";
    private final String PASSWORD = "password";
    private final String MQ_PROPERTIES_FILE_PATH = "/activemq.properties";
    private ActiveMQConnectionFactory connectionFactory = null;
    private String user;
    private String password;
    private String url;
    private String topicNames;
    private Connection connection;
    private Session session;
    private static Map<String, Destination> destinationMap = new HashMap();
    private static Map<String, MessageProducer> producerMap = new HashMap();
    Properties properties;

    public static synchronized MQProducerTool getInstance() {
        if (instance == null) {
            instance = new MQProducerTool();
        }

        return instance;
    }

    private MQProducerTool() {
        this.user = ActiveMQConnection.DEFAULT_USER;
        this.password = ActiveMQConnection.DEFAULT_PASSWORD;
        this.url = "tcp://59.110.53.159:8161";
        this.connection = null;
        this.session = null;
        this.properties = null;
        this.init();
    }

    private void init() {
        InputStream is = null;

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
                    if (queueName != null) {
                        int queueType = this.getIntegerValue("queue_type_" + topics[i]);
                        int queueMode = this.getIntegerValue("queue_mode_" + topics[i]);
                        this.addDestination(queueName, queueType, queueMode);
                    } else {
                        System.out.println("topics[" + i + "]:" + topics[i]);
                    }
                }
            }
        } catch (JMSException var19) {
            var19.printStackTrace();
        } catch (IOException var20) {
            var20.printStackTrace();
        } catch (Exception var21) {
            var21.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException var18) {
                var18.printStackTrace();
            }

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

    protected void addDestination(MQDestinationItem item) throws JMSException {
        if (item.type == 1) {
            this.addQueue(item.name, item.mode);
        } else {
            this.addTopic(item.name, item.mode);
        }

    }

    public void addDestination(String destinationName, int queueOrTopic, int deliveryMode) throws JMSException {
        if (queueOrTopic == 1) {
            this.addQueue(destinationName, deliveryMode);
        } else {
            this.addTopic(destinationName, deliveryMode);
        }

    }

    private void addQueue(String queueName, int deliveryMode) throws JMSException {
        Queue queue = this.session.createQueue(queueName);
        this.addDestination(queue, queueName, deliveryMode);
    }

    private void addTopic(String topicName, int deliveryMode) throws JMSException {
        Topic topic = this.session.createTopic(topicName);
        this.addDestination(topic, topicName, deliveryMode);
    }

    private void addDestination(Destination destination, String destinationName, int deliveryMode) throws JMSException {
        MessageProducer producer = this.session.createProducer(destination);
        if (deliveryMode == 1) {
            producer.setDeliveryMode(1);
        } else {
            producer.setDeliveryMode(2);
        }

        destinationMap.put(destinationName, destination);
        producerMap.put(destinationName, producer);
    }

    public MessageProducer getProducer(String destinationName) {
        return (MessageProducer)producerMap.get(destinationName);
    }

    public void sendBuilder(String destinationName, AbstractMessage.Builder builder) throws JMSException, Exception {
        BytesMessage msg = this.session.createBytesMessage();
        msg.writeBytes(builder.build().toByteArray());
        MessageProducer producer = (MessageProducer)producerMap.get(destinationName);
        producer.send(msg);
    }

    public void sendBuilder(String destinationName, AbstractMessage.Builder builder, String selectorKey, String selectorValue) throws JMSException, Exception {
        BytesMessage msg = this.session.createBytesMessage();
        msg.writeBytes(builder.build().toByteArray());
        msg.setStringProperty(selectorKey, selectorValue);
        MessageProducer producer = (MessageProducer)producerMap.get(destinationName);
        producer.send(msg);
    }

    public void sendBytesMessage(String destinationName, Object message) throws JMSException, Exception {
        byte[] data = (byte[])message;
        BytesMessage msg = this.session.createBytesMessage();
        msg.writeBytes(data);
        MessageProducer producer = (MessageProducer)producerMap.get(destinationName);
        System.out.println("Producer:->Sending message: " + message);
        producer.send(msg);
        System.out.println("Producer:->Message sent complete!");
    }

    public void sendTextMessage(String destinationName, String message) throws JMSException, Exception {
        TextMessage msg = this.session.createTextMessage(message);
        MessageProducer producer = (MessageProducer)producerMap.get(destinationName);
        System.out.println("Producer:->Sending message: " + message);
        producer.send(msg);
        System.out.println("Producer:->Message sent complete!");
    }

    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        MessageProducer producer = this.session.createProducer(destination);
        producer.send(message, deliveryMode, priority, timeToLive);
    }

    public void send(String name, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        MessageProducer producer = (MessageProducer)producerMap.get(name);
        producer.send(message, deliveryMode, priority, timeToLive);
    }

    public void send(String name, String message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        TextMessage msg = this.session.createTextMessage(message);
        MessageProducer producer = (MessageProducer)producerMap.get(name);
        producer.send(msg, deliveryMode, priority, timeToLive);
    }

    public void send(String name, Object message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        byte[] data = (byte[])message;
        BytesMessage msg = this.session.createBytesMessage();
        msg.writeBytes(data);
        MessageProducer producer = (MessageProducer)producerMap.get(name);
        producer.send(msg, deliveryMode, priority, timeToLive);
    }

    public void sendMessage(Connection connection, List<String> key, List<String> message, String topicName) {
        try {
            MessageProducer producer = (MessageProducer)producerMap.get(topicName);
            MapMessage mapMessage = this.session.createMapMessage();
            Iterator<String> keyIt = key.iterator();
            Iterator messageIt = message.iterator();

            while(keyIt.hasNext() && messageIt.hasNext()) {
                mapMessage.setString((String)keyIt.next(), (String)messageIt.next());
            }

            producer.send(mapMessage);
        } catch (Exception var9) {
            var9.printStackTrace();
        }

    }

    public void sendMessage(Connection connection, Map<String, String> msgMap, String topicName) {
        try {
            MessageProducer producer = (MessageProducer)producerMap.get(topicName);
            MapMessage mapMessage = this.session.createMapMessage();
            Iterator var7 = msgMap.entrySet().iterator();

            while(var7.hasNext()) {
                Map.Entry<String, String> entity = (Map.Entry)var7.next();
                mapMessage.setString((String)entity.getKey(), (String)entity.getValue());
            }

            producer.send(mapMessage);
        } catch (Exception var8) {
        }

    }

    public void close() throws JMSException {
        System.out.println("Producer:->Closing connection");
        Iterator var2 = producerMap.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, MessageProducer> entry = (Map.Entry)var2.next();
            MessageProducer pb = (MessageProducer)entry.getValue();
            pb.close();
        }

        if (destinationMap != null && destinationMap.size() > 0) {
            destinationMap.clear();
            destinationMap = null;
        }

        if (producerMap != null && producerMap.size() > 0) {
            producerMap.clear();
            producerMap = null;
        }

        if (this.session != null) {
            this.session.close();
        }

        if (this.connection != null) {
            this.connection.close();
        }

    }
}
