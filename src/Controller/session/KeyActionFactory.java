package Controller.session;

import Model.GameModel;
import javafx.scene.input.KeyEvent;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Applier;

public class KeyActionFactory {
    public static Applier getActionByKey(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case A:
            case L:
                return () -> GameModel.getInstance().turnSnake(SnakesProto.Direction.LEFT);
            case W:
            case P:
                return () -> GameModel.getInstance().turnSnake(SnakesProto.Direction.UP);
            case S:
            case SEMICOLON:
                return () -> GameModel.getInstance().turnSnake(SnakesProto.Direction.DOWN);
            case D:
            case QUOTE:
                return () -> GameModel.getInstance().turnSnake(SnakesProto.Direction.RIGHT);
            default:
                return () -> {};
        }
    }
}
