package cs451.links;

import java.io.IOException;

public interface Links {
    public abstract void send(Message m) throws IOException;
    public abstract Message deliver();
    public abstract void close();
}
