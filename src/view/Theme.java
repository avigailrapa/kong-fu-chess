package src.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class Theme {

    static final Color BACKGROUND = new Color(18, 18, 22);
    static final Color PANEL_BACKGROUND = new Color(26, 26, 32);
    static final Color FIELD_BACKGROUND = new Color(40, 40, 48);
    static final Color BORDER = new Color(58, 58, 68);
    static final Color TEXT = new Color(235, 235, 240);
    static final Color TEXT_MUTED = new Color(150, 150, 162);
    static final Color ACCENT = new Color(212, 160, 23);
    static final Color ACCENT_HOVER = new Color(232, 180, 43);
    static final Color ERROR = new Color(224, 100, 100);

    static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 32);
    static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 16);
    static final Font FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 18);
    static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 17);

    private Theme() {
    }

    static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.BLACK);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(14, 34, 14, 34));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ACCENT);
            }
        });
        return button;
    }

    static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(TEXT);
        button.setBackground(PANEL_BACKGROUND);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(13, 32, 13, 32)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(BORDER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PANEL_BACKGROUND);
            }
        });
        return button;
    }

    static JButton iconButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(TEXT);
        button.setBackground(PANEL_BACKGROUND);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        button.setPreferredSize(new Dimension(34, 34));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(BORDER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PANEL_BACKGROUND);
            }
        });
        return button;
    }

    static JTextField textField() {
        JTextField field = new JTextField();
        styleField(field);
        return field;
    }

    static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleField(field);
        return field;
    }

    private static void styleField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setForeground(TEXT);
        field.setBackground(FIELD_BACKGROUND);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    }

    static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    static JLabel bodyLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    static JLabel emphasisLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(FIELD_FONT);
        label.setForeground(TEXT);
        return label;
    }
}
