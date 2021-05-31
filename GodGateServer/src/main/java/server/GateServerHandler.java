package server;

import client.ClientTool;
import client.GateServerClient;
import client.GateServerClientHeartBeatManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import protobuf.SCMessage;
import util.ResponseWriter;

public class GateServerHandler extends SimpleChannelInboundHandler<SCMessage.Request> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SCMessage.Request msg) throws Exception {
        GateServer.getSingleton().logger.debug(String.format("服务器ID:%s 通道id:%s 收到信息 msg:%s", GateServer.getSingleton().getServerID(), ctx.channel().id(), msg.toString()));

        if (msg.getType().equals(SCMessage.Type.CONNECT)){
            GateServerClient gateServerClient = GateServerClientHeartBeatManager.getSingleton().clientHeartBeat(ClientTool.parseIDXtoClientID(GateServer.getSingleton(), 0));


            GateServer.getSingleton().logger.debug(String.format("服务器ID:%s 通道id:%s 用户请求连接，分配的客户端id：%s", GateServer.getSingleton().getServerID(), ctx.channel().id(), gateServerClient.getId()));

            gateServerClient.setCtx(ctx);
            gateServerClient.setState(GateServerClient.STATE.CONNECTED);

            SCMessage.Response.Builder responseBuilder = SCMessage.Response.newBuilder();
            responseBuilder.setClientId(gateServerClient.getId());
            responseBuilder.setType(SCMessage.Type.CONNECT);
            responseBuilder.setGateServerId(GateServer.getSingleton().getServerID());
            responseBuilder.addPassedServersId(GateServer.getSingleton().getServerID());

            ////
            //responseBuilder.setTip("123456789009876543211234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM");
            //responseBuilder.setTip("xkcjocpn10-928234125124123312");
            ////

            SCMessage.Response response = responseBuilder.build();
            int temp = response.toByteString().size();

            ResponseWriter.writeResponse(gateServerClient.getCtx(), response);


            RedisGateClientsWorker.getSingleton().updateClient(gateServerClient);
            return;
        }
        if (msg.getType().equals(SCMessage.Type.HEART_BEAT)) {
            MQSCMsgReqHeartBeatHandler.getSingleton().response(msg);
            return;
        }
        MQSCMsgReqHandler.getSingleton().distribute(msg);
    }
}
