package View.observers;

import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import View.render.score.ScoreRenderer;
import View.render.session.SessionRenderer;
import events.Event;
import events.EventType;
import events.SessionRenderEvent;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SessionRenderObserver implements Observer {
    private ScoreRenderer scoreRenderer;

    private SessionRenderer sessionRenderer;

    private ISnakeCoordWrapper wrapper;

    public SessionRenderObserver(ScoreRenderer scoreRenderer, SessionRenderer sessionRenderer,
                                 ISnakeCoordWrapper wrapper)
    {
        this.scoreRenderer = scoreRenderer;
        this.sessionRenderer = sessionRenderer;
        this.wrapper = wrapper;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType().equals(EventType.SESSION_RENDER)) {
            SessionRenderEvent renderEvent = (SessionRenderEvent) event;
            SnakesProto.GameState gameState = renderEvent.getState();
            double cellWidth = sessionRenderer.getCellWidth(), cellHeight = sessionRenderer.getCellHeight();

            //Converting snakes coords to pixels
            HashMap<Integer, List<Point2D>> snakes = new HashMap<>();
            gameState.getSnakesList().forEach((snake) -> {
                //Counting points2d from snake coords to draw
                List<Point2D> points = new ArrayList<>();
                wrapper.unwrap(snake.getPointsList()).
                        forEach((point) -> points.add(new Point2D(point.getX() * cellWidth,
                                point.getY() * cellHeight)));
                snakes.put(snake.getPlayerId(), points);
            });

            //Converting foods coords to pixels
            ArrayList<Point2D> foods = new ArrayList<>();
            gameState.getFoodsList().forEach((coord) -> {
                foods.add(new Point2D(coord.getX() * cellWidth, coord.getY() * cellHeight));
            });

            Platform.runLater(() -> {
                sessionRenderer.renderSession(foods, snakes);
                scoreRenderer.renderScores(gameState.getPlayers().getPlayersList());
            });
        }
    }
}
