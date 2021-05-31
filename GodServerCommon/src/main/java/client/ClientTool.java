package client;

import server.Server;

public class ClientTool {
    public static int parseClientIDtoIDX(String id){
        String[] temp = id.split("-");
        assert temp.length == 3;
        return Integer.parseInt(temp[2]);
    }

    public static String parseIDXtoClientID(Server server, int idx){
        return server.getServerID() + "-" + idx;
    }
}
