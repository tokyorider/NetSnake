package Model.session.admin.foodGenerators;

import Model.session.admin.util.Field;
import Model.session.admin.util.FieldCell;
import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CasualFoodGenerator implements IFoodGenerator {
    private int foodRequired;

    private Field field;

    public CasualFoodGenerator(int foodRequired, Field field) {
        this.foodRequired = foodRequired;
        this.field = field;
    }

    @Override
    public List<SnakesProto.GameState.Coord> generateFoods() {
        ArrayList<SnakesProto.GameState.Coord> defaultCells = new ArrayList<>(), foodCoords = new ArrayList<>();
        for (int y = 0; y < field.getHeight(); ++y) {
            for (int x = 0; x < field.getWidth(); ++x) {
                if (field.getCell(x, y) == FieldCell.DEFAULT) {
                    defaultCells.add(SnakesProto.GameState.Coord.newBuilder().setX(x).setY(y).build());
                }
            }
        }

        Random random = new Random();
        for (int i = 0; i < foodRequired && !defaultCells.isEmpty(); ++i) {
            SnakesProto.GameState.Coord coord = defaultCells.get(random.nextInt(defaultCells.size()));
            foodCoords.add(coord);
            defaultCells.remove(coord);
        }
        return foodCoords;
    }
}
