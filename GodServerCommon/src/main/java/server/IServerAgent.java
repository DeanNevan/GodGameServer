package server;

public interface IServerAgent {
    public void parseIDAddr(String ServerID, String addr);

    public void parseID(String ServerID);

    public void parseAddr(String addr);
}
