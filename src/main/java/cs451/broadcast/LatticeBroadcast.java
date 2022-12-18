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
    private int ackNum;
    private int nackNum;
    private boolean ack[];
    private int proposeValue[][];
    private HashSet<Integer>[] acceptValue;
    public int proposePosition;
    public int decidePosition;
    public int proposalActiveNumber;
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
        this.proposePosition = 1; //current broad message id
        this.decidePosition = 1;
        this.proposalActiveNumber=0;
        this.beb = new BestEffortBroadcast(myHost, hosts, messageNum);
        ack = new boolean[processNum+1];
        ackNum= 0;
        nackNum= 0;
        proposeValue = new int[1][];
        isDelivered = new boolean[messageNum+1];
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
        proposeValue[0] = message.m;//propose
        int tmp_size = proposeValue[0].length;
        acceptValue[message.no].addAll(Arrays.stream(proposeValue[0]).boxed().collect(Collectors.toList()));
        if(acceptValue[message.no].size()==tmp_size){//message.m is bigger than propose, acceptor gives back ack
            ackNum++;
            //System.out.println("repropose: "+message.no+" "+message.proposal_num+" "+Arrays.toString(message.m)+" ack+");
        }
        else{//acceptor gives back nack
            nackNum++;
            //System.out.println("repropose: "+message.no+" "+message.proposal_num+" "+Arrays.toString(message.m)+" nack+");
        }
        this.broadflag = false;
    }


    @Override
    public Message deliver() throws IOException {
        Message message = beb.deliver();
        if (message != null) {
            // receive ack from p
            //System.out.println("receive: " + message.no+" "+message.proposal_num+" "+message.id_from+" "+Arrays.toString(message.m)+" "+message.is_ack);
            if (message.is_ack && !ack[message.id_from] && (this.proposalActiveNumber==message.proposal_num) && (proposePosition == message.no) && (!isDelivered[message.no])) {
                ack[message.id_from] = true;
                //System.out.println("receive ack: " + message.no+" "+message.proposal_num+" "+message.id_from+" "+Arrays.toString(message.m));
                if (message.m.length != 0) {//this is a nack not ack, need to combine value
                    nackNum++;
                    Set<Integer> set = new HashSet<Integer>();
                    set.addAll(Arrays.stream(message.m).boxed().collect(Collectors.toList()));
                    set.addAll(Arrays.stream(proposeValue[0]).boxed().collect(Collectors.toList()));
                    proposeValue[0] = Arrays.stream(set.toArray(new Integer[0])).mapToInt(Integer::intValue).toArray();

                } else {
                    ackNum++;
                }
                //System.out.println("urb receive message: " + message.id_from+" "+this.beb.link.stubbornLink.ackNum.get(message.id_from).get(message.m));
            }
            if (!message.is_ack) {// origin message
                //System.out.println("receive origin: " + message.no+" "+message.proposal_num+" "+message.id_from+" "+Arrays.toString(message.m));
                int crr_size = message.m.length;
                acceptValue[message.no].addAll(Arrays.stream(message.m).boxed().collect(Collectors.toList()));
                if (acceptValue[message.no].size() == crr_size) { //send ack
                    //System.out.println("send ack: " + message.no+" "+message.proposal_num+" "+message.id_from);
                    Message tmp_m = new Message(message.no, message.proposal_num, new int[0], message.id_from, myHost.getId(), true);
                    beb.broadcast(tmp_m);
                }
                else { //send nack
                    int[] tmp = new int[acceptValue[message.no].size()];
                    int tmp_i = 0;
                    for (Integer tmp_v : acceptValue[message.no]) {
                        tmp[tmp_i] = tmp_v;
                        tmp_i++;
                    }
                    //System.out.println("send nack: " + message.no+" "+message.proposal_num+" "+message.id_from+" "+Arrays.toString(tmp));
                    Message tmp_m = new Message(message.no, message.proposal_num, tmp, message.id_from, myHost.getId(), true);
                    beb.broadcast(tmp_m);
                }

            }
        }
        //check if this message can deliver
        if ((decidePosition<messageNum+1) && (!isDelivered[decidePosition]) && ((ackNum) >= processNum / 2+1)) {//decide
            isDelivered[decidePosition] = true;
            System.out.println("fifo deliver: " + decidePosition);
            Message tmp_m = new Message(decidePosition, -1, proposeValue[0], -1, myHost.getId(), false);
            decidePosition ++;
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
            if((proposePosition <= messageNum)&&((ackNum+nackNum) >= processNum/2+1) && (nackNum>0)){ //repropose again
                proposalActiveNumber++;
                ackNum = 0;
                nackNum = 0;
                for (int i = 0; i < processNum+1; i++) {
                    ack[i] = false;
                }
                Message broadMessage = new Message(proposePosition, proposalActiveNumber, proposeValue[0], -1, myHost.getId(), false);
                try {
                    broadcast(broadMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //System.out.println("update propose pos: "+proposePosition);
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
                //System.out.println("fifo decide: " + receivedMessage.no+" "+proposePosition);
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
                // move to next propose
                proposalActiveNumber=0;
                ackNum = 0;
                nackNum = 0;
                ack = new boolean[processNum+1];
                broadflag = true;
                proposePosition++;

            }
        }
    }
}
