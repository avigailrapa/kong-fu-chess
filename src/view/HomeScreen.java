package src.view;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class HomeScreen {

    private static final Color BACKGROUND_COLOR = new Color(18, 18, 22);

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
        frame.getContentPane().setBackground(BACKGROUND_COLOR);

        this.playButton = new JButton("Play");
        playButton.addActionListener(e -> onPlayClicked.run());

        this.roomButton = new JButton("Room");
        roomButton.addActionListener(e -> openRoomDialog());

        this.cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancelClicked.run());
        cancelButton.setVisible(false);

        this.statusLabel = new JLabel("");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setVisible(false);

        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.add(playButton);
        panel.add(roomButton);
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
        roomButton.setVisible(false);
        statusLabel.setText("Searching for opponent...");
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
        roomDialog = new JDialog(frame, "Room", true);
        roomDialog.setLayout(new GridLayout(0, 1));

        roomIdLabel = new JLabel(" ");
        JTextField roomIdField = new JTextField();
        JButton createButton = new JButton("Create room");
        JButton joinButton = new JButton("Join room");
        JButton closeButton = new JButton("Close");

        createButton.addActionListener(e -> onRoomCreate.run());
        joinButton.addActionListener(e -> onRoomJoin.accept(roomIdField.getText().trim()));
        closeButton.addActionListener(e -> roomDialog.dispose());

        roomDialog.add(roomIdLabel);
        roomDialog.add(createButton);
        roomDialog.add(new JLabel("Room ID:"));
        roomDialog.add(roomIdField);
        roomDialog.add(joinButton);
        roomDialog.add(closeButton);

        roomDialog.setSize(280, 240);
        roomDialog.setLocationRelativeTo(frame);
        roomDialog.setVisible(true);
    }

    public void showRoomId(String roomId) {
        if (roomIdLabel != null) {
            roomIdLabel.setText("Room ID: " + roomId + " (waiting for opponent)");
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
