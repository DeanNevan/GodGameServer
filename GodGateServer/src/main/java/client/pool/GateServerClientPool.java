package client.pool;


import client.ClientTool;
import client.GateServerClient;
import protobuf.SSMessageClientStateClose;
import server.MQSSMsgClientStateHandler;
import server.RedisGateClientsWorker;
import server.Server;

import java.util.HashMap;

public class GateServerClientPool {
    private volatile static GateServerClientPool singleton;
    public static GateServerClientPool getSingleton() {
        if (singleton == null) {
            synchronized (GateServerClientPool.class) {
                if (singleton == null) {
                    singleton = new GateServerClientPool();
                }
            }
        }
        return singleton;
    }

    public Server server;

    public void init(Server server){
        this.server = server;
        server.logger.debug(String.format("服务器ID:%s 客户端实体ID池初始化", server.getServerID()));
    }

    private static HashMap<String, GateServerClient> clients = new HashMap<>();

    public GateServerClient getClientViaID(String id){
        return clients.get(id);
    }

    public GateServerClient addClient(){
        int idx = GateServerClientIDXPool.getSingleton().getId();
        GateServerClient gateServerClient = new GateServerClient(ClientTool.parseIDXtoClientID(server, idx), server.getServerID());
        clients.put(gateServerClient.getId(), gateServerClient);

        RedisGateClientsWorker.getSingleton().updateClient(gateServerClient);

        return gateServerClient;
    }

    public HashMap<String, GateServerClient> getClients() {
        return clients;
    }

    public void removeClientViaID(String id, SSMessageClientStateClose.Type closeType){
        GateServerClient n = getClientViaID(id);
        if (n == null){
            return;
        }
        clients.remove(id);
        int idx = ClientTool.parseClientIDtoIDX(id);
        GateServerClientIDXPool.getSingleton().freeId(idx);
        RedisGateClientsWorker.getSingleton().removeClient(n);

        MQSSMsgClientStateHandler.getSingleton().sendSSMessageClientClose(n, closeType);

    }

    public int getClientsSize(){
        return clients.size();
    }
}
