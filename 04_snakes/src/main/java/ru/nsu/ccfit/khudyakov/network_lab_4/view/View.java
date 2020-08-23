package ru.nsu.ccfit.khudyakov.network_lab_4.view;


import ru.nsu.ccfit.khudyakov.network_lab_4.config.ConfigParser;
import ru.nsu.ccfit.khudyakov.network_lab_4.controller.GameController;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.IpPort;
import ru.nsu.ccfit.khudyakov.network_lab_4.service.AnnounceListener;

import javax.swing.*;
import java.awt.*;

public class View extends JFrame implements Runnable {
    private Board board;
    private GameController gameController;
    private AnnounceListener announceListener = new AnnounceListener();


    public View(GameController gameController) {
        super("Snake");

        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BorderLayout());

        GamesList gamesList = new GamesList();
        startPanel.add(gamesList, BorderLayout.CENTER);

        Thread announceThread = new Thread(announceListener);
        announceThread.start();

        JPanel buttonPanel = new JPanel();

        JButton createButton = new JButton("Create");
        JButton joinButton = new JButton("Join");

        createButton.setPreferredSize(new Dimension(100, 30));
        joinButton.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);

        startPanel.add(buttonPanel, BorderLayout.SOUTH);
        this.add(startPanel);


        createButton.addActionListener(e -> {
            announceThread.interrupt();

            this.remove(startPanel);
            this.revalidate();
            this.repaint();
            this.pack();

            board = new Board(ConfigParser.getWidth(), ConfigParser.getHeight(), gameController);
            this.add(board, BorderLayout.WEST);


            ScoreTable scoreTable = new ScoreTable(ConfigParser.getWidth());
            this.add(scoreTable, BorderLayout.EAST);
            this.pack();

            board.requestFocus(true);

            gameController.startMaster(board, scoreTable);
        });

        joinButton.addActionListener(e -> {
            announceThread.interrupt();
            setLayout(new BorderLayout());

            IpPort ipPort = gamesList.getSelectedIpPort();
            if (null != ipPort) {

                this.remove(startPanel);
                this.revalidate();
                this.repaint();
                this.pack();
                int width = announceListener.getGames().get(ipPort).getConfig().getWidth();
                int height = announceListener.getGames().get(ipPort).getConfig().getHeight();

                board = new Board(width, height, gameController);
                this.add(board, BorderLayout.WEST);


                ScoreTable scoreTable = new ScoreTable(ConfigParser.getHeight());
                this.add(scoreTable, BorderLayout.EAST);

                board.requestFocus(true);


                this.pack();
                gameController.startNormal(board, scoreTable, ipPort, announceListener.getGames().get(ipPort));
            }
        });

        announceListener.registerObserver(gamesList);
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }


    @Override
    public void run() {

    }
}
