package cs451.broadcast;

import cs451.Host;
import cs451.links.Logger;
import cs451.links.Message;

import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LatticeBroadcast implements Broadcast, Runnable{

    private BestEffortBroadcast beb;
    private int deliverPosition[];
    private Message broadPendingMessage[];
    private int ackNum[];
    private int nackNum[];
    private boolean ack[][];
    private int proposeValue[][];
    private HashSet<Integer>[] acceptValue;
    public int currPosition;
    private boolean isDelivered[];
    private int messageNum;
    private int processNum;
    private boolean flag = true;
    public  boolean broadflag;
    Host myHost;
    List<Host> hosts;
    Logger logger;
    public LatticeBroadcast(Host myHost, List<Host> hosts, Logger logger, int messageNum) throws SocketException {
        this.myHost = myHost;
        this.logger = logger;
        this.hosts = hosts;
        this.processNum = hosts.size();
        this.messageNum = messageNum;
        this.currPosition = 1; //next broad message id
        this.beb = new BestEffortBroadcast(myHost, hosts, messageNum);
        ack = new boolean[processNum+1][messageNum+1];
        ackNum= new int[messageNum+1];
        nackNum= new int[messageNum+1];
        proposeValue = new int[messageNum+1][];
        //acceptValue = new HashSet<Integer>();
        isDelivered = new boolean[processNum+1];
        flag = true;
        this.broadflag = true;
        acceptValue = new HashSet[messageNum+1];
        for (int i = 0; i < acceptValue.length; i++) {
            acceptValue[i] = new HashSet<Integer>();
        }
    }

    @Override
    public void broadcast(Message message) throws IOException {
        beb.broadcast(message);
        proposeValue[message.no] = message.m;//propose
        this.broadflag = false;
    }


    @Override
    public Message deliver() throws IOException {
        Message message = beb.deliver();
        if (message != null){
            //System.out.println("urb receive message: " + message.m +", "+message.id_from+", "+message.id_to+", "+message.last_hop);
            // receive ack from p
            if(message.is_ack && !ack[message.id_from][message.no] && (!isDelivered[message.no])){
                ack[message.id_from][message.no] = true;
                if(message.m.length != 0){//this is a nack not ack, need to combine value
                    nackNum[message.no] ++;
                    Set<Integer> set = new HashSet<Integer>();
                    set.addAll(Arrays.stream(message.m).boxed().collect(Collectors.toList()));
                    set.addAll(Arrays.stream(proposeValue[message.no]).boxed().collect(Collectors.toList()));
                    proposeValue[message.no] = Arrays.stream(set.toArray(new Integer[0])).mapToInt(Integer::intValue).toArray();

                }
                else {
                    ackNum[message.no] ++;
                }
                //System.out.println("urb receive message: " + message.id_from+" "+this.beb.link.stubbornLink.ackNum.get(message.id_from).get(message.m));
            }
            if(!message.is_ack){// origin message
                int crr_size = acceptValue[message.no].size();
                acceptValue[message.no].addAll(Arrays.stream(message.m).boxed().collect(Collectors.toList()));
                if(acceptValue[message.no].size() == crr_size){ //send ack
                    Message tmp_m = new Message(message.no, new int[0], message.id_from, myHost.getId(), true);
                    beb.broadcast(tmp_m);
                }
                else{ //send nack
                    int[] tmp = new int[acceptValue[message.no].size()];
                    int tmp_i = 0;
                    for(Integer tmp_v : acceptValue[message.no]){
                        tmp[tmp_i] = tmp_v;
                        tmp_i++;
                    }
                    Message tmp_m = new Message(message.no, tmp, message.id_from, myHost.getId(), true);
                    beb.broadcast(tmp_m);
                }

                }

            }
            // when pi already deliver m, this is a deliver ack
            //if(message.is_delivered && !isDelivered[message.id_from][message.m][message.last_hop]){
            //    isDelivered[message.id_from][message.m][message.last_hop] = true;
            //    this.beb.link.stubbornLink.deliverNum.get(message.id_from).set(message.m, this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m)+1);
            //System.out.println("urb receive message: " + message.id_from+" "+message.m+" "+message.last_hop+" "+this.beb.link.stubbornLink.deliverNum.get(message.id_from).get(message.m));
            //}
            //check if this message can deliver
            if((!isDelivered[message.no]) &&( ackNum[message.no] >= processNum/2+1)){//decide
                isDelivered[message.no] = true;
                Message tmp_m = new Message(message.no, proposeValue[message.no], message.id_from, myHost.getId(), false);
                return tmp_m;
            }
        return null;
    }

    @Override
    public void close() {
        flag = false;
    }

    @Override
    public void run() {
        System.out.println("fifo run "+flag);
        while (flag){
            //broadcast first
            if((ackNum[currPosition]+nackNum[currPosition] >= processNum/2+1) && (nackNum[currPosition]>0 )){
                broadflag = true;
                currPosition++;
            }
            //deliver
            Message receivedMessage = null;
            try {
                receivedMessage = deliver();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (receivedMessage != null){
                //log
                //System.out.println("fifo deliver: " + receivedMessage.id_from+", "+receivedMessage.m);
                String decided = "";
                for (int i = 0; i < receivedMessage.m.length; i++) {
                    if(i ==0){
                        decided = String.valueOf(receivedMessage.m[i]);
                    }
                    else{
                        decided += " ";
                        decided += String.valueOf(receivedMessage.m[i]);
                    }
                }
                String logContent = decided + "\n";
                logger.log(logContent);
            }
        }
    }
}
