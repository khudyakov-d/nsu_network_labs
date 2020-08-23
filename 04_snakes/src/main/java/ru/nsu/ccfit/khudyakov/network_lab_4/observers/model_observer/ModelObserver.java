package ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer;

import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Cell;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.PlayerInfo;

import java.util.List;

public interface ModelObserver {
    void updateField(Cell[][] field);

}
