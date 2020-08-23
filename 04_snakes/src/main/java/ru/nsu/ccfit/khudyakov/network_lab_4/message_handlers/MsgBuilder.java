package ru.nsu.ccfit.khudyakov.network_lab_4.message_handlers;


import ru.nsu.ccfit.khudyakov.network_lab_4.model.GameModel;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Coord;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Player;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Snake;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;
import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.*;
import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameMessage.AnnouncementMsg;

public class MsgBuilder {
    private static AtomicInteger msgSeq = new AtomicInteger(0);
    private static AtomicInteger stateOrder = new AtomicInteger(0);

    private static GameConfig buildGameConfig(GameModel gameModel) {
        return GameConfig.newBuilder().setWidth(gameModel.getWidth())
                .setHeight(gameModel.getHeight())
                .setFoodStatic(gameModel.getFoodStatic())
                .setFoodPerPlayer(gameModel.getFoodPerPlayer())
                .setStateDelayMs(gameModel.getStateDelayMs())
                .setDeadFoodProb(gameModel.getDeadFoodProb())
                .setPingDelayMs(gameModel.getServer().getPingDelayMs())
                .setNodeTimeoutMs(gameModel.getServer().getNodeTimeoutMs())
                .build();
    }

    private static SnakesProto.GamePlayer buildGamePlayer(Player player) {
        return GamePlayer.newBuilder()
                .setId(player.getId())
                .setPort(player.getPort())
                .setIpAddress(player.getIp())
                .setScore(player.getScore())
                .setRole(player.getNodeRole())
                .setName(player.getName())
                .build();
    }

    private static SnakesProto.GamePlayers buildGamePlayers(GameModel gameModel) {
        GamePlayers.Builder gamePlayers = GamePlayers.newBuilder();

        List<Player> playersList = gameModel.getServer().getPlayersList();

        if (playersList.size() > 0) {
            for (Player player : playersList) {
                gamePlayers.addPlayers(buildGamePlayer(player));
            }
        }

        return gamePlayers.build();
    }

    public static GameMessage buildAnnouncementMsg(GameModel gameModel) {
        return GameMessage.newBuilder()
                .setMsgSeq(msgSeq.getAndIncrement())
                .setAnnouncement(
                        AnnouncementMsg.newBuilder()
                                .setPlayers(buildGamePlayers(gameModel))
                                .setConfig(buildGameConfig(gameModel))
                                .build()
                )
                .build();
    }

