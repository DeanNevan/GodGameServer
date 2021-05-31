package connection.activemq;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class MQListener implements MessageListener {
    private String job;

    public MQListener(String job) {
        this.job = job;
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
}
