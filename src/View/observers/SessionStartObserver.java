package View.observers;

import Model.GameModel;
import View.render.score.ScoreRenderer;
import View.render.session.SessionRenderer;
import events.Event;
import events.EventType;
import events.SessionStartEvent;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import util.Observable;
import util.Observer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SessionStartObserver implements Observer {
    private Stage stage;

    private Scene backScene;

    public SessionStartObserver(Stage stage, Scene backScene) {
        this.stage = stage;
        this.backScene = backScene;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType().equals(EventType.SESSION_START)) {
            //Creating scene
            FXMLLoader sessionLoader = new FXMLLoader(getClass().getResource("/View/scenes/Session.fxml"));
            try {
                Scene sessionScene = new Scene(sessionLoader.load());
                Observable sessionController = sessionLoader.getController();
                SessionStartEvent sessionStartEvent = (SessionStartEvent) event;
                int fieldWidth = sessionStartEvent.getConfig().getWidth(),
                        fieldHeight = sessionStartEvent.getConfig().getHeight();

                //Finding session canvas
                Canvas canvas = null;
                GridPane gridPane = null;
                for (Node node : sessionScene.getRoot().getChildrenUnmodifiable()) {
                    if (node instanceof Canvas) {
                        canvas = (Canvas) node;
                    } else if (node instanceof GridPane) {
                        gridPane = (GridPane) node;
                    }
                }
                canvas.setWidth(Math.min((double)fieldWidth / (double)fieldHeight, 1) * canvas.getWidth());
                canvas.setHeight(Math.min((double)fieldHeight / (double)fieldWidth, 1) * canvas.getHeight());
                drawBorders(canvas);

                //Computing renderSession parameters

                double cellWidth = canvas.getWidth() / fieldWidth,
                        cellHeight = canvas.getHeight() / fieldHeight;
                SessionRenderer sessionRenderer = new SessionRenderer(canvas.getGraphicsContext2D(), cellWidth, cellHeight);

                sessionController.addObserver(new GoBackObserver(stage, backScene));
                Platform.runLater(() -> {
                    stage.setScene(sessionScene);
                    stage.show();
                });

                SessionRenderObserver renderObserver = new SessionRenderObserver(new ScoreRenderer(gridPane),
                        sessionRenderer, sessionStartEvent.getCoordsWrapper());
                GameModel.getInstance().addObserver(renderObserver);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawBorders(Canvas canvas) {
        final double BORDER_DEPTH = 20;
        double minX = canvas.getBoundsInParent().getMinX(), minY = canvas.getBoundsInParent().getMinY(),
                maxX = canvas.getBoundsInParent().getMaxX(), maxY = canvas.getBoundsInParent().getMaxY();
        List<Rectangle> fieldBorders = Arrays.asList(
                //Top border
                new Rectangle(minX - BORDER_DEPTH, minY - BORDER_DEPTH,
                        canvas.getWidth() + 2 * BORDER_DEPTH, BORDER_DEPTH),
                //Left border
                new Rectangle(minX - BORDER_DEPTH, minY, BORDER_DEPTH, canvas.getHeight()),
                //Down border
                new Rectangle(minX - BORDER_DEPTH, maxY,canvas.getWidth() + 2 * BORDER_DEPTH, BORDER_DEPTH),
                //Right border
                new Rectangle(maxX, minY, BORDER_DEPTH, canvas.getHeight()));
        fieldBorders.forEach((line) -> {
            line.setFill(Color.CADETBLUE);
        });

        AnchorPane root = (AnchorPane) canvas.getScene().getRoot();
        root.getChildren().addAll(fieldBorders);
    }
}
