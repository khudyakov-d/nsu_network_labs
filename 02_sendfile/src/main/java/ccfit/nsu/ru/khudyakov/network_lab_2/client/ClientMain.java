package ccfit.nsu.ru.khudyakov.network_lab_2.client;


public class ClientMain {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Invalid input arguments. Please enter argument in order \n"
                    + "1. name of file \n"
                    + "2. server ip \n"
                    + "3. server port \n");
        } else {
            Client client = new Client(args[0], args[1], Integer.valueOf(args[2]));
            client.startTransferring();
        }

    }
}
