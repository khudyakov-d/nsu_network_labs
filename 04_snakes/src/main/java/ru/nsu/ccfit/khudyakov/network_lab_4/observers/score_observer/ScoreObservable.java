package ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer;

import ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer.ModelObserver;

public interface ScoreObservable {
    void registerObserver(ScoreObserver observer);

    void removeObserver(ScoreObserver observer);

    void scoreNotify();
}
