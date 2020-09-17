package Model.session.admin.foodGenerators;

import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeadSnakesFoodGenerator implements IFoodGenerator {
    private List<SnakesProto.GameState.Coord> snakeCoords;

    private double probability;

    public DeadSnakesFoodGenerator(List<SnakesProto.GameState.Coord> snakeCoords, double probability) {
        this.snakeCoords = snakeCoords;
        this.probability = probability;
    }

    @Override
    public List<SnakesProto.GameState.Coord> generateFoods() {
        Random random = new Random();
        ArrayList<SnakesProto.GameState.Coord> foodCoords = new ArrayList<>();

        snakeCoords.forEach((coord) -> {
            if (((double) random.nextInt(100)) / 100 < probability) {
                foodCoords.add(coord);
            }
        });

        return foodCoords;
    }
}
