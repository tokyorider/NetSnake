package events;

import me.ippolitov.fit.snakes.SnakesProto;

public class SessionRenderEvent extends Event {
    private SnakesProto.GameState state;

    public SessionRenderEvent(SnakesProto.GameState state) {
        super(EventType.SESSION_RENDER);
        this.state = state;
    }

    public SnakesProto.GameState getState() {
        return state;
    }
}
