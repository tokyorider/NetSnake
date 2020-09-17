package Model.session;

import Model.network.CommonUserNetworkModel;
import Model.session.admin.snakeCoordWrapper.EdgePointsSnakeCoordsWrapper;
import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import events.*;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observer;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CommonUserSessionModel implements ISessionModel {
    private int selfId;

    private InetAddress adminIp;

    private SnakesProto.GameState lastGameState = null;

    private SnakesProto.NodeRole role = SnakesProto.NodeRole.NORMAL;

    private SnakesProto.GameConfig config;

    private ISnakeCoordWrapper wrapper;

    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    private CommonUserNetworkModel networkModel;

    public CommonUserSessionModel(int selfId, InetAddress adminIp, SnakesProto.GameConfig config,
                                  CommonUserNetworkModel networkModel)
    {
        this.selfId = selfId;
        this.adminIp = adminIp;
        this.config = config;
        wrapper = new EdgePointsSnakeCoordsWrapper(config.getWidth(), config.getHeight());
        this.networkModel = networkModel;
    }

    @Override
    public void start() {
        registerHandlers();
        networkModel.addObserver(this);
        networkModel.start();
        notifyObservers(new SessionStartEvent(config, wrapper));
    }

    public void continueAsViewer(SnakesProto.GameState gameState) {
        lastGameState = gameState;

        registerViewerHandlers();
        notifyObservers(new SessionRenderEvent(gameState));
        networkModel.startOnlyView();
    }

    private void registerHandlers() {
        registerViewerHandlers();

        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE, (source, msg) -> {
            var senderRole = msg.getRoleChange().getSenderRole();
            var receiverRole = msg.getRoleChange().getReceiverRole();
            if (senderRole == SnakesProto.NodeRole.MASTER) {
                adminIp = source.getAddress();
                networkModel.setAdminId(msg.getSenderId());
                networkModel.setAdminIp(adminIp);
            }
            role = receiverRole;
            if (receiverRole == SnakesProto.NodeRole.MASTER) {
                notifyObservers(new BecomeAdminEvent(selfId, adminIp, lastGameState));
            }

            networkModel.sendAck(msg.getMsgSeq());
        });
    }

    private void registerViewerHandlers() {
        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.STATE, (source, msg) -> {
            var gameState = msg.getState().getState();
            //Checking if game state is actual
            if (lastGameState == null || gameState.getStateOrder() > lastGameState.getStateOrder()) {
                lastGameState = gameState;
                notifyObservers(new SessionRenderEvent(gameState));
            }

            networkModel.sendAck(msg.getMsgSeq());
        });

        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.PING, (source, msg) -> {
            networkModel.sendAck(msg.getMsgSeq());
        });
    }

    @Override
    public void exit() {
        networkModel.sendRoleChange(SnakesProto.NodeRole.VIEWER,
                SnakesProto.NodeRole.MASTER);

        networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.STATE);
        networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE);
        networkModel.exit();
    }

    @Override
    public void registerSteer(SnakesProto.Direction direction) {
        networkModel.sendSteer(direction);
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Event event) {
        observers.forEach(observer -> observer.handleEvent(event));
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getType() == EventType.NODE_DISCONNECTED) {
            if (role == SnakesProto.NodeRole.DEPUTY) {
                networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.STATE);
                networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE);
                networkModel.exit();

                notifyObservers(new BecomeAdminEvent(selfId, adminIp, lastGameState));
            }
        }
    }
}
