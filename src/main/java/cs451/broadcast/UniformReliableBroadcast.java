package cs451.broadcast;

import cs451.Host;
import cs451.links.Message;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class UniformReliableBroadcast implements Broadcast, Runnable {

    private BestEffortBroadcast beb;
    private int processNum;
    private int ackNum[][];
    private boolean ack[][][];
    private boolean isDelivered[][][];
    private boolean reply[][];
    public LinkedBlockingQueue<Message> line;
    boolean flag;
    Host myHost;
    List<Host> hosts;

    public UniformReliableBroadcast(Host myHost, List<Host> hosts, int messageNum) throws SocketException {
        this.myHost = myHost;
        this.hosts = hosts;
        this.beb = new BestEffortBroadcast(myHost, hosts, messageNum);
        this.processNum = hosts.size();
        ack = new boolean[processNum+1][messageNum+1][processNum+1];
        isDelivered = new boolean[processNum+1][messageNum+1][processNum+1];
        ackNum= new int[processNum+1][messageNum+1];
        reply = new boolean[processNum+1][messageNum+1];
        this.line = new LinkedBlockingQueue<Message>();
        this.flag = true;
    }

    @Override
    public void broadcast(Message m) throws IOException {
        beb.broadcast(m);
    }

    @Override
    public Message deliver() throws IOException, InterruptedException {
        Message message = beb.deliver();
        if (message != null){
            //System.out.println("urb receive message: " + message.m +", "+message.id_from+", "+message.id_to+", "+message.last_hop);
            // receive ack from p
            if(!ack[message.id_from][message.m][message.last_hop]){
                ack[message.id_from][message.m][message.last_hop] = true;
                ackNum[message.id_from][message.m] ++;
                //System.out.println("urb receive message: " + message.id_from+" "+this.beb.link.stubbornLink.ackNum.get(message.id_from).get(message.m));
            }
            //origin message not broadcast ack yet
            if(!reply[message.id_from][message.m] && message.id_from != myHost.getId()){
                this.line.put(message);
                reply[message.id_from][message.m] = true;
                //System.out.println("urb ack message: " + message.id_from +", " +message.m);
            }
            // when pi already deliver m, this is a deliver ack
            if(message.is_delivered && !isDelivered[message.id_from][message.m][message.last_hop]){
                isDelivered[message.id_from][message.m][message.last_hop] = true;
                this.beb.link.stubbornLink.deliverNum.get(message.id_from).set(message.m, this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m)+1);
                //System.out.println("urb receive message: " + message.id_from+" "+message.m+" "+message.last_hop+" "+this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m));
            }
            //check if this message can deliver
            if(!isDelivered[message.id_from][message.m][myHost.getId()] && ackNum[message.id_from][message.m]+1 > processNum/2){//I won't receive ack from myself

                isDelivered[message.id_from][message.m][myHost.getId()] = true;
                Message tmp_m = new Message(message.m, message.id_to, message.id_from, message.last_hop, true);
                this.line.put(tmp_m);

                return message;
            }

        }
        return null;
    }

    @Override
    public void close() {
        this.beb.close();
        flag = false;
    }

    @Override
    public void run() {
        while(flag){
            try{
                Message pack = this.line.take();
                //System.out.println("urb broad message: " + pack.id_from+" "+pack.m+" "+pack.last_hop);
                broadcast(pack);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
