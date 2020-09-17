package Model.session.admin;

import Model.network.AdminNetworkModel;
import Model.session.ISessionModel;
import Model.session.admin.exceptions.NoPlaceForNewSnakeException;
import Model.session.admin.foodGenerators.CasualFoodGenerator;
import Model.session.admin.gameTurnMaker.GameTurnMaker;
import Model.session.admin.newSnakePlacementComputer.INewSnakePlacementComputer;
import Model.session.admin.newSnakePlacementComputer.NewDoubleCellSnakePlacementComputer;
import Model.session.admin.playerIdGenerators.IPlayerIdGenerator;
import Model.session.admin.snakeCoordWrapper.EdgePointsSnakeCoordsWrapper;
import Model.session.admin.snakeCoordWrapper.ISnakeCoordWrapper;
import Model.session.admin.stateOrderIdGenerators.IStateOrderIdGenerator;
import Model.session.admin.util.Field;
import events.*;
import me.ippolitov.fit.snakes.SnakesProto;
import util.CollectionFinderByIf;
import util.Observer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AdminSessionModel implements ISessionModel, Observer {
    private static final int ANNOUNCEMENT_DELAY_MS = 0;
    private static final int ANNOUNCEMENT_PERIOD_MS = 1000;

    private final static Map<SnakesProto.Direction, SnakesProto.Direction> opposites = new HashMap<>();
    static {
        opposites.put(SnakesProto.Direction.LEFT, SnakesProto.Direction.RIGHT);
        opposites.put(SnakesProto.Direction.RIGHT, SnakesProto.Direction.LEFT);
        opposites.put(SnakesProto.Direction.UP, SnakesProto.Direction.DOWN);
        opposites.put(SnakesProto.Direction.DOWN, SnakesProto.Direction.UP);
    }

    private int selfId;

    private Integer deputyId = null;

    private ConcurrentLinkedDeque<Observer> observers = new ConcurrentLinkedDeque<>();

    private SnakesProto.GameConfig config;

    private SnakesProto.GameState gameState = SnakesProto.GameState.newBuilder().buildPartial();

    private ConcurrentHashMap<Integer, SnakesProto.Direction> snakesTurns = new ConcurrentHashMap<>();

    private INewSnakePlacementComputer snakePlacer = new NewDoubleCellSnakePlacementComputer();

    private ISnakeCoordWrapper wrapper;

    private IPlayerIdGenerator playerIdGenerator;

    private IStateOrderIdGenerator stateIdGenerator;

    private GameTurnMaker gameTurnMaker;

    private AdminNetworkModel networkModel;

    private Timer timer = new Timer(true);

    public AdminSessionModel(SnakesProto.GameConfig config, IPlayerIdGenerator playerIdGenerator,
                             IStateOrderIdGenerator stateIdGenerator, AdminNetworkModel networkModel) {
        this.config = config;
        this.wrapper = new EdgePointsSnakeCoordsWrapper(config.getWidth(), config.getHeight());
        this.playerIdGenerator = playerIdGenerator;
        this.stateIdGenerator = stateIdGenerator;
        this.networkModel = networkModel;
        gameTurnMaker = new GameTurnMaker(wrapper);
    }

    @Override
    public void start() {
        gameState = SnakesProto.GameState.newBuilder().
                setConfig(config).
                buildPartial();

        List<SnakesProto.GameState.Coord> foodCoords =
                new CasualFoodGenerator(config.getFoodStatic() + Math.round(config.getFoodPerPlayer()),
                        initField()).generateFoods();
        gameState = SnakesProto.GameState.newBuilder(gameState).
                addAllFoods(foodCoords).
                buildPartial();

        selfId = playerIdGenerator.generateId();
        var admin = SnakesProto.GamePlayer.newBuilder().
                setId(selfId).
                setScore(0).
                //TODO
                setName("a").
                setRole(SnakesProto.NodeRole.MASTER).
                setType(SnakesProto.PlayerType.HUMAN).
                setIpAddress("").
                setPort(networkModel.getPort()).
                build();
        try {
            addPlayer(admin);
        } catch (NoPlaceForNewSnakeException e) {
            notifyObservers(new ErrorEvent(e.getMessage()));
            exit();
            return;
        }

        gameState = SnakesProto.GameState.newBuilder(gameState).
                setStateOrder(stateIdGenerator.generateId()).
                buildPartial();

        initialize();
        notifyObservers(new SessionStartEvent(config, wrapper));
        notifyObservers(new SessionRenderEvent(gameState));
    }

    public void continueSession(int selfId, InetAddress oldAdminIp,
                                SnakesProto.GameState gameState)
    {
        this.gameState = gameState;
        this.selfId = selfId;
        gameState.getSnakesList().forEach((snake -> {
            if (snake.getState() == SnakesProto.GameState.Snake.SnakeState.ALIVE) {
                snakesTurns.put(snake.getPlayerId(), snake.getHeadDirection());
            }
        }));

        //TODO fix nado tochno
        var oldAdmin = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                (player) -> player.getRole() == SnakesProto.NodeRole.MASTER);
        var newAdmin = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                (player) -> player.getId() == selfId);
        var playersList = new ArrayList<>(gameState.getPlayers().getPlayersList());
        playersList.remove(oldAdmin);
        playersList.remove(newAdmin);
        oldAdmin = SnakesProto.GamePlayer.newBuilder(oldAdmin)
                    .setIpAddress(oldAdminIp.getHostAddress())
                    .build();
        newAdmin = SnakesProto.GamePlayer.newBuilder(newAdmin)
                    .setIpAddress("")
                    .build();
        playersList.add(oldAdmin);
        playersList.add(newAdmin);
        var players = SnakesProto.GamePlayers.newBuilder()
                        .addAllPlayers(playersList)
                        .build();
        this.gameState = SnakesProto.GameState.newBuilder(gameState)
                    .setPlayers(players)
                    .build();
        changePlayerRole(oldAdmin, SnakesProto.NodeRole.VIEWER);
        changePlayerRole(newAdmin, SnakesProto.NodeRole.MASTER);
        chooseNewDeputy();
        gameState.getPlayers().getPlayersList().forEach((player) -> {
            if (player.getId() != selfId) {
                networkModel.addNode(player.getId(), player.getIpAddress());
            }
        });

        initialize();
    }

    private void initialize() {
        networkModel.addObserver(this);
        networkModel.setSelfId(selfId);
        networkModel.start();

        scheduleTasks();
        registerMessageHandlers();
    }

    private void addPlayer(SnakesProto.GamePlayer player) throws NoPlaceForNewSnakeException {
        SnakesProto.GameState.Snake snake;
        snake = SnakesProto.GameState.Snake.
                newBuilder(snakePlacer.computeNewSnakePlacement(initField(), wrapper)).
                setPlayerId(player.getId()).
                setState(SnakesProto.GameState.Snake.SnakeState.ALIVE).
                buildPartial();


        var playersList = new ArrayList<>(gameState.getPlayers().getPlayersList());
        playersList.add(player);
        var players = SnakesProto.GamePlayers.newBuilder().
                addAllPlayers(playersList).
                build();

        snakesTurns.put(player.getId(), snake.getHeadDirection());
        gameState = SnakesProto.GameState.newBuilder(gameState).
                setPlayers(players).
                addSnakes(snake).
                buildPartial();
    }

    private void removePlayer(int id) {
        var player = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                (p) -> p.getId() == id);

        List<SnakesProto.GamePlayer> playersList =
                new ArrayList<>(gameState.getPlayers().getPlayersList());
        playersList.remove(player);
        var players = SnakesProto.GamePlayers.newBuilder().
                        addAllPlayers(playersList).
                        build();

        List<SnakesProto.GameState.Snake> snakes = new ArrayList<>(gameState.getSnakesList());
        var snake = CollectionFinderByIf.findByIf(gameState.getSnakesList(),
                (s) -> s.getPlayerId() == player.getId());
        if (snake != null) {
            snakes.add(SnakesProto.GameState.Snake.newBuilder(snake).
                    setState(SnakesProto.GameState.Snake.SnakeState.ZOMBIE).
                    build());
            snakes.remove(snake);
        }

        gameState = SnakesProto.GameState.newBuilder().
                    setStateOrder(gameState.getStateOrder()).
                    setConfig(gameState.getConfig()).
                    addAllSnakes(snakes).
                    addAllFoods(gameState.getFoodsList()).
                    setPlayers(players).
                    build();
    }

    private void registerMessageHandlers() {
        //Player joins handler
        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.JOIN, (source, msg) -> {
            var joinMsg = msg.getJoin();
            var player = SnakesProto.GamePlayer.newBuilder().
                    setId(playerIdGenerator.generateId()).
                    setScore(0).
                    setName(joinMsg.getName()).
                    setRole(SnakesProto.NodeRole.NORMAL).
                    setType(joinMsg.getPlayerType()).
                    setIpAddress(source.getHostString()).
                    setPort(source.getPort()).
                    build();
            try {
                addPlayer(player);

                networkModel.addNode(player.getId(), player.getIpAddress());

                if (gameState.getPlayers().getPlayersCount() == 2) {
                    chooseNewDeputy();
                }

                networkModel.sendAck(msg.getMsgSeq(), player.getId());
            } catch (NoPlaceForNewSnakeException e) {
                networkModel.sendErr(source.getAddress(), e.getMessage());
                networkModel.sendAck(source.getAddress(), msg.getMsgSeq(), selfId, -1);
            }
        });

        //Players controls handler
        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.STEER, (source, msg) -> {
            registerSteer(msg.getSenderId(), msg.getSteer().getDirection());

            networkModel.sendAck(msg.getMsgSeq(),  msg.getSenderId());
        });

        //Send role handler
        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE, (source, msg) -> {
            if (msg.getRoleChange().getSenderRole() == SnakesProto.NodeRole.VIEWER) {
                handleEvent(new NodeDisconnectedEvent(msg.getSenderId()));
            }

            networkModel.sendAck(msg.getMsgSeq(), msg.getSenderId());
        });

        networkModel.registerHandler(SnakesProto.GameMessage.TypeCase.PING, (source, msg) -> {
            networkModel.sendAck(msg.getMsgSeq(),  msg.getSenderId());
        });
    }

    private void scheduleTasks() {
        //Announcements
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                networkModel.sendAnnouncement(gameState);
            }
        }, ANNOUNCEMENT_DELAY_MS, ANNOUNCEMENT_PERIOD_MS);

        //Game state updates
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                makeTurn();
                //Notifying players about game state changing
                gameState.getPlayers().getPlayersList().forEach((player) -> {
                    if (player.getType() == SnakesProto.PlayerType.HUMAN &&
                            player.getId() != selfId) {
                        networkModel.sendState(gameState, player.getId());
                    }
                });
            }
        }, config.getStateDelayMs(), config.getStateDelayMs());
    }

    @Override
    public void registerSteer(SnakesProto.Direction direction) {
        registerSteer(selfId, direction);
    }

    private void registerSteer(int playerId, SnakesProto.Direction direction) {
        SnakesProto.GameState.Snake snake = CollectionFinderByIf.findByIf(gameState.getSnakesList(),
                (s) -> s.getPlayerId() == playerId);

        if (snake != null && snake.getHeadDirection() != opposites.get(direction)) {
            snakesTurns.put(playerId, direction);
        }
    }

    private void makeTurn() {
        var gs = gameTurnMaker.makeTurn(gameState, initField(), snakesTurns);

        gameState = SnakesProto.GameState.newBuilder(gs).
                setStateOrder(stateIdGenerator.generateId()).
                addAllFoods(generateFoods()).
                buildPartial();
        removeDeadPlayers(gs);

        notifyObservers(new SessionRenderEvent(gameState));
    }

    //Generating food after turn
    private List<SnakesProto.GameState.Coord> generateFoods() {
        int foodRequired = config.getFoodStatic() +
                (int) (gameState.getSnakesCount() * config.getFoodPerPlayer())
                - gameState.getFoodsCount();
        return new CasualFoodGenerator(foodRequired, initField()).generateFoods();
    }

    private void removeDeadPlayers(SnakesProto.GameState state) {
        var aliveSnakeIds = new ArrayList<>();
        state.getSnakesList().forEach((snake) -> aliveSnakeIds.add(snake.getPlayerId()));

        state.getPlayers().getPlayersList().forEach((player) -> {
            if (!aliveSnakeIds.contains(player.getId())) {
                changePlayerRole(player, SnakesProto.NodeRole.VIEWER);
            }
        });

        //If deputy's dead, then choose new
        if (deputyId != null && !aliveSnakeIds.contains(deputyId)) {
            chooseNewDeputy();
        }

        //If we're dead, then delegate admin model
        if (!aliveSnakeIds.contains(selfId)) {
            becomeViewer();
        }
    }

    private void chooseNewDeputy() {
        var deputy = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                        (player) -> player.getId() != selfId &&
                                player.getRole() != SnakesProto.NodeRole.VIEWER);
        if (deputy == null) {
            deputyId = null;
        } else {
            networkModel.sendRoleChange(deputy.getId(), SnakesProto.NodeRole.MASTER,
                    SnakesProto.NodeRole.DEPUTY);

            changePlayerRole(deputy, SnakesProto.NodeRole.DEPUTY);
            deputyId = deputy.getId();
        }
    }

    private void changePlayerRole(SnakesProto.GamePlayer player, SnakesProto.NodeRole newRole) {
        List<SnakesProto.GamePlayer> playersList =
                new ArrayList<>(gameState.getPlayers().getPlayersList());
        playersList.remove(player);
        playersList.add(SnakesProto.GamePlayer.newBuilder(player)
                        .setRole(newRole)
                        .build());
        var players = SnakesProto.GamePlayers.newBuilder().
                addAllPlayers(playersList).
                build();

        gameState = SnakesProto.GameState.newBuilder(gameState)
                    .setPlayers(players)
                    .build();
    }

    private Field initField() {
        return new Field(gameState, wrapper);
    }

    private void becomeViewer() {
        timer.cancel();
        networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.JOIN);
        networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.STEER);
        networkModel.removeHandler(SnakesProto.GameMessage.TypeCase.ROLE_CHANGE);

        if (deputyId != null) {
            var deputy = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                    (player) -> player.getId() == deputyId);

            networkModel.sendRoleChange(deputyId, SnakesProto.NodeRole.VIEWER,
                    SnakesProto.NodeRole.MASTER);
            try {
                notifyObservers(new BecomeViewerEvent(selfId, deputyId,
                        InetAddress.getByName(deputy.getIpAddress()), gameState));
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            exit();
        }
    }

    public void exit() {
        timer.cancel();
        networkModel.exit();
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
            int disconnected = ((NodeDisconnectedEvent) event).getNodeId();
            var player = CollectionFinderByIf.findByIf(gameState.getPlayers().getPlayersList(),
                            (p) -> p.getId() == disconnected);
            if (player != null) {
                removePlayer(player.getId());
                if (deputyId != null && player.getId() == deputyId) {
                    chooseNewDeputy();
                }
            }
        }
    }
}
