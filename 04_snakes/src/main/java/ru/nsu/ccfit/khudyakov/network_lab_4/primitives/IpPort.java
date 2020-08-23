package ru.nsu.ccfit.khudyakov.network_lab_4.primitives;

import java.util.Objects;

public class IpPort {
    private final String ip;
    private final int port;
    private long lastTimeMsg;

    public IpPort(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public IpPort(String ip, int port, long lastTimeMsg) {
        this.ip = ip;
        this.port = port;
        this.lastTimeMsg = lastTimeMsg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpPort ipPort = (IpPort) o;
        return port == ipPort.port && Objects.equals(ip, ipPort.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "ip: " + ip + ", port:" + port;
    }

    public long getLastTimeMsg() {
        return lastTimeMsg;
    }

    public void setLastTimeMsg(long lastTimeMsg) {
        this.lastTimeMsg = lastTimeMsg;
    }
}
