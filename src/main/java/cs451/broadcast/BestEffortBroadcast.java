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
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).getId() != this.myHost.getId()) {
                Message tmp_m = new Message(message.m, hosts.get(i).getId(), message.id_from, myHost.getId(), message.is_delivered);
                link.send(tmp_m);
            }
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