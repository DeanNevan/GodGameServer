package server;

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import protobuf.SCMessage;

public class CustomProtobufEncoder extends MessageToByteEncoder<SCMessage.Response> {

    @Override
    protected void encode(ChannelHandlerContext ctx, SCMessage.Response msg, ByteBuf out)
            throws Exception {
        byte[] bytes = msg.toByteArray();// 将对象转换为byte
        int length = bytes.length;// 读取消息的长度

        ByteBuf buf = Unpooled.buffer(4 + length);
        buf.writeInt(length);// 先将消息长度写入，也就是消息头
        buf.writeBytes(bytes);// 消息体中包含我们要发送的数据
        out.writeBytes(buf);
    }

}
