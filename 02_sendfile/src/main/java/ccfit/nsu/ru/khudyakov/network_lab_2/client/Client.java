package ccfit.nsu.ru.khudyakov.network_lab_2.client;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket clientSocket;
    private DataSender dataSender;
    private DataReceiver dataReceiver;

    public Client(String filePath, String ipAddress, int port) {
        try {
            clientSocket = new Socket(ipAddress, port);

            this.dataSender = new DataSender(filePath);
            this.dataReceiver = new DataReceiver();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startTransferring() {

        try {
            dataSender.start();

            if (dataSender.isAlive()) {
                dataSender.join();
            }

            dataReceiver.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DataReceiver extends Thread {
        private DataInputStream inputData;

        public DataReceiver() {
            try {
                this.inputData = new DataInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String msg = inputData.readUTF();

                if (msg.equals("success")) {
                    System.out.println("File was transferred successfully");
                } else if (msg.equals("failure")) {
                    System.out.println("File wasn't transferred");
                    dataSender.interrupt();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class DataSender extends Thread {
        private final int BLOCK_SIZE = 4 * 1024;
        private String filePath;
        private DataOutputStream outputData;

        public DataSender(String filePath) {
            try {
                this.filePath = filePath;
                this.outputData = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            File file = new File(filePath);

            try (FileInputStream inputFile = new FileInputStream(file)) {
                dataReceiver.start();

                outputData.writeLong(file.length());
                outputData.writeUTF(file.getName());

                byte[] buf = new byte[BLOCK_SIZE];

                int curBytesCount;

                while ( ((curBytesCount = inputFile.read(buf)) != -1) && !isInterrupted()) {
                    outputData.write(buf, 0, curBytesCount);
                }

                outputData.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
