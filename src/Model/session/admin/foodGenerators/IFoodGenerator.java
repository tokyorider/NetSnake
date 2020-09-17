package Model.session.admin.foodGenerators;

import me.ippolitov.fit.snakes.SnakesProto;

import java.util.List;

public interface IFoodGenerator {
    List<SnakesProto.GameState.Coord> generateFoods();
}
