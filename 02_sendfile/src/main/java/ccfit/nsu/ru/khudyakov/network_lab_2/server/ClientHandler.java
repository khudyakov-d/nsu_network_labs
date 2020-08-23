package ccfit.nsu.ru.khudyakov.network_lab_2.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final int BLOCK_SIZE = 4 * 1024;
    private final int RECEIVE_TIMEOUT = 3000;

    private Socket clientSocket;

    private DataInputStream inputData;
    private DataOutputStream outputData;

    public ClientHandler(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;

            inputData = new DataInputStream(clientSocket.getInputStream());
            outputData = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFile(String fileName) throws IOException {
        File filePath = new File("uploads");

        fileName = fileName.replace("\\", "/");
        String trueFileName = fileName.substring(fileName.lastIndexOf("/") + 1);

        filePath.mkdir();
        File file = new File(filePath + File.separator + trueFileName);

        return file;
    }

    private void printSpeed(long curTime, long totalBytesByInterval) {
        System.out.println("Current speed connection - " + (totalBytesByInterval / (curTime / (double) 1000)));
    }

    private void printAverageSpeed(long fullTime, long totalBytes) {
        System.out.println("Average speed connection - " + (totalBytes / (fullTime / (double) 1000)));
    }

    private void sendErrorMessage() {
        try {
            outputData.writeUTF("failure");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void run() {
        try(Socket socket = clientSocket) {
            long fileSize = inputData.readLong();
            String fileName = inputData.readUTF();
            byte[] buf = new byte[BLOCK_SIZE];

            File file = createFile(fileName);

            try (FileOutputStream outputFile = new FileOutputStream(file)) {

                int curBytesCount;
                long timeBorder = System.currentTimeMillis(), curTime, totalBytesCount = 0, totalBytesByInterval = 0;
                long startBorder = System.currentTimeMillis();

                while ((curBytesCount = inputData.read(buf)) != -1) {

                    if ((curTime = System.currentTimeMillis() - timeBorder) > RECEIVE_TIMEOUT) {
                        printAverageSpeed(System.currentTimeMillis() - startBorder, totalBytesCount);
                        printSpeed(curTime, totalBytesByInterval);

                        timeBorder = System.currentTimeMillis();
                        totalBytesByInterval = 0;
                    }

                    totalBytesByInterval += curBytesCount;
                    totalBytesCount += curBytesCount;

                    outputFile.write(buf, 0, curBytesCount);

                    if (totalBytesCount == fileSize) {
                        break;
                    }

                    if (totalBytesCount > fileSize) {
                        throw new WrongDataException("Error, the wrong number of bytes was transferred");
                    }
                }

                curTime = System.currentTimeMillis() - timeBorder;

                if (curTime == 0) {
                    curTime = 1;
                }

                printAverageSpeed(System.currentTimeMillis() - startBorder, totalBytesCount);
                printSpeed(curTime, totalBytesByInterval);

                outputFile.flush();
            }
            outputData.writeUTF("success");

        } catch (WrongDataException e) {
            System.out.println(e.getMessage());
            sendErrorMessage();
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMessage();
        }
    }

    private class WrongDataException extends Exception {
        public WrongDataException(String msg) {
            super(msg);
        }
    }
}
