package server;


import com.alibaba.fastjson.JSONObject;

public class ServerAgent extends Thread implements IServerAgent {

    public enum SERVER_STATE{
        UNKNOWN, ACTIVE, INACTIVE, STOPPING
    }
    public SERVER_STATE serverState = SERVER_STATE.UNKNOWN;

    public String ip = "127.0.0.1";
    public int port = 8080;
    public ServerAgentType serverAgentType = ServerAgentType.UNKNOWN_SERVER;
    public int serverIDX = -1;
    public String serverName = "服务器名称";

    public String getServerID(){
        return serverAgentType + "-" + serverIDX;
    }

    public String getAddr(){
        return ip + ":" + port;
    }

    public void parseIDAddr(String ServerID, String addr){
        String[] strings = ServerID.split("-");
        assert strings.length == 2;
        serverIDX = Integer.parseInt(strings[0]);
        serverAgentType = ServerAgentType.valueOf(strings[1]);
        strings = addr.split(":");
        assert strings.length == 2;
        ip = strings[0];
        port = Integer.parseInt(strings[1]);
    }

    public void parseID(String ServerID){
        String[] strings = ServerID.split("-");
        assert strings.length == 2;
        serverAgentType = ServerAgentType.valueOf(strings[0]);
        serverIDX = Integer.parseInt(strings[1]);
    }

    public void parseAddr(String addr){
        String[] strings = addr.split(":");
        assert strings.length == 2;
        ip = strings[0];
        port = Integer.parseInt(strings[1]);
    }

    public void setServerState(SERVER_STATE state) {
        this.serverState = state;
    }

    public SERVER_STATE getServerState() {
        return serverState;
    }

    public void setServerAgentType(ServerAgentType serverAgentType) {
        this.serverAgentType = serverAgentType;
    }

    public ServerAgentType getServerAgentType() {
        return serverAgentType;
    }

    public void parseToJSONObject(JSONObject jsonObject){
        if (jsonObject.getString("id") == null) {
            jsonObject.put("id", getServerID());
        } else {
            jsonObject.replace("id", getServerID());
        }
        if (jsonObject.getString("addr") == null) {
            jsonObject.put("addr", getAddr());
        } else {
            jsonObject.replace("addr", getAddr());
        }
        if (jsonObject.getString("type") == null) {
            jsonObject.put("type", getServerAgentType());
        } else {
            jsonObject.replace("type", getServerAgentType());
        }
        if (jsonObject.getString("state") == null) {
            jsonObject.put("state", getServerState());
        } else {
            jsonObject.replace("state", getServerState());
        }
    }

    public void parseFromJSONString(String JSONString){
        JSONObject jsonObject = JSONObject.parseObject(JSONString);
        if (jsonObject.getString("id") != null) {
            parseID(jsonObject.getString("id"));
        }
        if (jsonObject.getString("addr") != null) {
            parseAddr(jsonObject.getString("addr"));
        }
        if (jsonObject.getString("type") != null) {
            setServerAgentType(ServerAgentType.valueOf(jsonObject.getString("type")));
        }
        if (jsonObject.getString("state") != null) {
            setServerState(SERVER_STATE.valueOf(jsonObject.getString("state")));
        }
    }

}
