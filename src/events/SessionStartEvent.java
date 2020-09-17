package events;

import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import me.ippolitov.fit.snakes.SnakesProto;

public class SessionStartEvent extends Event {
    private SnakesProto.GameConfig config;

    private ISnakeCoordWrapper wrapper;

    public SessionStartEvent(SnakesProto.GameConfig config, ISnakeCoordWrapper wrapper)
    {
        super(EventType.SESSION_START);
        this.config = config;
        this.wrapper = wrapper;
    }

    public SnakesProto.GameConfig getConfig() {
        return config;
    }

    public ISnakeCoordWrapper getCoordsWrapper() {
        return wrapper;
    }
}
