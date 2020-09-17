package Model.session;

import me.ippolitov.fit.snakes.SnakesProto;
import util.Observable;
import util.Observer;

public interface ISessionModel extends Observable, Observer {
    void start();

    void registerSteer(SnakesProto.Direction direction);

    void exit();
}
