package cs451.broadcast;

import cs451.Host;
import cs451.links.Message;
import cs451.links.PerfectLinks;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public class BestEffortBroadcast implements Broadcast{

    List<Host> hosts;
    Host myHost;
    public  PerfectLinks link;

    public BestEffortBroadcast(Host myHost, List<Host> hosts, int messageNum) throws SocketException {
        this.myHost = myHost;
        this.link = new PerfectLinks(myHost.getPort(), hosts, messageNum);
        this.hosts = hosts;
    }

    @Override
    public void broadcast(Message message) throws IOException {
        if (message.id_to == -1){//broadcast
            for (int i = 0; i < hosts.size(); i++) {
                if (hosts.get(i).getId() != this.myHost.getId()) {
                    Message tmp_m = new Message(message.no, message.proposal_num, message.m, hosts.get(i).getId(), message.id_from, message.is_ack);
                    link.send(tmp_m);
                }
            }
        }
        else {//p2p
            link.send(message);
        }

    }

    @Override
    public Message deliver() {
        return link.deliver();
    }

    @Override
    public void close() {
        link.close();
    }
}