package src.view.screens;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.BiConsumer;

public class LoginScreen {

    private final JFrame frame;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JLabel statusLabel;

    public LoginScreen(BiConsumer<String, String> onLoginClicked) {
        this.frame = new JFrame("♟ Kung Fu Chess ♟");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(Theme.BACKGROUND);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 56, 40, 56));

        JLabel title = Theme.titleLabel("Kung Fu Chess");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = Theme.bodyLabel("New usernames are registered automatically");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel usernameLabel = Theme.bodyLabel("Username");
        usernameLabel.setHorizontalAlignment(SwingConstants.LEFT);
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.usernameField = Theme.textField();
        usernameField.setMaximumSize(new Dimension(320, 48));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passwordLabel = Theme.bodyLabel("Password");
        passwordLabel.setHorizontalAlignment(SwingConstants.LEFT);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.passwordField = Theme.passwordField();
        passwordField.setMaximumSize(new Dimension(320, 48));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        this.statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Theme.ERROR);
        statusLabel.setFont(Theme.LABEL_FONT);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.loginButton = Theme.primaryButton("Login / Register");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        ActionListener submit = e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isBlank() || password.isBlank()) {
                showError("Enter a username and password");
                return;
            }
            onLoginClicked.accept(username, password);
        };
        loginButton.addActionListener(submit);
        passwordField.addActionListener(submit);

        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(26));
        panel.add(usernameLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(16));
        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(22));
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(statusLabel);

        frame.add(panel);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
    }

    public void open() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        usernameField.requestFocusInWindow();
    }

    public void showError(String message) {
        statusLabel.setText(message);
        setBusy(false);
    }

    public void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        loginButton.setText(busy ? "Connecting..." : "Login");
        if (busy) {
            statusLabel.setText(" ");
        }
    }

    public void close() {
        frame.dispose();
    }
}
