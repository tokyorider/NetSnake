package Model.session.admin.snakeCoordWrapper;

import me.ippolitov.fit.snakes.SnakesProto;

import java.util.List;

public interface ISnakeCoordWrapper {
    List<SnakesProto.GameState.Coord> wrap(List<SnakesProto.GameState.Coord> coords);
    List<SnakesProto.GameState.Coord> unwrap(List<SnakesProto.GameState.Coord> wrappedCoords);
}
