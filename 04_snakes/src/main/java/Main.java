import ru.nsu.ccfit.khudyakov.network_lab_4.controller.GameController;
import ru.nsu.ccfit.khudyakov.network_lab_4.view.View;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {

        GameController gameController = new GameController();
        SwingUtilities.invokeAndWait(new View(gameController));
    }

}
