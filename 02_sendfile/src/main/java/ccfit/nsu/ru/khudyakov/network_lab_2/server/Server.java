package ccfit.nsu.ru.khudyakov.network_lab_2.server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private ServerSocket serverSocket;

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startReceivingData() {
        try {
            while (true) {
                ClientHandler clientHandler = new ClientHandler(serverSocket.accept());
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
