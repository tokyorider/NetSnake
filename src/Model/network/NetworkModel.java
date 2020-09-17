package Model.network;

import Model.network.listener.SocketListener;
import Model.network.messageSequenceGenerator.IMessageSequenceGenerator;
import Model.network.sender.MessageSender;
import Model.network.util.MessageInfo;
import events.Event;
import events.EventType;
import events.MessageReceiveEvent;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observable;
import util.Observer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class NetworkModel implements Observer, Observable {
    private final static int RESEND_LIMIT = 3;
    final static int RESEND_TIMEOUT = 1000;
    //Message types that can be resent not immediately
    private final static List<SnakesProto.GameMessage.TypeCase>
            nonImmediateAcknowledgeableMessageTypes =
            Arrays.asList(SnakesProto.GameMessage.TypeCase.PING, SnakesProto.GameMessage.TypeCase.ACK);

    private Collection<Observer> observers = new ConcurrentLinkedDeque<>();

    private MulticastSocket socket;

    MessageSender sender;

    SocketListener listener;

    IMessageSequenceGenerator seqGenerator;

    Map<Long, MessageInfo> messages = new ConcurrentHashMap<>();

    Timer timer = new Timer(true);

    ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public NetworkModel(MulticastSocket socket, IMessageSequenceGenerator seqGenerator) {
        this.socket = socket;
        sender = new MessageSender(socket);
        listener = new SocketListener(socket);
        this.seqGenerator = seqGenerator;
        listener.addObserver(this);

        listener.startListen();
    }

    NetworkModel(NetworkModel model) {
        socket = model.socket;
        listener = model.listener;
        sender = model.sender;
        seqGenerator = model.seqGenerator;
        listener.addObserver(this);

        model.exit();
    }

    public void start() {
        scheduleTasks();
    }

    public void exit() {
        listener.removeObserver(this);
        timer.cancel();
    }

    private void scheduleTasks() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                messages.forEach((seq, info) -> resendMsg(seq, info));
            }
        },0, RESEND_TIMEOUT);
    }

    void resendMsg(long seq, MessageInfo info) {

        if (info.resendTries == RESEND_LIMIT) {
            messages.remove(seq);
            return;
        }

        if (new Date().getTime() - info.lastSent.getTime() > RESEND_TIMEOUT) {
            if (nonImmediateAcknowledgeableMessageTypes.contains(info.msg.getTypeCase())) {
                threadPool.submit(() -> sendAcknowledgeableMsg(info.destAddress, info.msg));
            } else {
                sendAcknowledgeableMsg(info.destAddress, info.msg);
            }

            info.lastSent = new Date();
            ++info.resendTries;
        }
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public void registerHandler(SnakesProto.GameMessage.TypeCase messageType,
                                BiConsumer<InetSocketAddress, SnakesProto.GameMessage> messageHandler)
    {
        listener.addHandler(messageType, messageHandler);
    }

    public void removeHandler(SnakesProto.GameMessage.TypeCase msgType) {
        listener.removeHandler(msgType);
    }

    public void sendJoin(String playerName, InetAddress adminIp) {
        var msg = SnakesProto.GameMessage.newBuilder().
                    setJoin(SnakesProto.GameMessage.JoinMsg.newBuilder().
                            setName(playerName).
                            build()).
                    setMsgSeq(seqGenerator.generateSequence()).
                    buildPartial();
        sendAcknowledgeableMsg(adminIp, msg);
    }

    public void sendAck(InetAddress destIp, long seq, int senderId, int receiverId) {
        var msg = SnakesProto.GameMessage.newBuilder()
                .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                .setMsgSeq(seq)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .buildPartial();
        sender.sendMsg(destIp, msg);
    }

    void sendPing(InetAddress destIp, int senderId, int receiverId) {
        var msg = SnakesProto.GameMessage.newBuilder()
                    .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                    .setMsgSeq(seqGenerator.generateSequence())
                    .setSenderId(senderId)
                    .setReceiverId(receiverId)
                    .buildPartial();
        threadPool.submit(() -> sendAcknowledgeableMsg(destIp, msg));
    }

    void sendRoleChange(InetAddress destIp, int senderId, int receiverId,
                        SnakesProto.NodeRole senderRole, SnakesProto.NodeRole receiverRole)
    {
        var msg = SnakesProto.GameMessage.newBuilder()
                    .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                    .setSenderRole(senderRole)
                                    .setReceiverRole(receiverRole)
                                    .build())
                    .setMsgSeq(seqGenerator.generateSequence())
                    .setSenderId(senderId)
                    .setReceiverId(receiverId)
                    .build();
        sendAcknowledgeableMsg(destIp, msg);
    }

    void sendAcknowledgeableMsg(InetAddress destIp, SnakesProto.GameMessage msg) {
        sender.sendMsg(destIp, msg);
        messages.put(msg.getMsgSeq(), new MessageInfo(destIp, msg, new Date(), 0));
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

    @Override
    public void handleEvent(Event event) {
        if (event.getType() == EventType.MESSAGE_RECEIVE) {
            SnakesProto.GameMessage msg = ((MessageReceiveEvent) event).getMsg();
            if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                messages.remove(msg.getMsgSeq());
            }
        }
    }
}
