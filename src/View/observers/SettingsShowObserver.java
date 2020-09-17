package View.observers;

import events.Event;
import events.EventType;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.Observer;

public class SettingsShowObserver implements Observer {
    private Stage stage;

    private Scene settingsScene;

    public SettingsShowObserver(Stage stage, Scene settingsScene) {
        this.stage = stage;
        this.settingsScene = settingsScene;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType().equals(EventType.SETTINGS_SHOW)) {
            stage.setScene(settingsScene);
            stage.show();
        }
    }
}
