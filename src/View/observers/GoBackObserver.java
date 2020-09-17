package View.observers;

import events.Event;
import events.EventType;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.Observer;

public class GoBackObserver implements Observer {
    private Stage stage;

    private Scene backScene;

    public GoBackObserver(Stage stage, Scene backScene) {
        this.stage = stage;
        this.backScene = backScene;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType().equals(EventType.GO_BACK)) {
            stage.setScene(backScene);
            stage.show();
        }
    }
}
