package View.render.session.foods;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class FoodDrawer {
    private GraphicsContext gc;

    private double cellWidth, cellHeight;

    private final static Color foodsColor = Color.RED;

    public FoodDrawer(GraphicsContext gc, double cellWidth, double cellHeight) {
        this.gc = gc;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }
    public void drawFoods(List<Point2D> foods) {
        gc.setFill(foodsColor);
        foods.forEach((point) -> gc.fillRect(point.getX() + cellWidth / 4, point.getY() + cellHeight / 4,
                                                cellWidth / 2, cellHeight / 2));
    }
}
