package cs451.links;

import cs451.Host;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;

public class Utils {

    public static final String getIpFromHosts(List<Host> hosts, int processId){
        return hosts.get(processId - 1).getIp();
    }

    public static final int getPortFromHosts(List<Host> hosts, int processId){
        return hosts.get(processId - 1).getPort();
    }
}

