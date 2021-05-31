package client;

import client.pool.GateServerClientPool;
import client.pool.GateServerClientIDXPool;
import protobuf.SSMessageClientStateClose;
import server.GateServer;
import util.data.DateUtil;

import java.util.*;

public class GateServerClientHeartBeatManager {
    static final int MAX_TIMEOUT_SECONDS = 15;
    static final int MONITOR_SLEEP_TIME = 1000;

    private volatile static GateServerClientHeartBeatManager singleton;
    public static GateServerClientHeartBeatManager getSingleton() {
        if (singleton == null) {
            synchronized (GateServerClientHeartBeatManager.class) {
                if (singleton == null) {
                    singleton = new GateServerClientHeartBeatManager();
                }
            }
        }
        return singleton;
    }

    private static void clientHeartBeatTimeout(String id){
        GateServer.getSingleton().logger.debug(String.format("客户端实体id：%s 客户端实体心跳超时", id));
        heartBeats.remove(id);
        GateServerClientPool.getSingleton().removeClientViaID(id, SSMessageClientStateClose.Type.HEART_BEAT_TIMEOUT);
    }

    private static HashMap<String, Integer> heartBeats = new HashMap<>();

    public GateServerClient newClient(){
        return GateServerClientPool.getSingleton().addClient();
    }

    public GateServerClient clientHeartBeat(String id){
        int idx = ClientTool.parseClientIDtoIDX(id);

        GateServerClient n;
        if (idx < GateServerClientIDXPool.MIN){
            n = newClient();
            heartBeats.put(n.getId(), DateUtil.getSecondTimestamp(new Date()));
            return n;
        }
        n = GateServerClientPool.getSingleton().getClientViaID(id);
        if (n == null){
            n = GateServerClientPool.getSingleton().addClient();
        }
        heartBeats.put(n.getId(), DateUtil.getSecondTimestamp(new Date()));
        return n;
    }

    public void activeMonitor(){
        threadHeatBeatsMonitor.active = true;
    }

    public void inactiveMonitor(){
        threadHeatBeatsMonitor.active = false;
    }

    public void startMonitor(){
        activeMonitor();
        threadHeatBeatsMonitor.start();
    }

    public void stopMonitor(){
        inactiveMonitor();
        threadHeatBeatsMonitor.interrupt();
    }

    private ThreadHeatBeatsMonitor threadHeatBeatsMonitor = new ThreadHeatBeatsMonitor();



    static class ThreadHeatBeatsMonitor extends Thread {
        boolean active = false;
        boolean stopFlag = false;
        public void run() {
            while (!stopFlag){
                try {
                    sleep(MONITOR_SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!active) continue;
                int currentTimeStampSecond = DateUtil.getSecondTimestamp(new Date());
                //System.out.println("currentTimeStampSecond:" + currentTimeStampSecond);
                Iterator iter = heartBeats.entrySet().iterator();
                Vector<String> timeoutKeys = new Vector<>();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String key = (String) entry.getKey();
                    int lastTimeSecond = (int) entry.getValue();
                    int timeInterval = currentTimeStampSecond - lastTimeSecond;
                    //System.out.println("key:" + key);
                    //System.out.println("timeInterval:" + timeInterval);
                    //System.out.println();
                    if (timeInterval > MAX_TIMEOUT_SECONDS){
                        timeoutKeys.add(key);
                        //clientHeartBeatTimeout(key);
                    }
                }
                iter = timeoutKeys.iterator();
                while (iter.hasNext()) {
                    String id = (String) iter.next();
                    clientHeartBeatTimeout(id);
                }
            }
        }
        public void willStop(){
            stopFlag = true;
        }
    }
}
