package Model.session.admin.gameTurnMaker;

import Model.session.admin.foodGenerators.DeadSnakesFoodGenerator;
import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import Model.session.admin.util.Field;
import Model.session.admin.util.FieldCell;
import me.ippolitov.fit.snakes.SnakesProto;
import util.CollectionFinderByIf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameTurnMaker {
    private final static Map<SnakesProto.Direction, Integer> xOffsets = new HashMap<>();
    private final static Map<SnakesProto.Direction, Integer> yOffsets = new HashMap<>();
    static {
        xOffsets.put(SnakesProto.Direction.LEFT, -1);
        xOffsets.put(SnakesProto.Direction.RIGHT, 1);
        xOffsets.put(SnakesProto.Direction.UP, 0);
        xOffsets.put(SnakesProto.Direction.DOWN, 0);

        yOffsets.put(SnakesProto.Direction.LEFT, 0);
        yOffsets.put(SnakesProto.Direction.RIGHT, 0);
        yOffsets.put(SnakesProto.Direction.UP, -1);
        yOffsets.put(SnakesProto.Direction.DOWN, 1);
    }

    private ISnakeCoordWrapper wrapper;

    public GameTurnMaker(ISnakeCoordWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public SnakesProto.GameState makeTurn(SnakesProto.GameState gameState, Field field,
                                          Map<Integer, SnakesProto.Direction> snakesTurns)
    {
        int width = field.getWidth(), height = field.getHeight();
        ArrayList<SnakesProto.GameState.Snake> snakes = new ArrayList<>();
        ArrayList<SnakesProto.GameState.Coord> foods = new ArrayList<>(gameState.getFoodsList());
        ArrayList<SnakesProto.GamePlayer> players = new ArrayList<>(gameState.getPlayers().getPlayersList());

        gameState.getSnakesList().forEach((snake) -> {
            var player = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                    (p) -> p.getId() == snake.getPlayerId());

            var coords = wrapper.unwrap(snake.getPointsList());
            var direction = (player == null) ?
                    snake.getHeadDirection() : snakesTurns.get(snake.getPlayerId());
            int xOffset = xOffsets.get(direction), yOffset = yOffsets.get(direction);
            var head = SnakesProto.GameState.Coord.newBuilder().
                    setX(Math.floorMod(coords.get(0).getX() + xOffset, width)).
                    setY(Math.floorMod(coords.get(0).getY() + yOffset, height)).
                    build();
            coords.add(0, head);

            //Checking if snake ate food
            if(field.getCell(head.getX(), head.getY()) == FieldCell.FOOD) {
                if (player != null) {
                    players.remove(player);
                    players.add(SnakesProto.GamePlayer.newBuilder(player).
                                setScore(player.getScore() + 1).
                                buildPartial());
                }
                foods.remove(head);
            } else {
                coords.remove(coords.size() - 1);
            }

            snakes.add(SnakesProto.GameState.Snake.newBuilder().
                   addAllPoints(wrapper.wrap(coords)).
                   setHeadDirection(direction).
                   setPlayerId(snake.getPlayerId()).
                   setState(snake.getState()).
                   buildPartial());
        });

        var victimsAndKillers = findDeadSnakes(snakes);
        victimsAndKillers.keySet().forEach((snake) -> {
            foods.addAll(new DeadSnakesFoodGenerator(wrapper.unwrap(snake.getPointsList()),
                    gameState.getConfig().getDeadFoodProb()).generateFoods());
        });
        snakes.removeAll(victimsAndKillers.keySet());

        //+1 score to killer for each victim if killer is alive
        victimsAndKillers.values().forEach((killerId) -> {
            var snake = CollectionFinderByIf.findByIf(snakes, (s) -> s.getPlayerId() == killerId);
            if (snakes.contains(snake) && snake.getState() == SnakesProto.GameState.Snake.SnakeState.ALIVE) {
                var player = CollectionFinderByIf.findByIf(players, (p) -> p.getId() == killerId);
                players.set(players.indexOf(player),
                        SnakesProto.GamePlayer.newBuilder(player).setScore(player.getScore() + 1).buildPartial());
            }
        });

        return SnakesProto.GameState.newBuilder().
               setStateOrder(gameState.getStateOrder()).
               addAllSnakes(snakes).
               addAllFoods(foods).
               setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).buildPartial()).
               setConfig(gameState.getConfig()).
               buildPartial();
    }

    private Map<SnakesProto.GameState.Snake, Integer> findDeadSnakes(
            List<SnakesProto.GameState.Snake> snakes)
    {
        Map<SnakesProto.GameState.Snake, Integer> deadSnakes = new HashMap<>();
        //Finding dead snakes
        snakes.forEach((snake) -> {
            var points1 = wrapper.unwrap(snake.getPointsList());
            for (var s : snakes) {
                var unwrappedPoints = wrapper.unwrap(s.getPointsList());
                var points2 = (s.getPlayerId() == snake.getPlayerId()) ?
                        unwrappedPoints.subList(1, unwrappedPoints.size()) : unwrappedPoints;
                if (points2.contains(points1.get(0))) {
                    deadSnakes.put(snake, s.getPlayerId());
                    return;
                }
            }
        });

        return deadSnakes;
    }
}
