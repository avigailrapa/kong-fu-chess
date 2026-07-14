package src.view;

import src.engine.GameEngine;
import src.input.Controller;
import src.model.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class GameWindow {

    private static final int SCREEN_CHROME_ALLOWANCE_PX = 80;

    private final GameEngine gameEngine;
    private final Controller controller;
    private final Renderer renderer;
    private final JFrame frame;
    private final ImagePanel panel;
    private final Timer timer;
    private boolean gameOverAnnounced = false;
    private Double scale = null;

    public GameWindow(GameEngine gameEngine, Controller controller, Renderer renderer) {
        this.gameEngine = gameEngine;
        this.controller = controller;
        this.renderer = renderer;
        this.panel = new ImagePanel();
        this.frame = new JFrame("Kung Fu Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JScrollPane(panel));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = (int) Math.round(e.getX() / scale) - renderer.boardOffsetX();
                int y = (int) Math.round(e.getY() / scale) - renderer.boardOffsetY();
                if (SwingUtilities.isRightMouseButton(e)) {
                    controller.jump(x, y);
                } else {
                    controller.click(x, y);
                }
                repaint();
            }
        });

        this.timer = new Timer(50, e -> tick(50));
    }

    public void open() {
        repaint();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        timer.start();
    }

    public void tick(long ms) {
        gameEngine.waitMs(ms);
        repaint();
    }

    private void repaint() {
        Position selected = controller.getSelectedCell().orElse(null);
        GameSnapshot snapshot = gameEngine.snapshot(selected);
        BufferedImage image = renderer.render(snapshot);

        if (scale == null) {
            scale = computeScale(image.getWidth(), image.getHeight());
        }
        if (scale < 1.0) {
            Dimension target = new Dimension(
                    (int) Math.round(image.getWidth() * scale),
                    (int) Math.round(image.getHeight() * scale));
            image = new Img(image).resize(target, false).get();
        }
        panel.setImage(image);

        if (snapshot.isGameOver() && !gameOverAnnounced) {
            gameOverAnnounced = true;
            timer.stop();
            frame.setTitle("Kung Fu Chess - " + snapshot.winner() + " wins!");
        }
    }

    private double computeScale(int imageWidth, int imageHeight) {
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        double widthScale = (screenBounds.width - SCREEN_CHROME_ALLOWANCE_PX) / (double) imageWidth;
        double heightScale = (screenBounds.height - SCREEN_CHROME_ALLOWANCE_PX) / (double) imageHeight;
        return Math.min(1.0, Math.min(widthScale, heightScale));
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;

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
