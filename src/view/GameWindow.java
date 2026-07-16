package src.view;

import src.engine.GameEngine;
import src.input.Controller;
import src.model.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

public class GameWindow {

    private static final int SCREEN_CHROME_ALLOWANCE_PX = 80;
    private static final Color BACKGROUND_COLOR = new Color(18, 18, 22);

    private final Supplier<GameComponents> gameFactory;
    private GameEngine gameEngine;
    private Controller controller;
    private Renderer renderer;
    private final JFrame frame;
    private final ImagePanel panel;
    private final Timer timer;
    private boolean gameOverAnnounced = false;
    private boolean wasActive = true;

    public GameWindow(Supplier<GameComponents> gameFactory) {
        this.gameFactory = gameFactory;

        GameComponents initial = gameFactory.get();
        this.gameEngine = initial.engine();
        this.controller = initial.controller();
        this.renderer = initial.renderer();

        this.panel = new ImagePanel();
        this.frame = new JFrame("♟ Kung Fu Chess ♟");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        frame.getContentPane().setBackground(BACKGROUND_COLOR);
        frame.add(scrollPane);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gameOverAnnounced) {
                    restart();
                    return;
                }
                int x = e.getX() - renderer.boardOffsetX();
                int y = e.getY() - renderer.boardOffsetY();
                if (SwingUtilities.isRightMouseButton(e)) {
                    controller.jump(x, y);
                } else {
                    controller.click(x, y);
                }
                repaint();
            }
        });

        this.timer = new Timer(16, e -> tick(16));
    }

    public void open() {
        repaint();
        frame.pack();
        capFrameToScreen();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        timer.start();
    }

    private void capFrameToScreen() {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension packedSize = frame.getSize();
        int width = Math.min(packedSize.width, screenBounds.width - SCREEN_CHROME_ALLOWANCE_PX);
        int height = Math.min(packedSize.height, screenBounds.height - SCREEN_CHROME_ALLOWANCE_PX);
        frame.setSize(width, height);
    }

    public void tick(long ms) {
        gameEngine.waitMs(ms);
        boolean active = gameEngine.hasActivity();
        if (active || wasActive) {
            repaint();
        }
        wasActive = active;
    }

    private void restart() {
        GameComponents fresh = gameFactory.get();
        this.gameEngine = fresh.engine();
        this.controller = fresh.controller();
        this.renderer = fresh.renderer();
        this.gameOverAnnounced = false;
        this.wasActive = true;
        frame.setTitle("♟ Kung Fu Chess ♟");
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    private void repaint() {
        Position selected = controller.getSelectedCell().orElse(null);
        GameSnapshot snapshot = gameEngine.snapshot(selected);
        BufferedImage image = renderer.render(snapshot);
        panel.setImage(image);

        if (snapshot.isGameOver() && !gameOverAnnounced) {
            gameOverAnnounced = true;
            timer.stop();
            frame.setTitle("♟ Kung Fu Chess - " + snapshot.winner() + " wins! (click to restart)");
        }
    }

    public record GameComponents(GameEngine engine, Controller controller, Renderer renderer) {
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;

        ImagePanel() {
            setBackground(BACKGROUND_COLOR);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}