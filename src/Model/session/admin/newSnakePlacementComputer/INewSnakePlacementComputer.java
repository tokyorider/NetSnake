package Model.session.admin.newSnakePlacementComputer;

import Model.session.admin.exceptions.NoPlaceForNewSnakeException;
import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import Model.session.admin.util.Field;
import me.ippolitov.fit.snakes.SnakesProto;

public interface INewSnakePlacementComputer {
    SnakesProto.GameState.Snake computeNewSnakePlacement(Field field, ISnakeCoordWrapper wrapper)
            throws NoPlaceForNewSnakeException;
}
