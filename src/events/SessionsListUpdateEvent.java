package events;

import javafx.scene.layout.GridPane;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.InetAddress;
import java.util.Map;

public class SessionsListUpdateEvent extends Event{
    private Map<InetAddress, SnakesProto.GameConfig> sessionInfos;

    private GridPane sessionsListPane;

    public SessionsListUpdateEvent(Map<InetAddress, SnakesProto.GameConfig> sessionInfos,
                                   GridPane sessionsListPane)
    {
        super(EventType.SESSIONS_LIST_UPDATE);

        this.sessionInfos = sessionInfos;
        this.sessionsListPane = sessionsListPane;
    }

    public Map<InetAddress, SnakesProto.GameConfig> getSessionInfos() {
        return sessionInfos;
    }

    public GridPane getSessionsListPane() {
        return sessionsListPane;
    }
}
