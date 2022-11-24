package cs451.broadcast;

import cs451.Host;
import cs451.links.Logger;
import cs451.links.Message;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import static java.lang.Math.min;

public class FifoBroadcast implements Broadcast, Runnable{

    private int deliverPosition[];
    private boolean pending[][];
    private Message broadQueue[];
    private int broadPosition;
    private int broadQueueSize;
    private UniformReliableBroadcast urb;
    Thread threadUrb;
    private int messageNum;
    private int processNum;
    private boolean flag = true;
    Host myHost;
    List<Host> hosts;
    Logger logger;
    float iii;
    public  FifoBroadcast(Host myHost, List<Host> hosts, Logger logger, int messageNum) throws SocketException {
        this.myHost = myHost;
        this.logger = logger;
        this.hosts = hosts;
        this.processNum = hosts.size();
        this.messageNum = messageNum;
        this.broadPosition = 1; //next broad message id
        this.broadQueueSize = 0;
        //this.broadQueue = new Message[messageNum+1];
        urb = new UniformReliableBroadcast(myHost, hosts, messageNum);
        threadUrb = new Thread(this.urb);
        threadUrb.start();
        pending = new boolean[processNum+1][messageNum+1];
        deliverPosition = new int[processNum+1];
        //iii = 0;
        flag = true;
        //System.out.println("open ready");
    }

    @Override
    public void broadcast(Message message) throws IOException {
        //implement rolling window
        //broadQueue[message.m] = message;
        broadQueueSize = message.m; //message from main, ready to broadcast

    }

    @Override
    public Message deliver() throws IOException {
        Message message = null;
        try {
            message = urb.deliver();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(message != null) {
            //System.out.println("fifo receive message: " + message.m +", "+message.id_from+", "+message.last_hop);
            // is able to deliver
            if (deliverPosition[message.id_from] == message.m - 1) {
                deliverPosition[message.id_from]++;
                return message;
            }
            pending[message.id_from][message.m] = true;
        }
        for (int i = 0; i < hosts.size(); i++) {
            if (deliverPosition[hosts.get(i).getId()]<messageNum) {
                int tmp_pos = deliverPosition[hosts.get(i).getId()]+1; //current deliver position of pi
                if (pending[hosts.get(i).getId()][tmp_pos] != false){ //message tmp_pos is waiting to deliver
                    deliverPosition[hosts.get(i).getId()]++;
                    Message tmp = new Message(tmp_pos, -1, hosts.get(i).getId(), hosts.get(i).getId(), false);
                    return tmp;
                }
            }
        }
        return null;
    }

    @Override
    public void close() {
        this.urb.close();
        flag = false;
    }

    @Override
    public void run() {
        System.out.println("fifo run "+flag);
        while (flag){
            //broadcast first
            if(broadPosition<broadQueueSize+1 && broadPosition < deliverPosition[myHost.getId()]+50){
                for (int i = broadPosition; i < min(broadQueueSize+1, deliverPosition[myHost.getId()]+100); i++){
                    try {
                        //System.out.println("fifo broad: " + i+" "+deliverPosition[myHost.getId()]);
                        Message message = new Message(i, -1, myHost.getId(), myHost.getId(), false);
                        //this.urb.line.put(broadQueue[i]);
                        this.urb.line.put(message);
                        broadPosition++;
                        String log = 'b' + " " + message.m + "\n";
                        logger.log(log);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //String log = 'b' + " " + message.m + "\n";
                    //logger.log(log);
                }
            }
            if(broadPosition == messageNum){

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
                String logContent = 'd' + " " + receivedMessage.id_from + " " + receivedMessage.m + "\n";
                logger.log(logContent);
            }
        }
    }
}
