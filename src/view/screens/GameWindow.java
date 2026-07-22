package src.view.screens;

import src.input.ClickHandler;
import src.view.Renderer;
import src.view.snapshot.GameSnapshot;
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
    private static final double ZOOM_STEP = 0.1;
    private static final int TICK_INTERVAL_MS = 16;
    private static final Dimension ZOOM_LABEL_SIZE = new Dimension(52, 20);
    private static final int PERCENT_SCALE = 100;

    private final Supplier<GameComponents> gameFactory;
    private ClickHandler clickHandler;
    private Renderer renderer;
    private DoubleFunction<GameSnapshot> snapshotSupplier;
    private LongPredicate tickSource;
    private EffectsController effects;
    private final JFrame frame;
    private final ImagePanel panel;
    private final Timer timer;
    private JButton zoomOutButton;
    private JButton zoomInButton;
    private JLabel zoomLabel;
    private boolean gameOverAnnounced = false;
    private double zoom = GameSnapshot.DEFAULT_ZOOM;

    public GameWindow(Supplier<GameComponents> gameFactory) {
        this.gameFactory = gameFactory;

        bindComponents(gameFactory.get());

        this.panel = new ImagePanel();
        this.frame = new JFrame(Theme.APP_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JScrollPane scrollPane = new JScrollPane(new CenteringPanel(panel));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Theme.BACKGROUND);
        scrollPane.getViewport().setBackground(Theme.BACKGROUND);
        frame.getContentPane().setBackground(Theme.BACKGROUND);
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
                    clickHandler.jump(x, y);
                } else {
                    clickHandler.click(x, y, result -> SwingUtilities.invokeLater(() -> {
                        if (result.isAccepted()) {
                            effects.announceMoveAccepted();
                        } else {
                            effects.announceIllegalMove();
                        }
                        repaint();
                    }));
                }
                repaint();
            }
        });

        this.timer = new Timer(TICK_INTERVAL_MS, e -> tick(TICK_INTERVAL_MS));
    }

    private void bindComponents(GameComponents components) {
        this.clickHandler = components.clickHandler();
        this.renderer = components.renderer();
        this.snapshotSupplier = components.snapshotSupplier();
        this.tickSource = components.tickSource();
        this.effects = components.effects();
        this.clickHandler.setZoom(zoom);
    }

    private JComponent buildZoomToolbar() {
        this.zoomOutButton = Theme.iconButton("−");
        this.zoomInButton = Theme.iconButton("+");
        zoomOutButton.addActionListener(e -> changeZoom(-ZOOM_STEP));
        zoomInButton.addActionListener(e -> changeZoom(ZOOM_STEP));

        this.zoomLabel = new JLabel(zoomPercentText(), SwingConstants.CENTER);
        zoomLabel.setFont(Theme.LABEL_FONT);
        zoomLabel.setForeground(Theme.TEXT_MUTED);
        zoomLabel.setPreferredSize(ZOOM_LABEL_SIZE);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Theme.BACKGROUND);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        toolbar.add(zoomOutButton);
        toolbar.add(zoomLabel);
        toolbar.add(zoomInButton);
        updateZoomControls();
        return toolbar;
    }

    private void changeZoom(double delta) {
        double newZoom = Math.max(GameSnapshot.MIN_ZOOM, Math.min(GameSnapshot.MAX_ZOOM, zoom + delta));
        if (newZoom == zoom) {
            return;
        }
        zoom = newZoom;
        clickHandler.setZoom(zoom);
        updateZoomControls();
        repaint();
    }

    private void updateZoomControls() {
        zoomLabel.setText(zoomPercentText());
        zoomOutButton.setEnabled(zoom > GameSnapshot.MIN_ZOOM);
        zoomInButton.setEnabled(zoom < GameSnapshot.MAX_ZOOM);
    }

    private String zoomPercentText() {
        return Math.round(zoom * PERCENT_SCALE) + "%";
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

    public void setStatusMessage(String message) {
        frame.setTitle(message);
    }

    public void clearStatusMessage() {
        frame.setTitle(Theme.APP_TITLE);
    }

    private void restart() {
        bindComponents(gameFactory.get());
        this.gameOverAnnounced = false;
        frame.setTitle(Theme.APP_TITLE);
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
                                  ClickHandler clickHandler, Renderer renderer, EffectsController effects) {
    }

    private static class CenteringPanel extends JPanel implements Scrollable {
        private static final int SCROLL_UNIT_INCREMENT_PX = 16;
        private static final int SCROLL_BLOCK_INCREMENT_PX = 100;

        private final JComponent child;

        CenteringPanel(JComponent child) {
            super(new GridBagLayout());
            this.child = child;
            setBackground(Theme.BACKGROUND);
            add(child);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT_PX;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_BLOCK_INCREMENT_PX;
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
            setBackground(Theme.BACKGROUND);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            Dimension newSize = new Dimension(image.getWidth(), image.getHeight());
            if (!newSize.equals(getPreferredSize())) {
                setPreferredSize(newSize);
                revalidate();
            }
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
