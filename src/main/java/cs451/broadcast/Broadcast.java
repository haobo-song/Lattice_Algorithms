package cs451.broadcast;

import cs451.links.Message;

import java.io.IOException;

public interface Broadcast {

    public abstract void broadcast(Message m) throws IOException;
    public abstract Message deliver() throws IOException, InterruptedException;
    public abstract void close();
}
