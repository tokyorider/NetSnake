package events;

import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;

public class BecomeAdminEvent extends Event {
    private int selfId;

    private InetAddress oldAdminIp;

    private SnakesProto.GameState lastGameState;

    public BecomeAdminEvent(int selfId, InetAddress oldAdminIp,
                            SnakesProto.GameState lastGameState)
    {
        super(EventType.BECOME_ADMIN);

        this.selfId = selfId;
        this.oldAdminIp = oldAdminIp;
        this.lastGameState = lastGameState;
    }

    public int getSelfId() {
        return selfId;
    }

    public InetAddress getOldAdminIp() {
        return oldAdminIp;
    }

    public SnakesProto.GameState getLastGameState() {
        return lastGameState;
    }
}
