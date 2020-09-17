package Model;

import Model.network.AdminNetworkModel;
import Model.network.CommonUserNetworkModel;
import Model.network.NetworkModel;
import Model.network.messageSequenceGenerator.RandomSequenceGenerator;
import Model.session.CommonUserSessionModel;
import Model.session.ISessionModel;
import Model.session.admin.AdminSessionModel;
import Model.session.admin.playerIdGenerators.IncrementPlayerIdGenerator;
import Model.session.admin.stateOrderIdGenerators.IncrementStateOrderIdGenerator;
import events.*;
import me.ippolitov.fit.snakes.SnakesProto;
import util.Observable;
import util.Observer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameModel implements Observable, Observer {
    private class SessionInfo {
        private SnakesProto.GameConfig config;

        private SnakesProto.GamePlayers players;

        private Date lastActivity;

        private SessionInfo(SnakesProto.GameConfig config,
                            SnakesProto.GamePlayers players, Date lastActivity)
        {
            this.config = config;
            this.players = players;
            this.lastActivity = lastActivity;
        }
    }

    private final static int SESSION_TIMEOUT_MS = 1200;

    private static GameModel instance = new GameModel();

    private Collection<Observer> observers = new ConcurrentLinkedDeque<>();

    private NetworkModel networkModel;

    private Map<InetAddress, SessionInfo> lastSessionsInfos = new ConcurrentHashMap<>();

    private ISessionModel currentSession = null;

    private Timer timer = new Timer(true);

    public static GameModel getInstance() {
        return instance;
    }

    private GameModel() {
        try {
            MulticastSocket socket = new MulticastSocket(9193);
            socket.joinGroup(InetAddress.getByName("239.192.0.4"));

            networkModel = new NetworkModel(socket, new RandomSequenceGenerator());
            networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT, (source, msg) -> {
                        var announcementMsg = msg.getAnnouncement();
                        lastSessionsInfos.put(source.getAddress(), new SessionInfo(
                                announcementMsg.getConfig(), announcementMsg.getPlayers(),
                                new Date()));
                    });
            networkModel.start();

            //Deleting unactual sessions
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    lastSessionsInfos.forEach((gameConfig, sessionInfo) -> {
                        if (new Date().getTime() - sessionInfo.lastActivity.getTime()
                                > SESSION_TIMEOUT_MS)
                        {
                            lastSessionsInfos.remove(gameConfig);
                        }
                    });
                }
            }, 1000, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<InetAddress, SnakesProto.GameConfig> getLastSessionsInfos() {
        Map<InetAddress, SnakesProto.GameConfig> infos = new ConcurrentHashMap<>();
        lastSessionsInfos.forEach((adminIp, sessionInfo) -> {
            infos.put(adminIp, sessionInfo.config);
        });
        return infos;
    }

    public void createSession(SnakesProto.GameConfig config) {
        networkModel = new AdminNetworkModel(config.getPingDelayMs(), config.getNodeTimeoutMs(),
                                                networkModel);

        currentSession = new AdminSessionModel(config, new IncrementPlayerIdGenerator(0),
                new IncrementStateOrderIdGenerator(0), (AdminNetworkModel) networkModel);
        currentSession.addObserver(this);
        currentSession.start();
    }

    public void connectToSession(InetAddress adminIp) {
        var info = lastSessionsInfos.get(adminIp);
        if (info != null) {
            networkModel.sendJoin("b", adminIp);

            networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.ERROR, (source, msg) -> {
                networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ACK);
                networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ERROR);
                networkModel.sendAck(source.getAddress(), msg.getMsgSeq(), msg.getReceiverId(), msg.getSenderId());
                notifyObservers(new ErrorEvent(msg.getError().getErrorMessage()));
            });

            networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.ACK, (source, msg) -> {
                networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ACK);

                var config = info.config;
                if (msg.getReceiverId() != -1) {
                    int selfId = msg.getReceiverId(), adminId = msg.getSenderId();
                    networkModel = new CommonUserNetworkModel(config.getPingDelayMs(),
                            config.getNodeTimeoutMs(), selfId, adminId, adminIp, networkModel);

                    currentSession = new CommonUserSessionModel(selfId, adminIp, config,
                            (CommonUserNetworkModel) networkModel);
                    currentSession.addObserver(this);
                    currentSession.start();
                }
            });
        }
    }

    public void turnSnake(SnakesProto.Direction direction) {
        if (currentSession != null) {
            currentSession.registerSteer(direction);
        }
    }

    public void exitSession() {
        if (currentSession != null) {
            currentSession.exit();
        }

        currentSession = null;
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
        if (event.getType() == EventType.BECOME_ADMIN) {
            var gameState = ((BecomeAdminEvent) event).getLastGameState();
            if (gameState != null) {
                int maxPlayerId = 0;
                for (var player : gameState.getPlayers().getPlayersList()) {
                    if (player.getId() > maxPlayerId) {
                        maxPlayerId = player.getId();
                    }
                }

                networkModel = new AdminNetworkModel(gameState.getConfig().getPingDelayMs(),
                        gameState.getConfig().getNodeTimeoutMs(), networkModel);
                networkModel.start();

                currentSession.removeObserver(this);
                currentSession = new AdminSessionModel(gameState.getConfig(),
                        new IncrementPlayerIdGenerator(maxPlayerId + 1),
                        new IncrementStateOrderIdGenerator(gameState.getStateOrder() + 1),
                        (AdminNetworkModel) networkModel);
                currentSession.addObserver(this);

                BecomeAdminEvent adminEvent = (BecomeAdminEvent) event;
                ((AdminSessionModel) currentSession).continueSession(adminEvent.getSelfId(),
                        adminEvent.getOldAdminIp(), adminEvent.getLastGameState());
            }
        } else if (event.getType() == EventType.BECOME_VIEWER) {
            BecomeViewerEvent viewerEvent = ((BecomeViewerEvent) event);
            var gameState = viewerEvent.getGameState();
            var config = gameState.getConfig();
            networkModel = new CommonUserNetworkModel(config.getPingDelayMs(), config.getNodeTimeoutMs(),
                        viewerEvent.getSelfId(), viewerEvent.getAdminId(), viewerEvent.getAdminIp(), networkModel);

            currentSession = new CommonUserSessionModel(viewerEvent.getSelfId(), viewerEvent.getAdminIp(), config,
                    (CommonUserNetworkModel) networkModel);
            ((CommonUserSessionModel) currentSession).continueAsViewer(gameState);
            currentSession.addObserver(this);
        } else {
            notifyObservers(event);
        }
    }
}
