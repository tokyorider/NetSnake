package View.render.session.snakes;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SnakeDrawer {
    private ConcurrentHashMap<Integer, Color> snakeColors = new ConcurrentHashMap<>();

    private GraphicsContext gc;

    private double cellWidth, cellHeight;

    private SnakeColorGenerator colorGenerator = new SnakeColorGenerator();

    public SnakeDrawer(GraphicsContext gc, double cellWidth, double cellHeight) {
        this.gc = gc;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    public void drawSnakes(Map<Integer, List<Point2D>> pointsToDraw) {
        //Reuse colors
        snakeColors.forEach((id, color) -> {
            if (!pointsToDraw.containsKey(id)) {
                colorGenerator.addColor(color);
                snakeColors.remove(id);
            }
        });

        pointsToDraw.forEach((id, points) -> {
            if (!snakeColors.containsKey(id)) {
                snakeColors.put(id, colorGenerator.generateColor());
            }

            gc.setFill(snakeColors.get(id));
            points.forEach((point) -> {
                gc.fillRect(point.getX(), point.getY(), cellWidth, cellHeight);
            });

            gc.setFill(Color.BLACK);
            gc.fillRect(points.get(0).getX() + cellWidth / 4, points.get(0).getY() + cellHeight / 4,
                            cellWidth / 2, cellHeight / 2);
        });

    }

}
