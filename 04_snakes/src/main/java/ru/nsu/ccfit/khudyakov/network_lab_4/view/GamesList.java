package ru.nsu.ccfit.khudyakov.network_lab_4.view;

import ru.nsu.ccfit.khudyakov.network_lab_4.observers.announc_observer.AnnounceObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.IpPort;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamesList extends JPanel implements AnnounceObserver {
    private DefaultListModel<IpPort> gamesTitles = new DefaultListModel<>();
    private IpPort selectedIpPort = null;

    public GamesList() {
        JList<IpPort> gamesList = new JList<>(this.gamesTitles);

        gamesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gamesList.setLayoutOrientation(JList.VERTICAL_WRAP);

        JScrollPane listScroller = new JScrollPane(gamesList);
        listScroller.setPreferredSize(new Dimension(250, 400));


        this.add(listScroller);

        gamesList.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                selectedIpPort = gamesList.getSelectedValue();
            }
        });
    }

    @Override
    synchronized public void updateGames(ArrayList<IpPort> gamesTitles) {
        SwingUtilities.invokeLater(() -> {
            this.gamesTitles.clear();

            for (IpPort gameTitle : gamesTitles) {
                this.gamesTitles.addElement(gameTitle);
            }

            validate();
        });
    }

    public IpPort getSelectedIpPort() {
        return selectedIpPort;
    }
}
