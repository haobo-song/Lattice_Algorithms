package cs451.broadcast;

import cs451.Host;
import cs451.links.Message;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class HalfLatticeBroadcast implements Broadcast, Runnable {

    private BestEffortBroadcast beb;
    private int processNum;
    private int ackNum[];
    private int nackNum[];
    private boolean ack[][];
    private boolean isDelivered[][][];
    private boolean reply[][];
    private int decideValue[][];
    public LinkedBlockingQueue<Message> line;
    boolean flag;
    Host myHost;
    List<Host> hosts;

    public HalfLatticeBroadcast(Host myHost, List<Host> hosts, int messageNum) throws SocketException {
        this.myHost = myHost;
        this.hosts = hosts;
        this.beb = new BestEffortBroadcast(myHost, hosts, messageNum);
        this.processNum = hosts.size();
        ack = new boolean[processNum+1][messageNum+1];
        isDelivered = new boolean[processNum+1][messageNum+1][processNum+1];
        ackNum= new int[messageNum+1];
        nackNum= new int[messageNum+1];
        reply = new boolean[processNum+1][messageNum+1];
        decideValue = new int[messageNum+1][];
        this.line = new LinkedBlockingQueue<Message>();
        this.flag = true;
    }

    @Override
    public void broadcast(Message m) throws IOException {
        beb.broadcast(m);
    }

    @Override
    //when reach f+1, deliver
    public Message deliver() throws IOException, InterruptedException {
        Message message = beb.deliver();
        if (message != null){
            //System.out.println("urb receive message: " + message.m +", "+message.id_from+", "+message.id_to+", "+message.last_hop);
            // receive ack from p
            if(message.is_ack && !ack[message.id_from][message.no]){
                ack[message.id_from][message.no] = true;
                ackNum[message.no] ++;
                if(message.m.length != 0){//this is a nack not ack, need to combine value
                    nackNum[message.no] ++;
                    Set<Integer> set = new HashSet<Integer>();
                    set.addAll(Arrays.stream(message.m).boxed().collect(Collectors.toList()));
                    set.addAll(Arrays.stream(decideValue[message.no]).boxed().collect(Collectors.toList()));
                    decideValue[message.no] = Arrays.stream(set.toArray(new Integer[0])).mapToInt(Integer::intValue).toArray();

                }
                //System.out.println("urb receive message: " + message.id_from+" "+this.beb.link.stubbornLink.ackNum.get(message.id_from).get(message.m));
            }
            // when pi already deliver m, this is a deliver ack
            //if(message.is_delivered && !isDelivered[message.id_from][message.m][message.last_hop]){
            //    isDelivered[message.id_from][message.m][message.last_hop] = true;
            //    this.beb.link.stubbornLink.deliverNum.get(message.id_from).set(message.m, this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m)+1);
                //System.out.println("urb receive message: " + message.id_from+" "+message.m+" "+message.last_hop+" "+this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m));
            //}
            //check if this message can deliver
            if(ackNum[message.no]+1 > processNum/2){//I won't receive ack from myself

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
