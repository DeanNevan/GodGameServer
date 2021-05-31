package server.boot;

import org.kohsuke.args4j.*;
import org.kohsuke.args4j.CmdLineParser;
import server.boot.serverargs.GateServerArgs;

import java.util.ArrayList;
import java.util.List;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;



public class GodGameServerBoot {



    public static void main(String[] args) throws CmdLineException {
        String first = args[0];
        System.out.println(first);
        switch (first){
            case "GateServer":
//                GateServerArgs gateServerArgs = new GateServerArgs();
//                CmdLineParser parser = new CmdLineParser(gateServerArgs);
//
//                parser.printUsage(System.out);
//
//                parser.parseArgument(args);
//                gateServerArgs.output();
                break;
            default:
                break;
        }
    }



}

//java -jar GodGameServer.jar GateServer -port 8890 -mqURL failover:(tcp://59.110.53.159:61616) -mqUsername admin -mqPassword admin -sqlURL jdbc:mysql://59.110.53.159:3306?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC -sqlUsername GateServer -sqlPassword GateServer -redisIP 59.110.53.159 -redisPort 6379 -auth Deannevan233

//GateServer -port 8890 -mqURL failover:(tcp://59.110.53.159:61616) -mqUsername admin -mqPassword admin -sqlURL jdbc:mysql://59.110.53.159:3306?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC -sqlUsername GateServer -sqlPassword GateServer -redisIP 59.110.53.159 -redisPort 6379 -redisAuth Deannevan233

