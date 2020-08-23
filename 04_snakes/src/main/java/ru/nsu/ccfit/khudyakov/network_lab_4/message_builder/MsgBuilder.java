package ru.nsu.ccfit.khudyakov.network_lab_4.message_builder;


import ru.nsu.ccfit.khudyakov.network_lab_4.model.GameModel;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Coord;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Player;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Snake;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.abs;
import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.*;
import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameMessage.AnnouncementMsg;

public class MsgBuilder {
    volatile static int msgSeq = 0;
    volatile static int stateOrder = 0;

    private static GameConfig buildGameConfig(GameModel gameModel) {
        return GameConfig.newBuilder().setWidth(gameModel.getWidth())
                .setHeight(gameModel.getHeight())
                .setFoodStatic(gameModel.getFoodStatic())
                .setFoodPerPlayer(gameModel.getFoodPerPlayer())
                .setStateDelayMs(gameModel.getStateDelayMs())
                .setDeadFoodProb(gameModel.getDeadFoodProb())
                .setPingDelayMs(gameModel.getServer().getPingDelayMs())
                .setPingDelayMs(gameModel.getServer().getNodeTimeoutMs())
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
                .setMsgSeq(msgSeq++)
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
        gameStateBuilder.setStateOrder(stateOrder++);
        GameState gameState = gameStateBuilder.build();
        return GameMessage.newBuilder().setMsgSeq(msgSeq++).setState(GameMessage.StateMsg.newBuilder().setState(gameState)).build();
    }

    public static GameMessage buildJoinMsg(String name) {
        GameMessage.JoinMsg.Builder gameJoinMsg = GameMessage.JoinMsg.newBuilder();
        gameJoinMsg.setName(name);

        return GameMessage.newBuilder().setMsgSeq(msgSeq++).setJoin(gameJoinMsg).build();
    }

    public static GameMessage buildSteerMsg(Direction direction) {
        GameMessage.SteerMsg.Builder gameSteerMsg = GameMessage.SteerMsg.newBuilder();
        gameSteerMsg.setDirection(direction);

        return GameMessage.newBuilder().setMsgSeq(msgSeq++).setSteer(gameSteerMsg).build();
    }


    private static List<Coord> convertCoordsToOffsets(List<Coord> coords, int width, int height) {

        int x = 0, y = 0;
        Coord prevCoord;
        LinkedList<Coord> offsets = new LinkedList<>();
        offsets.add(coords.get(0));

        prevCoord = coords.get(0);

        Coord offset = new Coord(0, 0);
        int offsetCount = 0;

        x = calcNewX(width, coords.get(0), coords.get(1));
        y = calcNewY(height, coords.get(0), coords.get(1));

        x = x / (abs(x) + abs(y));
        y = y / (abs(x) + abs(y));

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
                } else if (x > height - 1) {
                    y -= height;
                }

                if (coord.getX() == x && coord.getY() == y) {
                    offsetCount++;
                } else {
                    x = calcNewX(width, prevCoord, coord);
                    y = calcNewY(height, prevCoord, coord);

                    x = x /  (abs(x) + abs(y));
                    y = y /  (abs(x) + abs(y));

                    addOffset(offsets, offset, offsetCount);
                    offset = new Coord(0, 0);
                    offset.setX(x);
                    offset.setY(y);
                    offsetCount = 0;
                }

                prevCoord = new Coord(coord.getX(), coord.getY());
            }

            addOffset(offsets, offset, offsetCount);
        }

        return offsets;
    }

    private static void addOffset(LinkedList<Coord> offsets, Coord offset, int offsetCount) {
        if (offsetCount == 0) {
            offsets.add(new Coord(offset.getX(), offset.getY()));
        } else {
            offsets.add(new Coord(offset.getX() * offsetCount, offset.getY() * offsetCount));
        }
    }

    private static int calcNewX(int weight, Coord prevCoord, Coord coord) {
        return coord.getX() - prevCoord.getX();
    }

    private static int calcNewY(int height, Coord prevCoord, Coord coord) {
        return coord.getY() - prevCoord.getY();
    }


}