    public static GameMessage buildAckMsg(long requiredMsgSeq, int senderId, int receiverId) {
        return GameMessage.newBuilder()
                .setAck(GameMessage.AckMsg.newBuilder().build())
                .setMsgSeq(requiredMsgSeq)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public static GameMessage buildAckMsg(long requiredMsgSeq, int senderId) {
        return GameMessage.newBuilder()
                .setAck(GameMessage.AckMsg.newBuilder().build())
                .setMsgSeq(requiredMsgSeq)
                .setSenderId(senderId)
                .build();
    }


    private static GameState.Snake buildSnake(Snake snake, int width, int height) {
        GameState.Snake.Builder snakeBuilder = GameState.Snake.newBuilder();

        snakeBuilder.setPlayerId(snake.getPlayerId());
        snakeBuilder.setHeadDirection(snake.getHeadDirection());

        List<Coord> coords = convertCoordsToOffsets(snake.getPoints(), width, height);
        for (Coord coord : coords) {
            snakeBuilder.addPoints(GameState.Coord.newBuilder()
                    .setX(coord.getX())
                    .setY(coord.getY())
                    .build());
        }
        snakeBuilder.setState(snake.getState());
        return snakeBuilder.build();
    }


    public static GameMessage buildGameStateMsg(GameModel gameModel) {
        GameState.Builder gameStateBuilder = GameState.newBuilder();

        for (Snake snake : gameModel.getSnakes()) {
            gameStateBuilder.addSnakes(buildSnake(snake, gameModel.getWidth(), gameModel.getHeight()));
        }

        gameStateBuilder.setConfig(buildGameConfig(gameModel));
        gameStateBuilder.setPlayers(buildGamePlayers(gameModel));
        for (Coord coord : gameModel.getSnacks()) {
            gameStateBuilder.addFoods(GameState.Coord.newBuilder()
                    .setX(coord.getX())
                    .setY(coord.getY())
                    .build());
        }
        gameStateBuilder.setStateOrder(stateOrder.getAndIncrement());
        GameState gameState = gameStateBuilder.build();
        return GameMessage.newBuilder().setMsgSeq(msgSeq.getAndIncrement()).setState(GameMessage.StateMsg.newBuilder().setState(gameState)).build();
    }

    public static GameMessage buildJoinMsg(String name) {
        GameMessage.JoinMsg.Builder gameJoinMsg = GameMessage.JoinMsg.newBuilder();
        gameJoinMsg.setName(name);

        return GameMessage.newBuilder().setMsgSeq(msgSeq.getAndIncrement()).setJoin(gameJoinMsg).build();
    }

    public static GameMessage buildSteerMsg(Direction direction) {
        GameMessage.SteerMsg.Builder gameSteerMsg = GameMessage.SteerMsg.newBuilder();
        gameSteerMsg.setDirection(direction);

        return GameMessage.newBuilder().setMsgSeq(msgSeq.getAndIncrement()).setSteer(gameSteerMsg).build();
    }

    public static GameMessage buildRoleChangeNewMasterMsg(int senderId, int receiverId) {
        GameMessage.RoleChangeMsg.Builder gameRoleChangeMsg = GameMessage.RoleChangeMsg.newBuilder();
        gameRoleChangeMsg.setSenderRole(NodeRole.MASTER);

        return GameMessage.newBuilder()
                .setMsgSeq(msgSeq.getAndIncrement())
                .setRoleChange(gameRoleChangeMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public static GameMessage buildRoleChangeMakeViewerMsg(int senderId, int receiverId) {
        GameMessage.RoleChangeMsg.Builder gameRoleChangeMsg = GameMessage.RoleChangeMsg.newBuilder();
        gameRoleChangeMsg.setReceiverRole(NodeRole.VIEWER);

        return GameMessage.newBuilder()
                .setMsgSeq(msgSeq.getAndIncrement())
                .setRoleChange(gameRoleChangeMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public static GameMessage buildRoleChangeMakeDeputyMsg(int senderId, int receiverId) {
        GameMessage.RoleChangeMsg.Builder gameRoleChangeMsg = GameMessage.RoleChangeMsg.newBuilder();
        gameRoleChangeMsg.setReceiverRole(NodeRole.DEPUTY);

        return GameMessage.newBuilder()
                .setMsgSeq(msgSeq.getAndIncrement())
                .setRoleChange(gameRoleChangeMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }


    public static GameMessage buildRoleChangeMakeMasterMsg(int senderId, int receiverId) {
        GameMessage.RoleChangeMsg.Builder gameRoleChangeMsg = GameMessage.RoleChangeMsg.newBuilder();
        gameRoleChangeMsg.setReceiverRole(NodeRole.MASTER);

        return GameMessage.newBuilder()
                .setMsgSeq(msgSeq.getAndIncrement())
                .setRoleChange(gameRoleChangeMsg)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public static GameMessage buildPingMsg() {
        GameMessage.PingMsg.Builder pingMsg = GameMessage.PingMsg.newBuilder();
        return GameMessage.newBuilder().setMsgSeq(msgSeq.getAndIncrement()).setPing(pingMsg).build();
    }


    private static List<Coord> convertCoordsToOffsets(List<Coord> coords, int width, int height) {
        LinkedList<Coord> offsets = new LinkedList<>();
        int x, y;

        Coord prevCoord;

        offsets.add(coords.get(0));

        prevCoord = coords.get(0);

        Coord offset = new Coord(0, 0);
        int offsetCount = 0;

        x = calcNewCoord(width, coords.get(0).getX(), coords.get(1).getX());
        y = calcNewCoord(height, coords.get(0).getY(), coords.get(1).getY());

        offset.setX(x);
        offset.setY(y);

        if (coords.size() == 2) {
            offsets.add(offset);
        } else {
            for (int i = 1; i < coords.size(); i++) {
                Coord coord = coords.get(i);

                x = prevCoord.getX() + offset.getX();
                if (x < 0) {
                    x += width;
                } else if (x > width - 1) {
                    x -= width;
                }

                y = prevCoord.getY() + offset.getY();
                if (y < 0) {
                    y += height;
                } else if (y > height - 1) {
                    y -= height;
                }

                if (coord.getX() == x && coord.getY() == y) {
                    ++offsetCount;
                } else {
                    x = calcNewCoord(width, prevCoord.getX(), coord.getX());
                    y = calcNewCoord(height, prevCoord.getY(), coord.getY());

                    addOffset(offsets, offset, offsetCount);

                    offset = new Coord(0, 0);
                    offset.setX(x);
                    offset.setY(y);
                    offsetCount = 1;
                }

                prevCoord = new Coord(coord.getX(), coord.getY());
            }

            addOffset(offsets, offset, offsetCount);
        }

        return offsets;
    }

    public static void setStateOrder(AtomicInteger stateOrder) {
        MsgBuilder.stateOrder = stateOrder;
    }

    private static void addOffset(LinkedList<Coord> offsets, Coord offset, int offsetCount) {
        if (offsetCount == 0) {
            offsets.add(new Coord(offset.getX(), offset.getY()));
        } else {
            offsets.add(new Coord(offset.getX() * offsetCount, offset.getY() * offsetCount));
        }
    }

    private static int calcNewCoord(int size, int prevCoord, int coord) {
        int offset = coord - prevCoord;
        if ((offset == size - 1) || (offset == -(size - 1))) {
            offset = (-1) * (offset / (abs(offset)));
        }
        return offset;
    }
}
