package ru.nsu.ccfit.khudyakov.network_lab_4.observers.announc_observer;

public interface AnnounceObservable {
    void registerObserver(AnnounceObserver observer);

    void removeObserver(AnnounceObserver observer);

    void announcesNotify();
}
