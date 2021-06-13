package server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import protobuf.SCMessage;


public class MatchGateServerInitializer extends ChannelInitializer<SocketChannel>{

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        MatchGateServer.getSingleton().logger.debug(String.format("网关服务器通道id:%s 网关服务器Channel初始化", ch.id()));
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MatchGateServerHandler());
    }
}