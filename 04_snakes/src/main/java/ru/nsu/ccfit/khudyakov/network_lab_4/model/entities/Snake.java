package ru.nsu.ccfit.khudyakov.network_lab_4.model.entities;

import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameState.Snake.SnakeState;

public class Snake {
    private int height;
    private int weight;
    private int score = 0;

    private final int playerId;
    private SnakeState state;
    private boolean isDead = false;
    private LinkedList<Coord> points = new LinkedList<>();
    private SnakesProto.Direction headDirection;

    private Color color = Color.red;

    public Snake(LinkedList<Coord> points, SnakesProto.Direction headDirection, int height, int weight, int playerId, SnakeState state) {
        this.height = height;
        this.weight = weight;
        this.playerId = playerId;
        this.state = state;
        this.points = points;
        this.headDirection = headDirection;
    }

    public Snake(int playerId) {
        this.playerId = playerId;
    }

    public Snake(Coord center, SnakesProto.Direction headDirection, int height, int weight, int playerId, SnakeState state) {
        this.height = height;
        this.weight = weight;
        this.playerId = playerId;
        this.state = state;

        points.add(center);
        switch (headDirection) {
            case UP:
                if (center.getY() == 0) {
                    points.add(new Coord(center.getX(), height - 1));
                } else {
                    points.add(new Coord(center.getX(), center.getY() - 1));
                }
                this.headDirection = SnakesProto.Direction.DOWN;
                break;

            case DOWN:
                points.add(new Coord(center.getX(), (center.getY() + 1) % height));
                this.headDirection = SnakesProto.Direction.UP;
                break;

            case LEFT:
                if (center.getX() == 0) {
                    points.add(new Coord(weight - 1, center.getY()));
                } else {
                    points.add(new Coord(center.getX() - 1, center.getY()));
                }
                this.headDirection = SnakesProto.Direction.RIGHT;
                break;

            case RIGHT:
                points.add(new Coord((center.getX() + 1) % weight, center.getY()));
                this.headDirection = SnakesProto.Direction.LEFT;
                break;
        }
    }

    public void move(ArrayList<Coord> snacks, SnakesProto.Direction direction) {
        switch (direction) {
            case UP:
                if (this.headDirection != SnakesProto.Direction.DOWN) {
                    this.headDirection = direction;
                    moveUp(snacks);
                } else {
                    moveDown(snacks);
                }
                break;
            case DOWN:
                if (this.headDirection != SnakesProto.Direction.UP) {
                    this.headDirection = direction;
                    moveDown(snacks);
                } else {
                    moveUp(snacks);
                }

                break;
            case RIGHT:
                if (this.headDirection != SnakesProto.Direction.LEFT) {
                    this.headDirection = direction;
                    moveRight(snacks);
                } else {
                    moveLeft(snacks);

                }
                break;
            case LEFT:
                if (this.headDirection != SnakesProto.Direction.RIGHT) {
                    this.headDirection = direction;
                    moveLeft(snacks);
                } else {
                    moveRight(snacks);
                }
                break;
        }
    }

    private void moveUp(ArrayList<Coord> snacks) {
        Coord head = points.getFirst();
        if (head.getY() == 0) {
            points.addFirst(new Coord(head.getX(), height - 1));
        } else {
            points.addFirst(new Coord(head.getX(), head.getY() - 1));
        }

        if (isNotSnacksEaten(snacks)) {
            points.removeLast();
        }
    }

    private void moveDown(ArrayList<Coord> snacks) {
        Coord head = points.getFirst();
        points.addFirst(new Coord(head.getX(), (head.getY() + 1) % height));
        if (isNotSnacksEaten(snacks)) {
            points.removeLast();
        }
    }

    private void moveLeft(ArrayList<Coord> snacks) {
        Coord head = points.getFirst();
        if (head.getX() == 0) {
            points.addFirst(new Coord(weight - 1, head.getY()));
        } else {
            points.addFirst(new Coord(head.getX() - 1, head.getY()));
        }
        if (isNotSnacksEaten(snacks)) {
            points.removeLast();
        }
    }

    private void moveRight(ArrayList<Coord> snacks) {
        Coord head = points.getFirst();
        points.addFirst(new Coord((head.getX() + 1) % weight, head.getY()));
        if (isNotSnacksEaten(snacks)) {
            points.removeLast();
        }
    }

    private boolean isNotSnacksEaten(ArrayList<Coord> snacks) {
        for (Coord coord : snacks) {
            if (points.getFirst().equals(coord)) {
                increaseScore();
                return false;
            }
        }
        return true;
    }

    public void increaseScore() {
        this.score++;
    }

    public SnakesProto.Direction getHeadDirection() {
        return headDirection;
    }

    public LinkedList<Coord> getPoints() {
        return points;
    }

    public void setPoints(LinkedList<Coord> points) {
        this.points = points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Snake snake = (Snake) o;
        return playerId == snake.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    public int getPlayerId() {
        return playerId;
    }

    public SnakeState getState() {
        return state;
    }

    public void setState(SnakeState state) {
        this.state = state;
    }


    public void setScore(int score) {
        this.score = score;
    }

    public boolean isDead() {
        return isDead;
    }

    public int getScore() {
        return score;
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public Color getColor() {
        return color;
    }
}
