package ru.nsu.ccfit.khudyakov.network_lab_4.model;

import ru.nsu.ccfit.khudyakov.network_lab_4.config.ConfigParser;
import ru.nsu.ccfit.khudyakov.network_lab_4.message_handlers.MsgBuilder;
import ru.nsu.ccfit.khudyakov.network_lab_4.message_handlers.MsgParser;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.*;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer.ModelObservable;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer.ModelObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.PlayerInfo;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.ScoreObservable;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.ScoreObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.Direction;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameMessage;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameState;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameState.Snake.SnakeState;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.NodeRole;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class GameModel extends Thread implements ModelObservable, ScoreObservable {
    private final static Logger LOGGER = Logger.getLogger(GameModel.class.getName());

    private final int width;
    private final int height;
    private final int stateDelayMs;
    private final int foodStatic;
    private final float foodPerPlayer;
    private final float deadFoodProb;

    private Cell[][] field;

    private Map<Snake, Direction> snakes = new HashMap<>();
    private ArrayList<Coord> snacks = new ArrayList<>();

    private Server server;
    private NodeRole nodeRole;

    private final Random random = new Random();
    private ArrayList<ModelObserver> fieldObservers = new ArrayList<>();
    private ArrayList<ScoreObserver> scoreObservers = new ArrayList<>();


    public GameModel(IpPort ipPort, GameMessage.AnnouncementMsg gameConfig) {
        this.width = gameConfig.getConfig().getWidth();
        this.height = gameConfig.getConfig().getHeight();
        this.stateDelayMs = gameConfig.getConfig().getStateDelayMs();
        this.foodStatic = gameConfig.getConfig().getFoodStatic();
        this.foodPerPlayer = gameConfig.getConfig().getFoodPerPlayer();
        this.deadFoodProb = gameConfig.getConfig().getDeadFoodProb();
        this.nodeRole = NodeRole.NORMAL;

        List<Player> players = MsgParser.parsePlayers(gameConfig.getPlayers().getPlayersList());

        try {
            server = new Server(gameConfig.getConfig().getPingDelayMs(), gameConfig.getConfig().getNodeTimeoutMs());
            server.setPlayersList(players);
            int maxId = 0;
            for (Player player :
                    server.getPlayersList()) {
                if (player.getId() > maxId) {
                    maxId = player.getId();
                }
            }
            server.setLastPlayerId(maxId + 1);
            server.getDataSender().sendJoinMsg(ipPort.getPort(), ipPort.getIp());

        } catch (IOException e) {
            e.printStackTrace();
        }

        field = new Cell[width][height];

        for (Cell[] row : field) {
            Arrays.fill(row, Cell.EMPTY);
        }
    }

    public GameModel() {
        this.width = ConfigParser.getWidth();
        this.height = ConfigParser.getHeight();
        this.stateDelayMs = ConfigParser.getStateDelayMs();
        this.foodStatic = ConfigParser.getFoodStatic();
        this.foodPerPlayer = ConfigParser.getFoodPerPlayer();
        this.deadFoodProb = ConfigParser.getDeadFoodProb();
        this.nodeRole = NodeRole.MASTER;

        try {
            server = new Server(ConfigParser.getPingDelayMs(), ConfigParser.getNodeTimeoutMs());
        } catch (SocketException e) {
            e.printStackTrace();
        }

        field = new Cell[width][height];
        for (Cell[] row : field) {
            Arrays.fill(row, Cell.EMPTY);
        }

        snakeGen(server.setMasterPlayer());
        refreshSnacks();

        start();
    }

    @Override
    public void run() {
        try {
            server.getDataSender().startSendAnnouncementMsg();

            while (!Thread.currentThread().isInterrupted()) {
                synchronized (this) {
                    getSnakesDirection();

                    for (Map.Entry<Snake, Direction> snake : snakes.entrySet()) {
                        snake.getKey().move(snacks, snake.getValue());
                    }

                    setPlayersScore();
                    updateField();
                    checkCollisions();
                    refreshSnacks();
                    server.getDataSender().sendGameStateMsg();

                    fieldNotify();
                    scoreNotify();
                }
                Thread.sleep(stateDelayMs);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateField() {
        clearField();

        for (Snake snake : snakes.keySet()) {
            if (snake.getState() == SnakeState.ALIVE) {
                for (Coord coord : snake.getPoints()) {
                    field[coord.getX()][coord.getY()] = Cell.SNAKE;
                }
            }
        }

        for (Iterator<Coord> iterator = snacks.iterator(); iterator.hasNext(); ) {
            Coord coord = iterator.next();
            if (Cell.SNAKE == field[coord.getX()][coord.getY()]) {
                iterator.remove();
            } else {
                field[coord.getX()][coord.getY()] = Cell.SNACK;
            }
        }

    }

    private void clearField() {
        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                field[i][j] = Cell.EMPTY;
            }
        }
    }

    private int calcAliveSnakes() {
        int count = 0;

        for (Snake snake : snakes.keySet()) {
            if (snake.getState() == SnakeState.ALIVE) {
                ++count;
            }
        }
        return count;
    }

    private void refreshSnacks() {
        int emptyCellCount = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (Cell.EMPTY == field[i][j]) {
                    emptyCellCount++;
                }
            }
        }

        int foodCount;
        if ((foodStatic + Math.round(calcAliveSnakes() * foodPerPlayer)) < emptyCellCount) {
            foodCount = (foodStatic + Math.round(calcAliveSnakes() * foodPerPlayer));
        } else {
            foodCount = emptyCellCount;
        }

        int currentFoodCount = snacks.size();

        while (currentFoodCount < foodCount) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            if (Cell.EMPTY == field[x][y]) {
                snacks.add(new Coord(x, y));
                currentFoodCount++;
            }
        }
    }

    private void checkCollisions() {
        for (Snake snake : snakes.keySet()) {
            Coord head = snake.getPoints().getFirst();

            for (Snake otherSnake : snakes.keySet()) {
                if (otherSnake.equals(snake)) {
                    for (int j = 2; j < otherSnake.getPoints().size(); ++j) {
                        if (head.equals(otherSnake.getPoints().get(j))) {
                            snake.setDead(true);
                        }
                    }
                } else {
                    for (int j = 0; j < otherSnake.getPoints().size(); ++j) {
                        if (head.equals(otherSnake.getPoints().get(j))) {
                            otherSnake.increaseScore();
                            snake.setDead(true);
                        }
                    }
                }
            }

            setPlayersScore();
        }


        for (Iterator<Map.Entry<Snake, Direction>> iterator = snakes.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Snake, Direction> snake = iterator.next();

            if (snake.getKey().isDead()) {
                if (snake.getKey().getPlayerId() != server.getCurrentPlayerId()) {
                    handleSnakeDeath(snake.getKey());
                }
                iterator.remove();
            }
        }
    }

    private void setPlayersScore() {
        List<Snake> snakes = new ArrayList<>(this.snakes.keySet());
        for (Player player : server.getPlayersList()) {
            if (snakes.contains(new Snake(player.getId()))) {
                player.setScore(snakes.get(snakes.indexOf(new Snake(player.getId()))).getScore());
            }
        }
    }

    private void handleSnakeDeath(Snake snake) {
        IpPort ipPort = server.findIpPort(snake.getPlayerId());
        if (null == ipPort) {
            LOGGER.severe("Error. Couldn't find ip and port for player with id:" + snake.getPlayerId());
        } else {
            server.getDataSender().sendMakeViewerMsg(ipPort.getPort(), ipPort.getIp(), server.getCurrentPlayerId(), snake.getPlayerId());
        }
    }

    synchronized private boolean snakeGen(int playerId) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                boolean flag = true;

                for (int k = 0; k < 5; k++) {
                    for (int l = 0; l < 5; l++) {
                        if (field[i + k][j + l] == Cell.SNAKE) {
                            flag = false;
                            break;
                        }
                    }
                    if (!flag) {
                        break;
                    }
                }

                if (flag) {
                    for (int k = 0; k < 5; k++) {
                        boolean innerFlag = false;
                        int x = random.nextInt(5) + i;
                        int y = random.nextInt(5) + j;

                        if (field[x][y] != Cell.SNACK) {
                            Direction direction = Direction.forNumber(random.nextInt(4) + 1);
                            assert direction != null;
                            switch (direction) {
                                case UP:
                                    if (y - 1 < 0) {
                                        y = height - 1;
                                    }
                                    if (field[x][y - 1] != Cell.SNACK) {
                                        innerFlag = true;
                                    }
                                    break;
                                case DOWN:
                                    if (y + 1 > height - 1) {
                                        y = 0;
                                    }
                                    if (field[x][y + 1] != Cell.SNACK) {
                                        innerFlag = true;
                                    }
                                    break;
                                case LEFT:
                                    if (x - 1 < 0) {
                                        x = width - 1;
                                    }
                                    if (field[x - 1][y] != Cell.SNACK) {
                                        innerFlag = true;
                                    }
                                    break;
                                case RIGHT:
                                    if (x + 1 > width - 1) {
                                        x = 0;
                                    }
                                    if (field[x + 1][y] != Cell.SNACK) {
                                        innerFlag = true;
                                    }
                                    break;
                            }

                            if (innerFlag) {
                                Snake snake = new Snake(new Coord(x, y), direction, height, width, playerId, SnakeState.ALIVE);
                                snakes.put(snake, snake.getHeadDirection());
                                return true;
                            }
                        }
                    }
                } else {
                    i += 5;
                }
            }
        }
        return false;
    }

    synchronized private void getSnakesDirection() {
        for (Player player : server.getPlayersList()) {
            player.getLock().lock();
            if (player.getId() != server.getCurrentPlayerId()) {
                if (snakes.containsKey(new Snake(player.getId()))) {
                    if (player.getLastSteer() != null) {
                        snakes.put(new Snake(player.getId()), player.getLastSteer());
                        player.setLastSteer(null);
                    }
                }
            }
            player.getLock().unlock();
        }
    }


    @Override
    public void registerObserver(ModelObserver observer) {
        fieldObservers.add(observer);
    }

    @Override
    public void removeObserver(ModelObserver observer) {
        fieldObservers.remove(observer);
    }

    @Override
    public void fieldNotify() {
        for (ModelObserver observer : fieldObservers) {
            observer.updateField(field);
        }
    }

    @Override
    public void registerObserver(ScoreObserver observer) {
        scoreObservers.add(observer);
    }

    @Override
    public void removeObserver(ScoreObserver observer) {
        scoreObservers.remove(observer);
    }

    @Override
    public void scoreNotify() {
        List<PlayerInfo> playerInfos = new ArrayList<>(server.getPlayersList());
        for (ScoreObserver observer : scoreObservers) {
            observer.updateScoreTable(playerInfos);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getStateDelayMs() {
        return stateDelayMs;
    }

    public int getFoodStatic() {
        return foodStatic;
    }

    public float getFoodPerPlayer() {
        return foodPerPlayer;
    }

    public float getDeadFoodProb() {
        return deadFoodProb;
    }

    public Server getServer() {
        return server;
    }

    public List<Snake> getSnakes() {
        return new ArrayList<>(this.snakes.keySet());
    }

    public ArrayList<Coord> getSnacks() {
        return snacks;
    }


    public void setSnakeDirection(Direction snakeDirection) {

        switch (nodeRole) {
            case MASTER:
                synchronized (this) {
                    if (snakes.containsKey(new Snake(server.getCurrentPlayerId()))) {
                        snakes.put(new Snake(server.getCurrentPlayerId()), snakeDirection);
                    }
                }
                break;
            case NORMAL:
            case DEPUTY:
                IpPort ipPort = server.getMasterPlayerIpPort();
                if (ipPort != null) {
                    try {
                        server.getDataSender().sendSteerMsg(ipPort.getPort(), ipPort.getIp(), snakeDirection);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case VIEWER:
                break;
        }
    }


    public class Server {
        private DatagramSocket nodeSocket;

        private int currentPlayerId;
        private Player deputy = null;

        private final int pingDelayMs;
        private final int nodeTimeoutMs;

        private List<Player> playersList = new CopyOnWriteArrayList<>();
        private int lastPlayerId = 0;

        private DataSender dataSender = new DataSender();
        private final Map<Long, DatagramPacket> sentMsgs = new HashMap<>();

        private DataReceiver dataReceiver = new DataReceiver();
        private final List<Long> confirmedMessages = new ArrayList<>();

        private int lastGameStateNumOrder = -1;

        private long lastTimeMaster = System.currentTimeMillis();
        private IpPort masterPlayerIpPort = null;

        private long joinMsgSeq;

        Server(int pingDelayMs, int nodeTimeoutMs) throws SocketException {
            nodeSocket = new DatagramSocket();
            this.pingDelayMs = pingDelayMs;
            this.nodeTimeoutMs = nodeTimeoutMs;

            dataSender.start();
            dataReceiver.start();
        }

        public List<Player> getPlayersList() {
            return playersList;
        }

        public int getPingDelayMs() {
            return pingDelayMs;
        }

        public int getNodeTimeoutMs() {
            return nodeTimeoutMs;
        }

        DataSender getDataSender() {
            return dataSender;
        }

        IpPort getMasterPlayerIpPort() {
            return masterPlayerIpPort;
        }

        int getCurrentPlayerId() {
            return currentPlayerId;
        }

        void setPlayersList(List<Player> playersList) {
            this.playersList = playersList;
        }

        public Player getDeputy() {
            return deputy;
        }

        private int setMasterPlayer() {
            currentPlayerId = lastPlayerId++;
            playersList.add(new Player(currentPlayerId, "", nodeSocket.getLocalPort(), -1, ConfigParser.getName(), NodeRole.MASTER, 0 ));
            return currentPlayerId;
        }

        void setLastPlayerId(int lastPlayerId) {
            this.lastPlayerId = lastPlayerId;
        }

        private IpPort findIpPort(int playerId) {
            for (Player player : playersList) {
                if (player.getId() == playerId) {
                    return new IpPort(player.getIp(), player.getPort());
                }
            }
            return null;
        }


        private void checkOtherPlayers() throws UnknownHostException {
            switch (nodeRole) {
                case VIEWER:
                case NORMAL:
                    if (null != masterPlayerIpPort) {
                        if (System.currentTimeMillis() - lastTimeMaster > nodeTimeoutMs) {
                            findNewMaster();
                        }
                    }
                    break;
                case DEPUTY:
                    if (null != masterPlayerIpPort) {
                        if (System.currentTimeMillis() - lastTimeMaster > nodeTimeoutMs) {
                            synchronized (GameModel.this) {
                                nodeRole = NodeRole.MASTER;
                            }

                            MsgBuilder.setStateOrder(new AtomicInteger(lastGameStateNumOrder));

                            for (Player player : playersList) {
                                if (player.getId() == currentPlayerId) {
                                    player.setNodeRole(NodeRole.MASTER);
                                    break;
                                }
                            }

                            playersList.remove(new Player(masterPlayerIpPort.getIp(), masterPlayerIpPort.getPort()));

                            dataSender.sendNewMaster(currentPlayerId);
                            findDeputy();

                            GameModel.this.start();
                        }
                    }

                    break;
                case MASTER:
                    for (Player player : playersList) {
                        if (player.getId() != currentPlayerId) {
                            if (System.currentTimeMillis() - player.getLastTime() > nodeTimeoutMs) {
                                playersList.remove(player);
                                if (player.getNodeRole() == NodeRole.DEPUTY) {
                                    findDeputy();
                                    break;
                                }
                            }
                        }
                    }
                    break;
            }
        }

        private void findDeputy() throws UnknownHostException {
            if (playersList.size() > 0) {
                for (Player player : playersList) {
                    if (player.getNodeRole() == NodeRole.NORMAL) {
                        player.setNodeRole(NodeRole.DEPUTY);
                        dataSender.sendMakeDeputyMsg(player.getPort(), player.getIp(), currentPlayerId, player.getId());
                        deputy = player;
                        return;
                    }
                }
            }
            deputy = null;
        }


        private void findNewMaster() {
            IpPort newMasterIpPort = null;
            for (Player player : playersList) {
                if (player.getNodeRole() == NodeRole.DEPUTY) {
                    newMasterIpPort = new IpPort(player.getIp(), player.getPort());
                    break;
                }
            }

            if (newMasterIpPort == null) {
                LOGGER.severe("Can't find info about new master");
            } else {
                masterPlayerIpPort = newMasterIpPort;
            }
        }


        private class DataSender extends Thread {
            @Override
            public void run() {
                long msgLastTime = 0;

                while (!Thread.currentThread().isInterrupted()) try {
                    Server.this.checkOtherPlayers();

                    synchronized (sentMsgs) {
                        sentMsgs.entrySet().removeIf(msg -> {
                            boolean status = checkDeliveryStatus(msg.getKey());
                            if (status) {
                                confirmedMessages.remove(msg.getKey());
                            }
                            return status;
                        });

                        for (Map.Entry<Long, DatagramPacket> entry : sentMsgs.entrySet()) {
                            switch (nodeRole) {
                                case MASTER:
                                    nodeSocket.send(entry.getValue());
                                    msgLastTime = System.currentTimeMillis();
                                    break;
                                case NORMAL:
                                case DEPUTY:
                                    if (masterPlayerIpPort != null) {
                                        if (entry.getValue().getPort() != masterPlayerIpPort.getPort()
                                                || entry.getValue().getAddress().getHostAddress().equals(masterPlayerIpPort.getIp())) {

                                            entry.setValue(makePacket(entry.getValue().getData(),
                                                    entry.getValue().getLength(),
                                                    masterPlayerIpPort.getIp(),
                                                    masterPlayerIpPort.getPort()));
                                        }
                                    }
                                    nodeSocket.send(entry.getValue());
                                    msgLastTime = System.currentTimeMillis();

                                    break;
                                case VIEWER:
                                    break;
                            }
                        }
                    }

                    if (System.currentTimeMillis() - msgLastTime > pingDelayMs)
                        switch (nodeRole) {
                            case DEPUTY:
                            case NORMAL:
                            case VIEWER:
                                if (null != masterPlayerIpPort) {
                                    sendPing(masterPlayerIpPort.getPort(), masterPlayerIpPort.getIp());
                                }
                                break;
                            case MASTER:
                                break;
                        }

                    Thread.sleep(pingDelayMs);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }

            private boolean checkDeliveryStatus(long msgSeq) {
                synchronized (confirmedMessages) {
                    return confirmedMessages.contains(msgSeq);
                }
            }

            private void startSendAnnouncementMsg() {
                new Thread(() -> {
                    int PORT = 9192;
                    String multicastIp = "239.192.0.4";

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            byte[] announcementMsg = MsgBuilder.buildAnnouncementMsg(GameModel.this).toByteArray();

                            nodeSocket.send(new DatagramPacket(
                                    announcementMsg,
                                    announcementMsg.length,
                                    InetAddress.getByName(multicastIp),
                                    PORT
                            ));

                            Thread.sleep(1000);
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }


            private DatagramPacket makePacket(byte[] data, int length, String ip, int port) throws UnknownHostException {
                return new DatagramPacket(data, length, InetAddress.getByName(ip), port);
            }

            private void sendAskMsg(int port, String ip, long msgSeq) throws IOException {
                byte[] ackMsg = MsgBuilder.buildAckMsg(msgSeq, currentPlayerId).toByteArray();
                nodeSocket.send(makePacket(ackMsg, ackMsg.length, ip, port));
            }

            private void sendAskMsg(int port, String ip, long msgSeq, int receiverId) throws IOException {
                byte[] ackMsg = MsgBuilder.buildAckMsg(msgSeq, currentPlayerId, receiverId).toByteArray();
                nodeSocket.send(makePacket(ackMsg, ackMsg.length, ip, port));
            }

            private void sendGameStateMsg() {
                synchronized (GameModel.this) {
                    for (Player player : playersList) {
                        if (player.getId() != currentPlayerId) {
                            GameMessage gameStateMsg = MsgBuilder.buildGameStateMsg(GameModel.this);

                            try {
                                synchronized (sentMsgs) {
                                    sentMsgs.put(gameStateMsg.getMsgSeq(), makePacket(
                                            gameStateMsg.toByteArray(),
                                            gameStateMsg.toByteArray().length,
                                            player.getIp(),
                                            player.getPort()));
                                }
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            private void sendJoinMsg(int port, String ip) throws IOException {
                GameMessage joinMsg = MsgBuilder.buildJoinMsg(ConfigParser.getName());
                joinMsgSeq = joinMsg.getMsgSeq();
                synchronized (sentMsgs) {
                    sentMsgs.put(joinMsg.getMsgSeq(), makePacket(joinMsg.toByteArray(), joinMsg.toByteArray().length, ip, port));
                }
            }

            private void sendSteerMsg(int port, String ip, Direction direction) throws IOException {
                GameMessage steerMsg = MsgBuilder.buildSteerMsg(direction);
                synchronized (sentMsgs) {
                    sentMsgs.put(steerMsg.getMsgSeq(), makePacket(steerMsg.toByteArray(), steerMsg.toByteArray().length, ip, port));
                }
            }

            private void sendNewMaster(int senderId) throws UnknownHostException {
                for (Player player : playersList) {
                    if (player.getId() != currentPlayerId) {
                        GameMessage masterMsg = MsgBuilder.buildRoleChangeNewMasterMsg(senderId, player.getId());
                        synchronized (sentMsgs) {
                            sentMsgs.put(masterMsg.getMsgSeq(), makePacket(
                                    masterMsg.toByteArray(),
                                    masterMsg.toByteArray().length,
                                    player.getIp(),
                                    player.getPort()));
                        }
                    }
                }
            }

            private void sendMakeMasterMsg(int port, String ip, int senderId, int receiverId) throws UnknownHostException {
                GameMessage masterMsg = MsgBuilder.buildRoleChangeMakeMasterMsg(senderId, receiverId);
                synchronized (sentMsgs) {
                    sentMsgs.put(masterMsg.getMsgSeq(), makePacket(masterMsg.toByteArray(), masterMsg.toByteArray().length, ip, port));
                }
            }

            private void sendMakeDeputyMsg(int port, String ip, int senderId, int receiverId) throws UnknownHostException {
                GameMessage masterMsg = MsgBuilder.buildRoleChangeMakeDeputyMsg(senderId, receiverId);
                synchronized (sentMsgs) {
                    sentMsgs.put(masterMsg.getMsgSeq(), makePacket(masterMsg.toByteArray(), masterMsg.toByteArray().length, ip, port));
                }
            }

            private void sendMakeViewerMsg(int port, String ip, int senderId, int receiverId) {
                GameMessage masterMsg = MsgBuilder.buildRoleChangeMakeViewerMsg(senderId, receiverId);
                synchronized (sentMsgs) {
                    try {
                        sentMsgs.put(masterMsg.getMsgSeq(), makePacket(masterMsg.toByteArray(), masterMsg.toByteArray().length, ip, port));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void sendPing(int port, String ip) throws UnknownHostException {
                GameMessage masterMsg = MsgBuilder.buildPingMsg();
                synchronized (sentMsgs) {
                    sentMsgs.put(masterMsg.getMsgSeq(), makePacket(masterMsg.toByteArray(), masterMsg.toByteArray().length, ip, port));
                }
            }
        }

        private class DataReceiver extends Thread {
            private final int PACKET_SIZE = 2048;

            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);

                        nodeSocket.receive(packet);
                        GameMessage gameMessage = GameMessage.parseFrom(ByteBuffer.wrap(packet.getData(), 0, packet.getLength()));
                        refreshPlayerTime(packet.getPort(), packet.getAddress().getHostAddress());

                        switch (gameMessage.getTypeCase()) {
                            case PING:
                                dataSender.sendAskMsg(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage.getMsgSeq());
                                break;
                            case JOIN:
                                handleJoinMessage(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage);
                                break;
                            case STEER:
                                handleSteerMessage(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage);
                                break;
                            case ACK:
                                handleAckMessage(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage);
                                break;
                            case STATE:
                                handleStateMessage(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage);
                                break;
                            case ROLE_CHANGE:
                                System.out.println(gameMessage);

                                handleRoleChangeMessage(packet.getPort(), packet.getAddress().getHostAddress(), gameMessage);
                                break;
                            case ERROR:
                                break;
                            case ANNOUNCEMENT:
                            case TYPE_NOT_SET:
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            private void refreshPlayerTime(int port, String ip) {
                switch (nodeRole) {

                    case NORMAL:
                    case DEPUTY:
                        lastTimeMaster = System.currentTimeMillis();
                        break;
                    case MASTER:
                        for (Player player : playersList) {
                            if (player.getIp().equals(ip) && player.getPort() == port) {
                                player.setLastTime(System.currentTimeMillis());
                            }
                        }
                        break;
                    case VIEWER:
                        break;
                }
            }

            private void handleJoinMessage(int port, String ip, GameMessage gameMessage) throws IOException {
                if (NodeRole.MASTER == nodeRole) {
                    GameMessage.JoinMsg joinMsg = gameMessage.getJoin();

                    int newPlayerId;

                    if (!playersList.contains(new Player(ip, port))) {
                        newPlayerId = lastPlayerId;

                        Player player;
                        if (deputy == null) {
                            player = new Player(lastPlayerId++, ip, port, System.currentTimeMillis(), joinMsg.getName(), NodeRole.DEPUTY, 0);
                            deputy = player;
                            dataSender.sendMakeDeputyMsg(port, ip, currentPlayerId, newPlayerId);
                        } else {
                            player = new Player(lastPlayerId++, ip, port, System.currentTimeMillis(), joinMsg.getName(), NodeRole.NORMAL, 0);
                        }
                        playersList.add(player);

                        snakeGen(newPlayerId);

                    } else {
                        newPlayerId = playersList.get(playersList.indexOf(new Player(ip, port))).getId();
                    }

                    dataSender.sendAskMsg(port, ip, gameMessage.getMsgSeq(), newPlayerId);
                }
            }

            private void handleAckMessage(int port, String ip, GameMessage gameMessage) {
                if (NodeRole.MASTER != nodeRole && NodeRole.VIEWER != nodeRole) {
                    if (gameMessage.getMsgSeq() == joinMsgSeq && masterPlayerIpPort == null) {
                        currentPlayerId = gameMessage.getReceiverId();
                        masterPlayerIpPort = new IpPort(ip, port);
                    }
                }

                synchronized (confirmedMessages) {
                    confirmedMessages.add(gameMessage.getMsgSeq());
                }
            }

            private void handleSteerMessage(int port, String ip, GameMessage gameMessage) throws IOException {
                Player player = playersList.get(playersList.indexOf(new Player(ip, port)));

                player.getLock().lock();
                if (player.getLastSteerMsgSeq() < gameMessage.getMsgSeq()) {
                    player.setLastSteer(gameMessage.getSteer().getDirection());
                }
                player.getLock().unlock();

                dataSender.sendAskMsg(port, ip, gameMessage.getMsgSeq());
            }

            private void parseGameState(GameState gameState) {
                synchronized (GameModel.this) {
                    server.setPlayersList(MsgParser.parsePlayers(gameState.getPlayers().getPlayersList()));
                    int maxId = 0;
                    for (Player player :
                            playersList) {
                        if (player.getId() > maxId) {
                            maxId = player.getId();
                        }
                    }
                    lastPlayerId = maxId + 1;
                    snakes = MsgParser.parseSnakes(gameState.getSnakesList(), width, height);
                    snacks = MsgParser.convertCoord(gameState.getFoodsList());
                }
            }

            private void handleStateMessage(int port, String ip, GameMessage gameMessage) throws IOException {
                lastTimeMaster = System.currentTimeMillis();

                if (nodeRole != NodeRole.MASTER) {
                    if (null != masterPlayerIpPort) {
                        if (lastGameStateNumOrder < gameMessage.getState().getState().getStateOrder()) {
                            lastGameStateNumOrder = gameMessage.getState().getState().getStateOrder();
                            synchronized (GameModel.this) {
                                parseGameState(gameMessage.getState().getState());
                                updateField();
                                fieldNotify();
                                scoreNotify();
                            }
                        }
                    }
                    dataSender.sendAskMsg(port, ip, gameMessage.getMsgSeq());
                }
            }

            private void handleRoleChangeMessage(int port, String ip, GameMessage gameMessage) throws IOException {
                if (NodeRole.VIEWER != nodeRole) {
                    GameMessage.RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();

                    if (roleChangeMsg.hasReceiverRole()) {
                        synchronized (GameModel.this) {
                            switch (roleChangeMsg.getReceiverRole()) {
                                case MASTER:
                                    nodeRole = NodeRole.MASTER;
                                    dataSender.sendNewMaster(currentPlayerId);
                                    GameModel.this.start();
                                    break;
                                case DEPUTY:
                                    nodeRole = NodeRole.DEPUTY;
                                    break;
                                case VIEWER:
                                    nodeRole = NodeRole.VIEWER;
                                    break;
                                case NORMAL:
                                    break;
                            }
                        }
                    }

                    if (roleChangeMsg.hasSenderRole()) {
                        synchronized (GameModel.this) {
                            switch (roleChangeMsg.getSenderRole()) {
                                case MASTER:
                                    masterPlayerIpPort = new IpPort(ip, port);
                                case DEPUTY:
                                case VIEWER:
                                case NORMAL:
                                    break;
                            }
                        }
                    }
                    dataSender.sendAskMsg(port, ip, gameMessage.getMsgSeq());
                }
            }
        }
    }
}
