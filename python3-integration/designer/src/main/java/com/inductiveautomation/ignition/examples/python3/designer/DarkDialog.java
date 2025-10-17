package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom dark-themed dialog builder for Python 3 IDE.
 *
 * Provides properly dark-themed alternatives to JOptionPane that actually work.
 * UIManager-based theming doesn't work reliably because Ignition overrides it.
 *
 * v2.0.11: Created to fix persistent light-themed dialog issues
 */
public class DarkDialog {

    private static final Color BACKGROUND = new Color(43, 43, 43);
    private static final Color BACKGROUND_DARKER = new Color(30, 30, 30);
    private static final Color FOREGROUND = new Color(224, 224, 224);
    private static final Color BUTTON_BG = new Color(60, 63, 65);
    private static final Color BORDER_COLOR = new Color(60, 63, 65);

    /**
     * Shows a dark-themed message dialog.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     */
    public static void showMessage(Component parent, String message, String title) {
        showMessage(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a dark-themed message dialog with specific message type.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @param messageType JOptionPane message type (INFO, WARNING, ERROR)
     */
    public static void showMessage(Component parent, String message, String title, int messageType) {
        JDialog dialog = createBaseDialog(parent, title);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setForeground(FOREGROUND);
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        // OK button
        JButton okButton = createDarkButton("OK");
        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND);
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Shows a dark-themed confirmation dialog (Yes/No).
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @return true if Yes was clicked, false otherwise
     */
    public static boolean showConfirm(Component parent, String message, String title) {
        JDialog dialog = createBaseDialog(parent, title);
        final boolean[] result = {false};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setForeground(FOREGROUND);
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        // Buttons
        JButton yesButton = createDarkButton("Yes");
        yesButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });

        JButton noButton = createDarkButton("No");
        noButton.addActionListener(e -> {
            result[0] = false;
            dialog.dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND);
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a dark-themed input dialog.
     *
     * @param parent parent component
     * @param message message to display
     * @param title dialog title
     * @param initialValue initial text value
     * @return user input, or null if cancelled
     */
    public static String showInput(Component parent, String message, String title, String initialValue) {
        JDialog dialog = createBaseDialog(parent, title);
        final String[] result = {null};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Message label
        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(FOREGROUND);
        messageLabel.setFont(ModernTheme.FONT_REGULAR);
        contentPanel.add(messageLabel, BorderLayout.NORTH);

        // Input field
        JTextField inputField = createDarkTextField(initialValue, 30);
        contentPanel.add(inputField, BorderLayout.CENTER);

        // Buttons
        JButton okButton = createDarkButton("OK");
        okButton.addActionListener(e -> {
            result[0] = inputField.getText();
            dialog.dispose();
        });

        JButton cancelButton = createDarkButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Shows a dark-themed multi-field input dialog (for Save Script).
     *
     * @param parent parent component
     * @param title dialog title
     * @param fields map of field names to initial values
     * @return map of field names to user input, or null if cancelled
     */
    public static Map<String, String> showMultiInput(Component parent, String title, Map<String, String> fields) {
        JDialog dialog = createBaseDialog(parent, title);
        final Map<String, String> result = new HashMap<>();
        final boolean[] cancelled = {true};

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create input fields
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(BACKGROUND);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Map<String, JTextField> fieldComponents = new HashMap<>();
        int row = 0;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            JLabel label = new JLabel(entry.getKey() + ":");
            label.setForeground(FOREGROUND);
            label.setFont(ModernTheme.FONT_REGULAR);
            fieldsPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            JTextField textField = createDarkTextField(entry.getValue(), 30);
            fieldComponents.put(entry.getKey(), textField);
            fieldsPanel.add(textField, gbc);

            row++;
        }

        contentPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Buttons
        JButton okButton = createDarkButton("OK");
        okButton.addActionListener(e -> {
            for (Map.Entry<String, JTextField> entry : fieldComponents.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getText());
            }
            cancelled[0] = false;
            dialog.dispose();
        });

        JButton cancelButton = createDarkButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(BACKGROUND);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        return cancelled[0] ? null : result;
    }

    // === Private Helper Methods ===

    private static JDialog createBaseDialog(Component parent, String title) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(BACKGROUND);
        return dialog;
    }

    private static JButton createDarkButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_BG);
        button.setForeground(FOREGROUND);
        button.setFont(ModernTheme.FONT_REGULAR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(75, 80, 85));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_BG);
            }
        });

        return button;
    }

    private static JTextField createDarkTextField(String initialValue, int columns) {
        JTextField textField = new JTextField(initialValue != null ? initialValue : "", columns);
        textField.setBackground(BACKGROUND_DARKER);
        textField.setForeground(FOREGROUND);
        textField.setCaretColor(FOREGROUND);
        textField.setFont(ModernTheme.FONT_REGULAR);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        return textField;
    }
}
