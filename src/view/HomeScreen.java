package src.view;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class HomeScreen {

    private final JFrame frame;
    private final JButton playButton;
    private final JButton cancelButton;
    private final JButton roomButton;
    private final JLabel statusLabel;
    private final Runnable onRoomCreate;
    private final Consumer<String> onRoomJoin;

    private JDialog roomDialog;
    private JLabel roomIdLabel;

    public HomeScreen(Runnable onPlayClicked, Runnable onCancelClicked, Runnable onRoomCreate,
                       Consumer<String> onRoomJoin) {
        this.onRoomCreate = onRoomCreate;
        this.onRoomJoin = onRoomJoin;
        this.frame = new JFrame("♟ Kung Fu Chess ♟");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Theme.BACKGROUND);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 56, 40, 56));

        JLabel title = Theme.titleLabel("Kung Fu Chess");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = Theme.bodyLabel("Find an opponent or play in a private room");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.playButton = Theme.primaryButton("Play");
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> onPlayClicked.run());

        this.roomButton = Theme.secondaryButton("Private room");
        roomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomButton.addActionListener(e -> openRoomDialog());

        this.cancelButton = Theme.secondaryButton("Cancel");
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(e -> onCancelClicked.run());
        cancelButton.setVisible(false);

        this.statusLabel = Theme.bodyLabel(" ");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setVisible(false);

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(28));
        panel.add(playButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(roomButton);
        panel.add(Box.createVerticalStrut(16));
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(cancelButton);

        frame.add(panel);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
    }

    public void open() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void showSearching() {
        playButton.setVisible(false);
        roomButton.setVisible(false);
        statusLabel.setText("Searching for an opponent...");
        statusLabel.setVisible(true);
        cancelButton.setVisible(true);
    }

    public void hideSearching() {
        playButton.setVisible(true);
        roomButton.setVisible(true);
        statusLabel.setVisible(false);
        cancelButton.setVisible(false);
    }

    public void showCantFindMatch() {
        hideSearching();
        JOptionPane.showMessageDialog(frame, "Could not find an opponent. Try again later.",
                "No match found", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openRoomDialog() {
        roomDialog = new JDialog(frame, "Private room", true);
        roomDialog.getContentPane().setBackground(Theme.BACKGROUND);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        JButton createButton = Theme.primaryButton("Create room");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.addActionListener(e -> onRoomCreate.run());

        this.roomIdLabel = Theme.emphasisLabel(" ");
        roomIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(260, 1));
        separator.setForeground(Theme.BORDER);

        JLabel joinLabel = Theme.bodyLabel("Or join with a room ID");
        joinLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField roomIdField = Theme.textField();
        roomIdField.setMaximumSize(new Dimension(240, 48));
        roomIdField.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomIdField.setHorizontalAlignment(SwingConstants.CENTER);

        JButton joinButton = Theme.secondaryButton("Join room");
        joinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        Runnable join = () -> onRoomJoin.accept(roomIdField.getText().trim());
        joinButton.addActionListener(e -> join.run());
        roomIdField.addActionListener(e -> join.run());

        JButton closeButton = Theme.secondaryButton("Close");
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> roomDialog.dispose());

        panel.add(createButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(roomIdLabel);
        panel.add(Box.createVerticalStrut(22));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(22));
        panel.add(joinLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(roomIdField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(joinButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(closeButton);

        roomDialog.add(panel);
        roomDialog.pack();
        roomDialog.setMinimumSize(roomDialog.getSize());
        roomDialog.setLocationRelativeTo(frame);
        roomDialog.setVisible(true);
    }

    public void showRoomId(String roomId) {
        if (roomIdLabel != null) {
            roomIdLabel.setText("Room ID: " + roomId + " - waiting for opponent...");
        }
    }

    public void closeRoomDialog() {
        if (roomDialog != null) {
            roomDialog.dispose();
        }
    }

    public void close() {
        frame.dispose();
    }
}
