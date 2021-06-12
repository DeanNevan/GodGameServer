package server;

public enum ServerAgentType {
    GATE_SERVER(0), LOGIN_SERVER(1), DIY_SERVER(2), MATCH_GATE_SERVER(3),  MATCH_MANAGER_SERVER(4), UNKNOWN_SERVER(-1);

    ServerAgentType(int i) {
    }
}
