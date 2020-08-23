package ru.nsu.ccfit.khudyakov.network_lab_4.message_builder;

import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Coord;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Player;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Snake;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Math.abs;

public class MsgParser {

    public static Map<Snake, SnakesProto.Direction> parseSnakes(List<SnakesProto.GameState.Snake> snakes, int width, int height) {
        Map<Snake, SnakesProto.Direction> newSnakes = new HashMap<>();

        for (SnakesProto.GameState.Snake snake : snakes) {
            newSnakes.put(new Snake(
                    convertSnakeOffsetsCoords(convertCoord(snake.getPointsList()), width, height),
                    snake.getHeadDirection(),
                    height,
                    width,
                    snake.getPlayerId(),
                    snake.getState()
            ), snake.getHeadDirection());
        }

        return newSnakes;
    }

    public static List<Player> parsePlayers(List<SnakesProto.GamePlayer> playerList) {
        List<Player> players = new CopyOnWriteArrayList<>();
        for (SnakesProto.GamePlayer gamePlayer : playerList) {
            players.add(new Player(
                    gamePlayer.getId(),
                    gamePlayer.getIpAddress(),
                    gamePlayer.getPort(),
                    System.currentTimeMillis(),
                    gamePlayer.getName()
            ));
        }
        return players;
    }

    public static ArrayList<Coord> convertCoord(List<SnakesProto.GameState.Coord> coords) {
        ArrayList<Coord> newCoords = new ArrayList<>();
        for (SnakesProto.GameState.Coord coord : coords) {
            newCoords.add(new Coord(coord.getX(), coord.getY()));
        }
        return newCoords;
    }

    public static LinkedList<Coord> convertSnakeOffsetsCoords(List<Coord> offsets, int width, int height) {
        LinkedList<Coord> coords = new LinkedList<>();
        Coord offsetCoord;
        Coord prevCoord;


        if (!offsets.isEmpty()) {
            prevCoord = offsets.remove(0);

            Coord temp = new Coord(prevCoord.getX(), prevCoord.getY());
            coords.add(temp);

            while (!offsets.isEmpty()) {
                offsetCoord = offsets.remove(0);
                int mod = abs(offsetCoord.getX()) + abs(offsetCoord.getY());

                if ((offsetCoord.getX() != 0 && offsetCoord.getY() != 0) || mod == 0) {
                    System.out.println("illegal snake state");
                    break;
                }

                offsetCoord.setX(offsetCoord.getX() / mod);
                offsetCoord.setY(offsetCoord.getY() / mod);

                while (mod > 0) {
                    temp = new Coord(0, 0);

                    temp.setX((prevCoord.getX() + offsetCoord.getX()) % width);
                    temp.setY((prevCoord.getY() + offsetCoord.getY()) % height);

                    if (temp.getX() < 0) {
                        temp.setX(prevCoord.getX() + (width - 1));
                    }
                    if (temp.getY() < 0) {
                        temp.setY(prevCoord.getY() + (height - 1));
                    }

                    prevCoord = temp;
                    coords.add(temp);
                    mod--;
                }
            }
            return coords;
        } else {
            return null;
        }
    }
}
