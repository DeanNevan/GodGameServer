package server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import protobuf.SCMessage;


public class GateServerInitializer extends ChannelInitializer<SocketChannel>{

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        GateServer.getSingleton().logger.debug(String.format("网关服务器通道id:%s 网关服务器Channel初始化", ch.id()));
        ChannelPipeline pipeline = ch.pipeline();

        ByteBuf delimiter = Unpooled.copiedBuffer("$_$".getBytes());
        pipeline.addLast(new DelimiterBasedFrameDecoder(4096, delimiter));
//        //解码器，通过Google Protocol Buffers序列化框架动态的切割接收到的ByteBuf
        //pipeline.addLast(new ProtobufVarint32FrameDecoder());
        //服务器端接收的是客户端RequestUser对象，所以这边将接收对象进行解码生产实列
        pipeline.addLast(new ProtobufDecoder(SCMessage.Request.getDefaultInstance()));

        pipeline.addLast(new CustomProtobufEncoder());
        //Google Protocol Buffers编码器
        //pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
        //Google Protocol Buffers编码器
        //pipeline.addLast(new ProtobufEncoder());
//
//        pipeline.addLast(new LengthFieldPrepender(1));

        pipeline.addLast(new GateServerHandler());
    }
}