package ccfit.nsu.ru.khudyakov.lab1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CopyFinder {
    private final int PORT = 4446;
    private final int RECEIVE_TIMEOUT = 1000;
    private final int TABLE_TIMEOUT = 5000;

    private String multicastIp;
    private Map<String, Long> currentIpTable = new HashMap<>();

    public CopyFinder(String multicastIp) {
        this.multicastIp = multicastIp;
    }

    public void runFinder() {

        try (MulticastSocket socket = new MulticastSocket(PORT)){

            InetAddress group = InetAddress.getByName(multicastIp);
            socket.joinGroup(group);

            socket.setSoTimeout(RECEIVE_TIMEOUT);

            while (true) {
                socket.send(new DatagramPacket("hello".getBytes(), "hello".getBytes().length, group, PORT));

                long end = System.currentTimeMillis() + RECEIVE_TIMEOUT;

                while (System.currentTimeMillis() < end) {

                    byte[] buf = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);

                    try {
                        socket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        break;
                    }

                    checkIpState(packet.getAddress().getHostAddress());
                }

                checkCurrentIpTable();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void checkCurrentIpTable() {

        for (Iterator<Map.Entry<String, Long>> iterator = currentIpTable.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() - System.currentTimeMillis() > TABLE_TIMEOUT) {
                iterator.remove();
                printCurrentIpTable();
            }
        }
    }

    private void checkIpState(String ip) {

        if (!currentIpTable.containsKey(ip)) {
            currentIpTable.put(ip, System.currentTimeMillis());
            printCurrentIpTable();
        } else {
            currentIpTable.put(ip, System.currentTimeMillis());
        }
    }

    private void printCurrentIpTable() {
        for (Map.Entry<String, Long> entry : currentIpTable.entrySet()) {
            System.out.println(entry.getKey() + "\n");
        }
        System.out.println("\n");
    }

}
