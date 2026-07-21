package src.view;

import javax.swing.*;
import java.awt.*;

public class HomeScreen {

    private static final Color BACKGROUND_COLOR = new Color(18, 18, 22);

    private final JFrame frame;
    private final JButton playButton;
    private final JButton cancelButton;
    private final JLabel statusLabel;

    public HomeScreen(Runnable onPlayClicked, Runnable onCancelClicked) {
        this.frame = new JFrame("♟ Kung Fu Chess ♟");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BACKGROUND_COLOR);

        this.playButton = new JButton("Play");
        playButton.addActionListener(e -> onPlayClicked.run());

        this.cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancelClicked.run());
        cancelButton.setVisible(false);

        this.statusLabel = new JLabel("");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setVisible(false);

        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.add(playButton);
        panel.add(statusLabel);
        panel.add(cancelButton);

        frame.add(panel);
        frame.setSize(320, 160);
    }

    public void open() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void showSearching() {
        playButton.setVisible(false);
        statusLabel.setText("Searching for opponent...");
        statusLabel.setVisible(true);
        cancelButton.setVisible(true);
    }

    public void hideSearching() {
        playButton.setVisible(true);
        statusLabel.setVisible(false);
        cancelButton.setVisible(false);
    }

    public void showCantFindMatch() {
        hideSearching();
        JOptionPane.showMessageDialog(frame, "Could not find an opponent. Try again later.",
                "No match found", JOptionPane.INFORMATION_MESSAGE);
    }

    public void close() {
        frame.dispose();
    }
}
