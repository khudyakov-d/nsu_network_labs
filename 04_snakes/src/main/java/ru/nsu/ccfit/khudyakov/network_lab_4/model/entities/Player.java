package ru.nsu.ccfit.khudyakov.network_lab_4.model.entities;

import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.PlayerInfo;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player implements PlayerInfo {
    private int id;
    private String ip;
    private int port;
    private String name;
    private SnakesProto.NodeRole nodeRole;
    private int score;
    private long lastTime;

    private int lastSteerMsgSeq = 0;
    private SnakesProto.Direction lastSteer;

    private Lock lock = new ReentrantLock();

    public Player(int id, String ip, int port, long lastTime, String name, SnakesProto.NodeRole nodeRole, int score) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.lastTime = lastTime;
        this.name = name;
        this.nodeRole = nodeRole;
        this.score = score;
    }

    public Player(String ip, int port) {
        this.ip = ip;
        this.port = port;
        nodeRole = SnakesProto.NodeRole.NORMAL;
        score = 0;
    }

    public SnakesProto.Direction getLastSteer() {
        return lastSteer;
    }

    public int getLastSteerMsgSeq() {
        return lastSteerMsgSeq;
    }

    public void setLastSteerMsgSeq(int lastSteerMsgSeq) {
        this.lastSteerMsgSeq = lastSteerMsgSeq;
    }

    public void setScore(int score) {
        this.score = score;
    }



    public void setLastSteer(SnakesProto.Direction lastSteer) {
        this.lastSteer = lastSteer;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public SnakesProto.NodeRole getNodeRole() {
        return nodeRole;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public void setNodeRole(SnakesProto.NodeRole nodeRole) {
        this.nodeRole = nodeRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return port == player.port && Objects.equals(ip, player.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    public Lock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return name + " "  + score + " " + nodeRole;
    }
}
