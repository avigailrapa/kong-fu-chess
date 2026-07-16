package src.view;

import src.input.Controller;

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
    private Controller controller;
    private Renderer renderer;
    private Supplier<GameSnapshot> snapshotSupplier;
    private GameLoop gameLoop;
    private final JFrame frame;
    private final ImagePanel panel;
    private final Timer timer;
    private boolean gameOverAnnounced = false;

    public GameWindow(Supplier<GameComponents> gameFactory) {
        this.gameFactory = gameFactory;

        bindComponents(gameFactory.get());

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

    private void bindComponents(GameComponents components) {
        this.controller = components.controller();
        this.renderer = components.renderer();
        this.snapshotSupplier = components.snapshotSupplier();
        this.gameLoop = components.gameLoop();
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
        if (gameLoop.tick(ms)) {
            repaint();
        }
    }

    private void restart() {
        bindComponents(gameFactory.get());
        this.gameOverAnnounced = false;
        frame.setTitle("♟ Kung Fu Chess ♟");
        if (!timer.isRunning()) {
            timer.start();
        }
        repaint();
    }

    private void repaint() {
        GameSnapshot snapshot = snapshotSupplier.get();
        BufferedImage image = renderer.render(snapshot);
        panel.setImage(image);

        if (snapshot.isGameOver() && !gameOverAnnounced) {
            gameOverAnnounced = true;
            timer.stop();
            frame.setTitle("♟ Kung Fu Chess - " + snapshot.winner() + " wins! (click to restart)");
        }
    }

    public record GameComponents(GameLoop gameLoop, Supplier<GameSnapshot> snapshotSupplier,
                                  Controller controller, Renderer renderer) {
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
