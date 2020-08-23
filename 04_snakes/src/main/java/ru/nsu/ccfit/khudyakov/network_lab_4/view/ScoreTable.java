package ru.nsu.ccfit.khudyakov.network_lab_4.view;

import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.PlayerInfo;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.ScoreObserver;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ScoreTable extends JPanel implements ScoreObserver {
    private static final int NODE_SIZE = 10;
    private DefaultListModel<PlayerInfo> playersInfo = new DefaultListModel<>();

    public ScoreTable(int height) {
        JList<PlayerInfo> playersInfo = new JList<>(this.playersInfo);

        playersInfo.setLayoutOrientation(JList.VERTICAL_WRAP);

        JScrollPane listScroller = new JScrollPane(playersInfo);
        listScroller.setPreferredSize(new Dimension(200, height * NODE_SIZE));

        setFocusable(false);
        this.add(listScroller);
    }

    @Override
    synchronized public void updateScoreTable(List<PlayerInfo> playersInfo) {
        SwingUtilities.invokeLater(() -> {
            this.playersInfo.clear();

            for (PlayerInfo playerInfo : playersInfo) {
                this.playersInfo.addElement(playerInfo);
            }

            validate();
        });
    }
}
