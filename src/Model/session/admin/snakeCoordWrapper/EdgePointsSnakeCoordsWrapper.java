package Model.session.admin.snakeCoordWrapper;

import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.List;

public class EdgePointsSnakeCoordsWrapper implements ISnakeCoordWrapper {
    private int width, height;

    public EdgePointsSnakeCoordsWrapper(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public List<SnakesProto.GameState.Coord> wrap(List<SnakesProto.GameState.Coord> coords) {
        ArrayList<SnakesProto.GameState.Coord> wrappedCoords = new ArrayList<>();
        wrappedCoords.add(coords.get(0));
        for (int i = 1, xOffset = 0, yOffset = 0; i < coords.size(); ++i) {
            int x = coords.get(i).getX(), y= coords.get(i).getY(),
                    prevX = coords.get(i - 1).getX(), prevY = coords.get(i - 1).getY();
            xOffset += (x > prevX && !(x == width - 1 && prevX == 0) || x == 0 && prevX == width - 1) ?
                        1 : ((x == prevX) ? 0 : -1);
            yOffset += (y > prevY && !(y == height - 1 && prevY == 0) || y == 0 && prevY == height - 1) ?
                        1 : ((y == prevY) ? 0 : -1);

            if (i == coords.size() - 1 ||
                prevX != coords.get(i + 1).getX() &&
                prevY != coords.get(i + 1).getY())
            {
                wrappedCoords.add(SnakesProto.GameState.Coord.newBuilder().setX(xOffset).setY(yOffset).build());
                xOffset = 0;
                yOffset = 0;
            }
        }

        return wrappedCoords;
    }

    @Override
    public List<SnakesProto.GameState.Coord> unwrap(List<SnakesProto.GameState.Coord> wrappedCoords) {
        ArrayList<SnakesProto.GameState.Coord> unwrappedCoords = new ArrayList<>();
        unwrappedCoords.add(wrappedCoords.get(0));
        for (int i = 1; i < wrappedCoords.size(); ++i) {
            int x = unwrappedCoords.get(unwrappedCoords.size() - 1).getX(),
                    y = unwrappedCoords.get(unwrappedCoords.size() - 1).getY();
            int xOffset = wrappedCoords.get(i).getX();
            for (int j = 1; j <= Math.abs(xOffset); ++j) {
                unwrappedCoords.add(SnakesProto.GameState.Coord.newBuilder()
                        .setX(Math.floorMod(x + j * (int) Math.signum(xOffset), width))
                        .setY(y)
                        .build());
            }

            int yOffset = wrappedCoords.get(i).getY();
            for (int j = 1; j <= Math.abs(yOffset); ++j) {
                unwrappedCoords.add(SnakesProto.GameState.Coord.newBuilder()
                        .setX(x)
                        .setY(Math.floorMod(y + j * (int) Math.signum(yOffset), height))
                        .build());
            }
        }

        return unwrappedCoords;
    }
}
