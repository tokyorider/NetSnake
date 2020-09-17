package Model.network.sender;

import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class MessageSender {
    private static final int destPort = 9192;

    private DatagramSocket socket;

    public MessageSender(DatagramSocket socket) {
        this.socket = socket;
    }

    public void sendMsg(InetAddress destIp, SnakesProto.GameMessage msg) {
        byte[] rawData = msg.toByteArray();
        try {
            socket.send(new DatagramPacket(rawData, rawData.length, new InetSocketAddress(destIp, destPort)));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
