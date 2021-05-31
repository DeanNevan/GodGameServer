package client;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import server.GateServer;
import server.Server;

import java.time.LocalTime;

public class GateServerClient extends BasicClient{

    private ChannelHandlerContext ctx;

    public GateServerClient(String id, String gateServerID){
        super(id, gateServerID);
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
}
