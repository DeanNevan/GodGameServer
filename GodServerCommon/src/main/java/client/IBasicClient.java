package client;

import com.alibaba.fastjson.JSONObject;
import server.Server;

public interface IBasicClient {
    public void parseToJSONObject(JSONObject jsonObject);
    public void parseFromJSONString(String JSONString);
}
