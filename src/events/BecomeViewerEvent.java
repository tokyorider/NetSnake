package events;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;

public class BecomeViewerEvent extends Event {
    private int selfId, adminId;

    private InetAddress adminIp;

    private SnakesProto.GameState gameState;

    public BecomeViewerEvent(int selfId, int adminId,
                             InetAddress adminIp, SnakesProto.GameState gameState)
    {
        super(EventType.BECOME_VIEWER);

        this.selfId = selfId;
        this.adminId = adminId;
        this.adminIp = adminIp;
        this.gameState = gameState;
    }

    public int getSelfId() {
        return selfId;
    }

    public int getAdminId() {
        return adminId;
    }

    public InetAddress getAdminIp() {
        return adminIp;
    }

    public SnakesProto.GameState getGameState() {
        return gameState;
    }
}
