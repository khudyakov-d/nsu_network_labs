package ru.nsu.ccfit.khudyakov.network_lab_4.view;

import ru.nsu.ccfit.khudyakov.network_lab_4.controller.GameController;
import ru.nsu.ccfit.khudyakov.network_lab_4.model.entities.Cell;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.model_observer.ModelObserver;
import ru.nsu.ccfit.khudyakov.network_lab_4.observers.score_observer.PlayerInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

public class Board extends JComponent implements ModelObserver {
    private static final int NODE_SIZE = 10;

    private int width;
    private int height;

    private Cell[][] field;

    public Board(int width, int height, GameController controller) {

        this.width = width;
        this.height = height;


        field = new Cell[width][height];

        addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                controller.moveSnake(e);
            }
        });

        setDoubleBuffered(true);
        setPreferredSize(new Dimension(width * NODE_SIZE, height * NODE_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g) {

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width * NODE_SIZE, height * NODE_SIZE);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (Cell.SNACK == field[i][j]) {
                    g.setColor(Color.YELLOW);
                    g.fillOval(i * NODE_SIZE, j * NODE_SIZE, NODE_SIZE, NODE_SIZE);
                }

                if (Cell.SNAKE == field[i][j]) {
                    g.setColor(Color.RED);
                    g.fillRect(i * NODE_SIZE, j * NODE_SIZE, NODE_SIZE, NODE_SIZE);
                }
            }
        }

        Toolkit.getDefaultToolkit().sync();
    }

    @Override
    public void updateField(Cell[][] field) {
        this.field = field;
        SwingUtilities.invokeLater(this::repaint);
    }


}
