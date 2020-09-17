package Model.network.listener;

import events.Event;
import events.MessageReceiveEvent;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observable;
import util.Observer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;

public class SocketListener extends Thread implements Observable {
    private final static int PACKET_SIZE = 1 << 16;

    private Collection<Observer> observers = new ConcurrentLinkedDeque<>();

    private MulticastSocket socket;

    private Map<SnakesProto.GameMessage.TypeCase, BiConsumer<InetSocketAddress, SnakesProto.GameMessage>>
            messageHandlers = new ConcurrentHashMap<>();

    public SocketListener(MulticastSocket socket) {
        this.socket = socket;
    }

    public void addHandler(SnakesProto.GameMessage.TypeCase messageType,
                                BiConsumer<InetSocketAddress, SnakesProto.GameMessage> messageHandler)
    {
        messageHandlers.put(messageType, messageHandler);
    }

    public void removeHandler(SnakesProto.GameMessage.TypeCase msgType) {
        messageHandlers.remove(msgType);
    }

    public void startListen() {
        start();
    }

    @Override
    public void run() {
        byte[] recvArray = new byte[PACKET_SIZE];

        while(true) {
                DatagramPacket recvPacket = new DatagramPacket(recvArray, PACKET_SIZE);
                try {
                    socket.receive(recvPacket);
                    SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(
                                Arrays.copyOf(recvPacket.getData(), recvPacket.getLength()));
                    if (new Random().nextInt(100) < 100) {
                        {
                            SnakesProto.GameMessage.TypeCase msgType = msg.getTypeCase();
                            if (messageHandlers.get(msgType) != null) {
                                messageHandlers.get(msgType).accept(
                                        (InetSocketAddress) recvPacket.getSocketAddress(), msg);
                            }

                            if (msg.hasSenderId()) {
                                notifyObservers(new MessageReceiveEvent(msg));
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Event event) {
        observers.forEach((observer) -> observer.handleEvent(event));
    }
}
