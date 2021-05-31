package connection.activemq;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

public class MQMessageParser {

    public static GeneratedMessageV3 parseMessageToProtobuf(BytesMessage bytesMessage, Parser parser){
        try {
            int len = (int) bytesMessage.getBodyLength();
            byte[] data = new byte[len];
            bytesMessage.readBytes(data);

            try {
                GeneratedMessageV3 proto = (GeneratedMessageV3) parser.parseFrom(data);
                return proto;

            } catch (InvalidProtocolBufferException var6) {
                var6.printStackTrace();
            }

        } catch (JMSException e){
            System.err.println(e);
        }
        return null;
    }

    public static void parseProtobufToMessage(GeneratedMessageV3 protobuf, BytesMessage bytesMessage){
        try {
            bytesMessage.writeBytes(protobuf.toByteArray());
        } catch (JMSException e){
            System.err.println(e);
        }
    }
}
