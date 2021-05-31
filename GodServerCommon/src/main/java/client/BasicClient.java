package client;

import com.alibaba.fastjson.JSONObject;
import server.Server;

import java.time.LocalTime;

public class BasicClient implements IBasicClient{
    public enum STATE {
        UNKNOWN, CONNECTED, ACTIVE, INACTIVE, CLOSE
    }

    private boolean authenticated = false;
    private String userName;
    private String deviceID;
    private String gateServerID;
    private String id;
    private STATE state = STATE.UNKNOWN;
    private LocalTime time = LocalTime.now();

    public BasicClient(String id, String gateServerID){
        setId(id);
        setGateServerID(gateServerID);
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        setState(STATE.ACTIVE);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String getGateServerID() {
        return gateServerID;
    }

    public void setGateServerID(String gateServerID) {
        this.gateServerID = gateServerID;
    }

    public String getId() {
        return id;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public String getUserName() {
        return userName;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public STATE getState() {
        return state;
    }

    public void setState(STATE state) {
        this.state = state;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void parseToJSONObject(JSONObject jsonObject){
        if (jsonObject.getString("id") == null) {
            jsonObject.put("id", getId());
        } else {
            jsonObject.replace("id", getId());
        }
        if (jsonObject.getString("user_name") == null) {
            jsonObject.put("user_name", getUserName());
        } else {
            jsonObject.replace("user_name", getUserName());
        }
        if (jsonObject.getString("device_id") == null) {
            jsonObject.put("device_id", getDeviceID());
        } else {
            jsonObject.replace("device_id", getDeviceID());
        }
        if (jsonObject.getString("state") == null) {
            jsonObject.put("state", getState());
        } else {
            jsonObject.replace("state", getState());
        }
        if (jsonObject.getString("gate_server_id") == null) {
            jsonObject.put("gate_server_id", getGateServerID());
        } else {
            jsonObject.replace("gate_server_id", getGateServerID());
        }
        if (jsonObject.getBoolean("authenticated") == null) {
            jsonObject.put("authenticated", isAuthenticated());
        } else {
            jsonObject.replace("authenticated", isAuthenticated());
        }
    }

    public void parseFromJSONString(String JSONString){
        JSONObject jsonObject = JSONObject.parseObject(JSONString);
        if (jsonObject.getString("id") != null) {
            setId(jsonObject.getString("id"));
        }
        if (jsonObject.getString("user_name") != null) {
            setUserName(jsonObject.getString("user_name"));
        }
        if (jsonObject.getString("device_id") != null) {
            setDeviceID(jsonObject.getString("device_id"));
        }
        if (jsonObject.getString("state") != null) {
            setState(STATE.valueOf(jsonObject.getString("state")));
        }
        if (jsonObject.getString("gate_server_id") != null) {
            setGateServerID((jsonObject.getString("gate_server_id")));
        }
        if (jsonObject.getBoolean("authenticated") != null) {
            setAuthenticated((jsonObject.getBoolean("authenticated")));
        }
    }
}
