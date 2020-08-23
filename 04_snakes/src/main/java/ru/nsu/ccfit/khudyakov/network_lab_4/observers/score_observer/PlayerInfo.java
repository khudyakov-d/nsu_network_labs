package ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer;

import ru.nsu.ccfit.khudyakov.network_lab_4.proto.SnakesProto;

public interface PlayerInfo {
    int getScore();
    String getName();
    SnakesProto.NodeRole getNodeRole();
}
