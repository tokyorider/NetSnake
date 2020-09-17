package Model.network;

import Model.network.util.NodeInfo;
import events.Event;
import events.EventType;
import events.MessageReceiveEvent;
import events.NodeDisconnectedEvent;
import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class AdminNetworkModel extends NetworkModel {
    private static InetAddress multicastAddress;
    static {
        try {
            multicastAddress = InetAddress.getByName("239.192.0.4");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private int pingDelayMs, nodeTimeoutMs, selfId;

    private Map<Integer, NodeInfo> nodes = new ConcurrentHashMap<>();

    public AdminNetworkModel(int pingDelayMs, int nodeTimeoutMs, NetworkModel model) {
        super(model);
        listener.addObserver(this);

        this.pingDelayMs = pingDelayMs;
        this.nodeTimeoutMs = nodeTimeoutMs;
        listener.addHandler(SnakesProto.GameMessage.TypeCase.PING, (source, msg) -> {
            sendAck(msg.getMsgSeq(), msg.getSenderId());
        });
    }

    public void start() {
        timer.cancel();
        timer = new Timer(true);
        scheduleTasks();
    }

    public void setSelfId(int selfId) {
        this.selfId = selfId;
    }

    public void addNode(int id, String nodeAddress) {
        try {
            nodes.put(id, new NodeInfo(InetAddress.getByName(nodeAddress),
                    new Date(), new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scheduleTasks() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                messages.forEach((seq, info) -> {
                    resendMsg(seq, info);
                    if (nodes.get(info.msg.getSenderId()) != null) {
                        nodes.get(info.msg.getSenderId()).lastOurActivity = new Date();
                    }
                });
            }
        },0, RESEND_TIMEOUT);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                nodes.forEach((id, nodeInfo) -> {
                    if (new Date().getTime() - nodeInfo.lastOurActivity.getTime() > pingDelayMs) {
                        sendPing(nodeInfo.address, selfId, id);
                        nodes.get(id).lastOurActivity = new Date();
                    }
                });
            }
        }, 0, pingDelayMs);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                nodes.forEach((id, nodeInfo) -> {
                    if (new Date().getTime() - nodeInfo.lastTheirActivity.getTime() > nodeTimeoutMs) {
                        notifyObservers(new NodeDisconnectedEvent(id));
                        nodes.remove(id);
                    }
                });
            }
        }, 0, nodeTimeoutMs);
    }

    public void sendAnnouncement(SnakesProto.GameState gameState) {
        var msg =  SnakesProto.GameMessage.newBuilder().
                       setAnnouncement(SnakesProto.GameMessage.AnnouncementMsg.newBuilder().
                                        setPlayers(gameState.getPlayers()).
                                        setConfig(gameState.getConfig()).
                                        buildPartial()).
                        setMsgSeq(seqGenerator.generateSequence()).
                        buildPartial();
        threadPool.submit(() -> sender.sendMsg(multicastAddress, msg));
    }

    public void sendState(SnakesProto.GameState gameState, int receiverId) {
        var info = nodes.get(receiverId);
        if (info != null) {
            var msg =  SnakesProto.GameMessage.newBuilder().
                    setState(SnakesProto.GameMessage.StateMsg.newBuilder().
                            setState(gameState).
                            buildPartial()).
                    setMsgSeq(seqGenerator.generateSequence()).
                    setSenderId(selfId).
                    setReceiverId(receiverId).
                    buildPartial();
            sendAcknowledgeableMsg(info.address, msg);

            info.lastOurActivity = new Date();
        }
    }

    public void sendRoleChange(int receiverId, SnakesProto.NodeRole senderRole,
                               SnakesProto.NodeRole receiverRole)
    {
        var info = nodes.get(receiverId);
        if (info != null) {
            super.sendRoleChange(info.address, selfId, receiverId, senderRole, receiverRole);
            info.lastOurActivity = new Date();
        }
    }

    public void sendAck(long seq, int receiverId) {
        var info = nodes.get(receiverId);
        if (info != null) {
            super.sendAck(info.address, seq, selfId, receiverId);
            info.lastOurActivity = new Date();
        }
    }

    public void sendErr(InetAddress destIp, String errMsg) {
        var msg = SnakesProto.GameMessage.newBuilder()
                    .setError(SnakesProto.GameMessage.ErrorMsg.newBuilder()
                             .setErrorMessage(errMsg)
                             .build())
                    .setMsgSeq(seqGenerator.generateSequence())
                    .setSenderId(selfId)
                    .setReceiverId(-1)
                    .build();
        threadPool.submit(() -> sendAcknowledgeableMsg(destIp, msg));
    }

    @Override
    public void handleEvent(Event event) {
        super.handleEvent(event);
        if (event.getType() == EventType.MESSAGE_RECEIVE) {
            int senderId = ((MessageReceiveEvent) event).getMsg().getSenderId();
            if (nodes.get(senderId) != null) {
                nodes.get(senderId).lastTheirActivity = new Date();
            }
        }
    }
}
