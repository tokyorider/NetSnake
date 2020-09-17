package Model.network.util;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;
import java.util.Date;

public class MessageInfo {
    public InetAddress destAddress;

    public SnakesProto.GameMessage msg;

    public Date lastSent;

    public int resendTries;

    public MessageInfo(InetAddress destAddress, SnakesProto.GameMessage msg, Date lastSent, int resendTries) {
        this.destAddress = destAddress;
        this.msg = msg;
        this.lastSent = lastSent;
        this.resendTries = resendTries;
    }
}
