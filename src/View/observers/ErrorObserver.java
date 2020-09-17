package View.observers;

import events.Event;
import events.EventType;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import util.Observer;

public class ErrorObserver implements Observer {
    private Stage ownerStage;

    private Scene errorScene;

    public ErrorObserver(Stage ownerStage, Scene errorScene) {
        this.ownerStage = ownerStage;
        this.errorScene = errorScene;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType() == EventType.ERROR) {
            Platform.runLater(() -> {
                Label errorLabel = null;
                Button okButton = null;
                for (Node node : errorScene.getRoot().getChildrenUnmodifiable()) {
                    if (node instanceof Label) {
                        errorLabel = (Label) node;
                    } else if (node instanceof Button) {
                        okButton = (Button) node;
                    }
                }
                errorLabel.setText(event.toString());

                Stage errorStage = new Stage();
                okButton.setOnAction((event1) -> errorStage.close());

                errorStage.initModality(Modality.WINDOW_MODAL);
                errorStage.setResizable(false);
                errorStage.setTitle("Error");
                errorStage.setScene(errorScene);
                errorStage.sizeToScene();
                errorStage.initOwner(ownerStage);
                errorStage.show();
            });
        }
    }
}
