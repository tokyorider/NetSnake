package View.observers;

import Model.GameModel;
import events.Event;
import events.EventType;
import events.SessionsListUpdateEvent;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import util.Observer;

public class SessionsListUpdateObserver implements Observer {
    private static Font font = new Font("Book antiqua", 10);

    @Override
    public void handleEvent(Event event) {
        if (event.getType() == EventType.SESSIONS_LIST_UPDATE) {
            SessionsListUpdateEvent refreshEvent = (SessionsListUpdateEvent) event;
            GridPane gridPane = refreshEvent.getSessionsListPane();
            Platform.runLater(() -> {
                gridPane.getChildren().clear();
                refreshEvent.getSessionInfos().forEach((adminIp, gameConfig) -> {
                    Button config = new Button(adminIp.toString().
                            replaceAll("\n", " ")),
                            players = new Button(gameConfig.toString().
                                replaceAll("\n", " "));
                    config.setFont(font);
                    players.setFont(font);

                    config.addEventHandler(MouseEvent.MOUSE_CLICKED,
                            (mouseEvent) -> GameModel.getInstance().connectToSession(adminIp));
                    players.addEventHandler(MouseEvent.MOUSE_CLICKED,
                            (mouseEvent) -> GameModel.getInstance().connectToSession(adminIp));

                    gridPane.addRow(gridPane.getRowCount(), config, players);
                });
            });
        }
    }
}
