package src.view;

import src.input.Controller;
import src.view.sound.EffectsController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.DoubleFunction;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

public class GameWindow {

    private static final int SCREEN_CHROME_ALLOWANCE_PX = 80;
    private static final Color BACKGROUND_COLOR = new Color(18, 18, 22);
    private static final double ZOOM_STEP = 0.1;

    private final Supplier<GameComponents> gameFactory;
    private Controller controller;
    private Renderer renderer;
    private DoubleFunction<GameSnapshot> snapshotSupplier;
    private LongPredicate tickSource;
    private EffectsController effects;
    private final JFrame frame;
    private final ImagePanel panel;
    private final Timer timer;
    private boolean gameOverAnnounced = false;
    private double zoom = GameSnapshot.DEFAULT_ZOOM;

    public GameWindow(Supplier<GameComponents> gameFactory) {
        this.gameFactory = gameFactory;

        bindComponents(gameFactory.get());

        this.panel = new ImagePanel();
        this.frame = new JFrame("♟ Kung Fu Chess ♟");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JScrollPane scrollPane = new JScrollPane(new CenteringPanel(panel));
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        frame.getContentPane().setBackground(BACKGROUND_COLOR);
        frame.add(buildZoomToolbar(), BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

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
        this.tickSource = components.tickSource();
        this.effects = components.effects();
        this.controller.setZoom(zoom);
    }

    private JComponent buildZoomToolbar() {
        JButton zoomOutButton = new JButton("-");
        JButton zoomInButton = new JButton("+");
        zoomOutButton.addActionListener(e -> changeZoom(-ZOOM_STEP));
        zoomInButton.addActionListener(e -> changeZoom(ZOOM_STEP));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(BACKGROUND_COLOR);
        toolbar.add(zoomOutButton);
        toolbar.add(zoomInButton);
        return toolbar;
    }

    private void changeZoom(double delta) {
        double newZoom = Math.max(GameSnapshot.MIN_ZOOM, Math.min(GameSnapshot.MAX_ZOOM, zoom + delta));
        if (newZoom == zoom) {
            return;
        }
        zoom = newZoom;
        controller.setZoom(zoom);
        repaint();
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
        boolean bannerWasActive = effects.activeBanner().isPresent();
        effects.tick(ms);
        if (tickSource.test(ms) || bannerWasActive) {
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
        GameSnapshot snapshot = snapshotSupplier.apply(zoom);
        BufferedImage image = renderer.render(snapshot);
        effects.activeBanner().ifPresent(text -> renderer.drawBanner(image, text));
        panel.setImage(image);

        if (snapshot.gameOver() && !gameOverAnnounced) {
            gameOverAnnounced = true;
            timer.stop();
            frame.setTitle("♟ Kung Fu Chess - " + snapshot.winner() + " wins! (click to restart)");
        }
    }

    public record GameComponents(LongPredicate tickSource, DoubleFunction<GameSnapshot> snapshotSupplier,
                                  Controller controller, Renderer renderer, EffectsController effects) {
    }

    private static class CenteringPanel extends JPanel implements Scrollable {
        private final JComponent child;

        CenteringPanel(JComponent child) {
            super(new GridBagLayout());
            this.child = child;
            setBackground(BACKGROUND_COLOR);
            add(child);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport && getParent().getWidth() > child.getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > child.getPreferredSize().height;
        }
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
