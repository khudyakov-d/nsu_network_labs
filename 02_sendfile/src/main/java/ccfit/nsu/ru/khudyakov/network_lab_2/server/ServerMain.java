package ccfit.nsu.ru.khudyakov.network_lab_2.server;

public class ServerMain {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Invalid input arguments. Please enter one argument - server port \n");
        } else {
            Server server = new Server(Integer.valueOf(args[0]));
            server.startReceivingData();
        }
    }
}
