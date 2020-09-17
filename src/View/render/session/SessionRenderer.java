package View.render.session;

import View.render.session.foods.FoodDrawer;
import View.render.session.snakes.SnakeDrawer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.List;

public class SessionRenderer {
    private double cellWidth, cellHeight;

    private FoodDrawer foodDrawer;

    private SnakeDrawer snakeDrawer;

    private GraphicsContext gc;

    public SessionRenderer(GraphicsContext gc, double cellWidth, double cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.gc = gc;

        foodDrawer = new FoodDrawer(gc, cellWidth, cellHeight);
        snakeDrawer = new SnakeDrawer(gc, cellWidth, cellHeight);
    }

    public void renderSession(List<Point2D> foods, HashMap<Integer, List<Point2D>> snakePoints) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        foodDrawer.drawFoods(foods);
        snakeDrawer.drawSnakes(snakePoints);
    }

    public double getCellWidth() {
        return cellWidth;
    }

    public double getCellHeight() {
        return cellHeight;
    }
}
