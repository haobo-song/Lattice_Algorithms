package cs451.links;

import cs451.Host;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PerfectLinks implements Links{
    public StubbornLinks stubbornLink;
    Thread threadStubborn;
    Set delivered;
    List hosts;


    public PerfectLinks(int port, List hosts, int messageNum) throws SocketException {
        this.stubbornLink = new StubbornLinks(port, hosts, messageNum);
        threadStubborn = new Thread(this.stubbornLink);
        threadStubborn.start();
        this.delivered = new HashSet<Message>();
        this.hosts = hosts;
    }

    @Override
    public void send(Message message) throws IOException {
        //System.out.println("perfect send message: " + m);
        try {
            stubbornLink.line.put(message);
            //System.out.println("perfect send message: " + message.m +", "+message.id_to);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message deliver() {
        Message receivedMessage = stubbornLink.deliver();
        if (receivedMessage != null){
            if (delivered.contains(receivedMessage)) {
                return null;
            } else {
                //System.out.println("perfect receive message: " + receivedMessage.m +", "+receivedMessage.id_from+", "+receivedMessage.last_hop);
                //System.out.println("perfect receive message: " + receivedMessage.m +", "+receivedMessage.id_from+", "+receivedMessage.id_to+", "+receivedMessage.last_hop);
                //System.out.println("perfect receive message hash: " + receivedMessage.hashCode() );
                delivered.add(receivedMessage);
                return receivedMessage;
            }
        }

        return null;
    }



    @Override
    public void close(){
        stubbornLink.close();
    }


    public static InetAddress getIpFromHosts(List<Host> hosts, int id) {
        String ip = hosts.get(id - 1).getIp();
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getPortFromHosts(List<Host> hosts, int id){
        Integer port = hosts.get(id - 1).getPort();
        return port;
    }
    public static int getIdFromHosts(List<Host> hosts, String ip, int port){
        for(int i = 1 ; i < hosts.size()+1 ; i++){
            int port_i = getPortFromHosts(hosts, i);
            String ip_i = getIpFromHosts(hosts, i).getHostAddress();
            if ((port_i == port ) & (ip_i.equals(ip))){
                return i;
            }
        }
        return -2; //can't find
    }
}


