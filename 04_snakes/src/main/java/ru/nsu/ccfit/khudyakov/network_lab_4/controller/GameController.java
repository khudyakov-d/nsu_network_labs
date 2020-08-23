package ru.nsu.ccfit.khudyakov.network_lab_4.controller;

import ru.nsu.ccfit.khudyakov.network_lab_4.model.GameModel;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.IpPort;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer.ModelObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.ScoreObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

import java.awt.event.KeyEvent;

import static ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto.GameMessage.AnnouncementMsg;

public class GameController {
    private GameModel model;

    public void moveSnake(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_RIGHT:
                model.setSnakeDirection(SnakesProto.Direction.RIGHT);
                break;
            case KeyEvent.VK_LEFT:
                model.setSnakeDirection(SnakesProto.Direction.LEFT);
                break;
            case KeyEvent.VK_DOWN:
                model.setSnakeDirection(SnakesProto.Direction.DOWN);
                break;
            case KeyEvent.VK_UP:
                model.setSnakeDirection(SnakesProto.Direction.UP);
                break;
        }
    }

    public void startMaster(ModelObserver modelObserver, ScoreObserver scoreObserver) {
        model = new GameModel();
        model.registerObserver(modelObserver);
        model.registerObserver(scoreObserver);
    }

    public void startNormal(ModelObserver modelObserver, ScoreObserver scoreObserver, IpPort ipPort, AnnouncementMsg gameConfig) {
        model = new GameModel(ipPort, gameConfig);
        model.registerObserver(modelObserver);
        model.registerObserver(scoreObserver);
    }
}
