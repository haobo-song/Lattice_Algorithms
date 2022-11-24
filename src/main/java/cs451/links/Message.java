package cs451.links;

import java.io.Serializable;

public class Message implements Serializable {
    public int m;
    public  int id_to;
    public int id_from; //origin id
    public  int last_hop;
    public  boolean is_delivered;
    public Message(int m, int id_to, int id_from, int last_hop, boolean is_delivered) {

        this.m = m;
        this.id_to = id_to;
        this.id_from = id_from; //-3: send out message
        this.last_hop = last_hop; //last hop
        this.is_delivered = is_delivered;

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        Message message = (Message) obj;
        if ((id_from == message.id_from)  && (id_to == message.id_to) && (m == message.m) && (last_hop == message.last_hop) && (is_delivered == message.is_delivered)) return true;
        return false;
    }

    @Override
    public int hashCode() {

        return (String.valueOf(this.m)+String.valueOf(this.id_to)+String.valueOf(this.id_from)+String.valueOf(this.last_hop)+String.valueOf(this.is_delivered)).hashCode();
    }
}
