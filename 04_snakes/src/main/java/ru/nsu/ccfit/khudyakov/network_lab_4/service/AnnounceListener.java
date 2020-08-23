package ru.nsu.ccfit.khudyakov.network_lab_4.service;


import ru.nsu.ccfit.khudyakov.network_lab_4.observers.announc_observer.AnnounceObservable;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.announc_observer.AnnounceObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.IpPort;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameMessage;

public class AnnounceListener implements AnnounceObservable, Runnable {
    private final Map<IpPort, GameMessage.AnnouncementMsg> games = new ConcurrentHashMap<>();

    private ArrayList<AnnounceObserver> observers = new ArrayList<>();

    @Override
    public void run() {
        int PORT = 9192;

        try (MulticastSocket socket = new MulticastSocket(PORT)) {
            String multicastIp = "239.192.0.4";
            InetAddress group = InetAddress.getByName(multicastIp);
            socket.joinGroup(group);
            socket.setSoTimeout(1000);


            while (!Thread.currentThread().isInterrupted()) {
                int BUF_SIZE = 2048;
                byte[] buf = new byte[BUF_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException ignored) {
                    checkMsgsTimeout();
                    continue;
                }

                GameMessage gameMessage = GameMessage.parseFrom(ByteBuffer.wrap(packet.getData(), 0, packet.getLength()));

                if (gameMessage.getTypeCase() == GameMessage.TypeCase.ANNOUNCEMENT) {
                    GameMessage.AnnouncementMsg announce = gameMessage.getAnnouncement();

                    if (games.containsKey(new IpPort(packet.getAddress().getHostAddress(), packet.getPort()))){
                        games.put(new IpPort(packet.getAddress().getHostAddress(), packet.getPort(), System.currentTimeMillis()), announce);
                    } else {
                        games.put(new IpPort(packet.getAddress().getHostAddress(), packet.getPort(), System.currentTimeMillis()), announce);
                        announcesNotify();
                    }

                } else {
                    System.out.println("Not correct type of message");
                }

                checkMsgsTimeout();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void checkMsgsTimeout() {
        for (IpPort ipPort : games.keySet()) {
            long TIMEOUT = 10000;
            if (System.currentTimeMillis() - ipPort.getLastTimeMsg() > TIMEOUT) {
                games.remove(ipPort);
                announcesNotify();
            }
        }
    }

    @Override
    public void registerObserver(AnnounceObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(AnnounceObserver observer) {
        observers.remove(observer);

    }

    @Override
    public void announcesNotify() {
        synchronized (games) {
            ArrayList<IpPort> gamesTitles = new ArrayList<>(games.keySet());
            for (AnnounceObserver observer : observers) {
                observer.updateGames(gamesTitles);
            }
        }
    }


    public Map<IpPort, GameMessage.AnnouncementMsg> getGames() {
        synchronized (games) {
            return games;
        }
    }
}
