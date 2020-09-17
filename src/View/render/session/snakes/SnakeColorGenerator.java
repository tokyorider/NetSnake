package View.render.session.snakes;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class SnakeColorGenerator {
    private ArrayList<Color> availableSnakeColors = new ArrayList<>(Arrays.asList(
             Color.BLUEVIOLET, Color.LAWNGREEN, Color.YELLOW, Color.LAVENDER,
             Color.ORANGE, Color.HOTPINK, Color.WHITE, Color.BLUE));

    private static Random random = new Random();

    Color generateColor() {
        Color color = availableSnakeColors.get(random.nextInt(availableSnakeColors.size()));
        availableSnakeColors.remove(color);
        return color;
    }

    void addColor(Color color) {
        availableSnakeColors.add(color);
    }
}
