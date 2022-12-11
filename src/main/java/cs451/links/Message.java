package cs451.links;

import java.io.Serializable;

public class Message implements Serializable {
    public int[] m;
    public  int no;
    public  int id_to;
    public int id_from; //origin id
    public  boolean is_ack;
    public Message(int no, int[] m, int id_to, int id_from, boolean is_ack) {
        this.no = no;
        this.m = m;
        this.id_to = id_to;
        this.id_from = id_from;
        this.is_ack = is_ack; //true: this is m delivering ack; false: this is m receiving ack

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        Message message = (Message) obj;
        if ((id_from == message.id_from)  && (id_to == message.id_to) && (no == message.no) && (m.equals(message.m)) && (is_ack == message.is_ack)) return true;
        return false;
    }

    @Override
    public int hashCode() {

        return (String.valueOf(this.no)+this.m.hashCode()+String.valueOf(this.id_to)+String.valueOf(this.id_from)+String.valueOf(this.is_ack)).hashCode();
    }
}
