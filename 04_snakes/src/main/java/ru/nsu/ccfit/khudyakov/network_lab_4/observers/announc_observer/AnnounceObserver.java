package ru.nsu.ccfit.khudyakov.network_lab_4.observers.announc_observer;


import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.IpPort;

import java.util.ArrayList;

public interface AnnounceObserver {
    void updateGames(ArrayList<IpPort> gamesTitles);
}
