package ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer;

public interface ModelObservable {
    void registerObserver(ModelObserver observer);

    void removeObserver(ModelObserver observer);

    void fieldNotify();
}
