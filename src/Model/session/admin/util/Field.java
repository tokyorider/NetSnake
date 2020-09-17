package Model.session.admin.util;

import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import me.ippolitov.fit.snakes.SnakesProto;

public class Field {
    private int width, height;

    private FieldCell[] field;

    public Field(int width, int height) {
        this.width = width;
        this.height = height;
        field = new FieldCell[width * height];
        for (int i = 0; i < field.length; ++i) {
            field[i] = FieldCell.DEFAULT;
        }
    }

    public Field(SnakesProto.GameState gameState, ISnakeCoordWrapper wrapper) {
        this(gameState.getConfig().getWidth(), gameState.getConfig().getHeight());
        gameState.getFoodsList().forEach((coord) -> setCell(FieldCell.FOOD, coord.getX(), coord.getY()));
        gameState.getSnakesList().forEach((snake) -> {
            wrapper.unwrap(snake.getPointsList())
                    .forEach((coord) -> setCell(FieldCell.SNAKE, coord.getX(), coord.getY()));
        });
    }

    public FieldCell getCell(int x, int y) {
        return field[y * width + x];
    }

    public void setCell(FieldCell cell, int x, int y) {
        field[y * width + x] = cell;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
