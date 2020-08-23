package ccfit.nsu.ru.khudyakov.lab1;

public class Main {
    public static void main(String[] args) {

        if(args.length == 1) {
            CopyFinder copyFinder = new CopyFinder(args[0]);
            copyFinder.runFinder();

        } else {
            System.out.println("Invalid input value. Please enter 1 argument - multicast ip address");
        }
    }
}
