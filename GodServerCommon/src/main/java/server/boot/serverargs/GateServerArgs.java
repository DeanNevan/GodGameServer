package server.boot.serverargs;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;

public class GateServerArgs {
    @Option(name = "-port", required = true, usage = "设置网关服务器ip")
    private int port;
    @Option(name = "-mqURL", required = false, usage = "消息中间件url")
    private String mqURL;
    @Option(name = "-mqUsername", required = false, usage = "消息中间件username")
    private String mqUsername;
    @Option(name = "-mqPassword", required = false, usage = "消息中间件password")
    private String mqPassword;
    @Option(name = "-sqlURL", required = false, usage = "MySQL数据库password")
    private String sqlURL;
    @Option(name = "-sqlUsername", required = false, usage = "MySQL数据库password")
    private String sqlUsername;
    @Option(name = "-sqlPassword", required = false, usage = "MySQL数据库password")
    private String sqlPassword;
    @Option(name = "-redisIP", required = false, usage = "Redis数据库ip")
    private String redisIP;
    @Option(name = "-redisPort", required = false, usage = "Redis数据库port")
    private String redisPort;
    @Option(name = "-redisAuth", required = false, usage = "Redis数据库auth")
    private String redisAuth;

    @Argument
    private List<String> arguments = new ArrayList<String>();

    public List<String> getArguments() {
        return arguments;
    }

    public void output() {
        System.out.println("port=" + port);
        System.out.println("mqURL=" + mqURL);
        System.out.println("mqUsername=" + mqUsername);
        System.out.println("mqPassword=" + mqPassword);
        System.out.println("sqlURL=" + sqlURL);
        System.out.println("sqlUsername=" + sqlUsername);
        System.out.println("sqlPassword=" + sqlPassword);
        System.out.println("redisIP=" + redisIP);
        System.out.println("redisPort=" + redisPort);
        System.out.println("redisAuth=" + redisAuth);
    }
}
