package Model.session.admin.newSnakePlacementComputer;

import Model.session.admin.exceptions.NoPlaceForNewSnakeException;
import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import Model.session.admin.util.Field;
import Model.session.admin.util.FieldCell;
import me.ippolitov.fit.snakes.SnakesProto;
import util.CollectionFinderByIf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewDoubleCellSnakePlacementComputer implements INewSnakePlacementComputer {
    private final static int freeSquareSize = 5;

    @Override
    public SnakesProto.GameState.Snake computeNewSnakePlacement(Field field, ISnakeCoordWrapper wrapper)
            throws NoPlaceForNewSnakeException {
        for (int y = 0; y < field.getHeight(); ++y) {
            for (int x = 0; x < field.getWidth(); ++x) {
                if (isFree(x, y, field)) {
                    var placement = findFreeFromFoodCells(x, y, field);
                    if (placement != null) {
                        //Computing snake direction by head and tail coords
                        var direction = (Math.floorMod(
                                placement.get(0).getX() - placement.get(1).getX(), field.getWidth()) == 1 ?
                                SnakesProto.Direction.RIGHT : ((Math.floorMod(
                                placement.get(0).getX() - placement.get(1).getX(), field.getWidth()) ==
                                field.getWidth() - 1) ? SnakesProto.Direction.LEFT : ((Math.floorMod(
                                placement.get(0).getY() - placement.get(1).getY(), field.getHeight()) == 1) ?
                                SnakesProto.Direction.DOWN : SnakesProto.Direction.UP)));

                        return SnakesProto.GameState.Snake.newBuilder().
                                addAllPoints(wrapper.wrap(placement)).
                                setHeadDirection(direction).buildPartial();
                    }
                }
            }
        }

        throw new NoPlaceForNewSnakeException();
    }

    private boolean isFree(int initialX, int initialY, Field field) {
        int width = field.getWidth(), height = field.getHeight();
        for (int y = initialY; y != (initialY + freeSquareSize) % height; y = (y + 1) % height) {
            for (int x = initialX; x != (initialX + freeSquareSize) % width; x = (x + 1) % width) {
                if (field.getCell(x, y) == FieldCell.SNAKE) {
                    return false;
                }
            }
        }
        return true;
    }

    private List<SnakesProto.GameState.Coord> findFreeFromFoodCells(int initialX, int initialY, Field field) {
        int width = field.getWidth(), height = field.getHeight(),
                x = (initialX + freeSquareSize / 2) % width, y = (initialY + freeSquareSize / 2) % height;

        if (field.getCell(x, y) == FieldCell.DEFAULT) {
            SnakesProto.GameState.Coord leftNeighbour =
                    SnakesProto.GameState.Coord.newBuilder().setX(Math.floorMod(x - 1, width)).setY(y).build(),
                    rightNeighbour =
                            SnakesProto.GameState.Coord.newBuilder().setX((x + 1) % width).setY(y).build(),
                    upNeighbour =
                            SnakesProto.GameState.Coord.newBuilder().setX(x).setY(Math.floorMod(y - 1, height)).build(),
                    downNeighbour =
                            SnakesProto.GameState.Coord.newBuilder().setX(x).setY((y + 1) % height).build();
            //Initializing cell neighbours
            var neighbours = Arrays.asList(leftNeighbour, rightNeighbour, downNeighbour, upNeighbour);

            //Checking if neighbour cell is in square and isnt food
            Collections.shuffle(neighbours);
            SnakesProto.GameState.Coord neighbour = CollectionFinderByIf.findByIf(neighbours,
                    (n) -> field.getCell(n.getX(), n.getY()) == FieldCell.DEFAULT),
                    coord = SnakesProto.GameState.Coord.newBuilder().setX(x).setY(y).build();
            //If non-food neighbour was found
            if (neighbour != null) {
                return Arrays.asList(coord, neighbour);
            }
        }
        return null;
    }
}
