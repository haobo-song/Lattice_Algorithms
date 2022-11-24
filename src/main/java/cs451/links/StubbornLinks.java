package cs451.links;

import cs451.Host;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class StubbornLinks implements Links, Runnable{
    private FairLossLinks fairLossLink;
    public int MAX_TIME = 1000;
    boolean flag = true;
    public LinkedBlockingQueue<Message> line;
    List hosts;
    private int processNum;
    public CopyOnWriteArrayList<ArrayList<Integer>> deliverNum;
    float iii;


    public StubbornLinks(int port, List<Host> hosts, int messageNum) throws SocketException {
        this.fairLossLink = new FairLossLinks(port, hosts);
        this.line = new LinkedBlockingQueue<Message>();
        this.hosts = hosts;
        this.processNum = hosts.size();
        this.deliverNum = new CopyOnWriteArrayList<ArrayList<Integer>>();
        for(int i=0; i < processNum+1; i++) {
            deliverNum.add(new ArrayList<Integer>(Collections.nCopies(messageNum+1, 0)));
        }

    }

    @Override
    public void send(Message message) throws IOException{
        fairLossLink.send(message);
    }


    @Override
    public Message deliver() {
        return fairLossLink.deliver();
    }

    @Override
    public void run(){
        while(flag){
            try{
                Message pack = this.line.take();
                send(pack);
                //System.out.println("stubborn: "+pack.id_from+" "+pack.m);
                if(this.deliverNum.get(pack.id_from).get(pack.m) < this.processNum-1){
                    line.put(pack);
                }
                //else{
                //    System.out.println("delete: "+pack.id_from+" "+pack.m);
                //}

            }catch (InterruptedException e){
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close(){
        flag = false;
        fairLossLink.close();
    }
}


