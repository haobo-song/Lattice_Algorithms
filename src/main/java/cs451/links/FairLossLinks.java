package cs451.links;

import cs451.Host;

import java.io.*;
import java.net.*;
import java.util.List;

public class FairLossLinks implements Links{
    DatagramSocket socket;
    List hosts;


    public FairLossLinks(int port, List<Host> hosts) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.hosts = hosts;

    }

    @Override
    public void send(Message message ) throws IOException {

        InetAddress ip = getIpFromHosts(hosts, message.id_to);
        int port = getPortFromHosts(hosts, message.id_to);
        //System.out.println("fairloss send message: " + message.m +", "+port+", "+message.id_to);
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(dataStream);
        objectStream.writeObject(message);
        objectStream.flush();
        byte[] b_message = dataStream.toByteArray();
        DatagramPacket packet = new DatagramPacket(b_message, b_message.length, ip, port);
        socket.send(packet);

    }


    @Override
    public Message deliver() {
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            socket.receive(packet);
            try{
                ByteArrayInputStream dataStream = new ByteArrayInputStream(packet.getData());
                ObjectInputStream objectStream = new ObjectInputStream(dataStream);
                Message data = (Message) objectStream.readObject();
                //String ip = packet.getAddress().getHostAddress();
                //int port = packet.getPort();
                //Message received_message = new Message(data, ip, port, -1);
                //System.out.println("perfect receive message: " + data.m +", "+data.id_from+", "+data.last_hop);
                return data;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void close(){ socket.close();}
        //System.out.println("socket closed");}


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


}
