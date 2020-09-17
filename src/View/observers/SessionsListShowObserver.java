package View.observers;

import events.Event;
import events.EventType;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.Observer;

public class SessionsListShowObserver implements Observer {
    private Stage stage;

    private Scene sessionsListScene;

    public SessionsListShowObserver(Stage stage, Scene sessionsListScene) {
        this.stage = stage;
        this.sessionsListScene = sessionsListScene;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType() == EventType.SESSIONS_SHOW) {
            stage.setScene(sessionsListScene);
            stage.show();
        }
    }
}
