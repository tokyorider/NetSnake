package Model.network;

import Model.network.util.NodeInfo;
import events.Event;
import events.NodeDisconnectedEvent;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;
import java.util.Date;
import java.util.TimerTask;

public class CommonUserNetworkModel extends NetworkModel {
    private int pingDelayMs, nodeTimeoutMs, selfId, adminId;

    private NodeInfo adminInfo;

    public CommonUserNetworkModel(int pingDelayMs, int nodeTimeoutMs, int selfId, int adminId,
            InetAddress adminIp, NetworkModel networkModel)
    {
        super(networkModel);

        this.pingDelayMs = pingDelayMs;
        this.nodeTimeoutMs = nodeTimeoutMs;
        this.selfId = selfId;
        this.adminId = adminId;
        adminInfo = new NodeInfo(adminIp, new Date(), new Date());
    }

    public void start() {
        super.start();
        scheduleTasks();
    }

    public void startOnlyView() {
        super.start();
        scheduleViewerTasks();
    }

    private void scheduleTasks() {
        scheduleViewerTasks();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (new Date().getTime() - adminInfo.lastTheirActivity.getTime() > nodeTimeoutMs) {
                    notifyObservers(new NodeDisconnectedEvent(adminId));
                }
            }
        }, 0, nodeTimeoutMs);
    }

    private void scheduleViewerTasks() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                messages.forEach((seq, info) -> {
                    resendMsg(seq, info);
                    adminInfo.lastOurActivity = new Date();
                });
            }
        },0, RESEND_TIMEOUT);


        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (new Date().getTime() - adminInfo.lastOurActivity.getTime() > pingDelayMs) {
                    sendPing(adminInfo.address, selfId, adminId);
                    adminInfo.lastOurActivity = new Date();
                }
            }
        }, 0, pingDelayMs);
    }

    public void sendSteer(SnakesProto.Direction direction)
    {
        var msg = SnakesProto.GameMessage.newBuilder().
                setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().
                        setDirection(direction).
                        build()).
                setMsgSeq(seqGenerator.generateSequence()).
                setSenderId(selfId).
                setReceiverId(adminId).
                buildPartial();
        sendAcknowledgeableMsg(adminInfo.address, msg);

        adminInfo.lastOurActivity = new Date();
    }

    public void sendRoleChange(SnakesProto.NodeRole senderRole,
                               SnakesProto.NodeRole receiverRole)
    {
        super.sendRoleChange(adminInfo.address, selfId, adminId, senderRole, receiverRole);

        adminInfo.lastOurActivity = new Date();
    }

    public void sendAck(long seq) {
        super.sendAck(adminInfo.address, seq, selfId, adminId);

        adminInfo.lastOurActivity = new Date();
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public void setAdminIp(InetAddress adminIp) {
        adminInfo.address = adminIp;
    }

    @Override
    public void handleEvent(Event event) {
        super.handleEvent(event);

        adminInfo.lastTheirActivity = new Date();
    }
}
