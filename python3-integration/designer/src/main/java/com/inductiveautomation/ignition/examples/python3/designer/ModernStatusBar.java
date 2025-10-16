package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import java.awt.*;

/**
 * Modern status bar component for displaying IDE information.
 * Shows cursor position, Python version, pool status, and other metadata.
 */
public class ModernStatusBar extends JPanel {
    private final JLabel statusLabel;
    private final JLabel cursorPositionLabel;
    private final JLabel pythonVersionLabel;
    private final JLabel poolStatsLabel;
    private final JLabel connectionLabel;

    /**
     * Creates a new modern status bar.
     */
    public ModernStatusBar() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARKER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_DEFAULT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Left panel: Status message
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        statusLabel = createStatusLabel();
        statusLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        leftPanel.add(statusLabel);

        add(leftPanel, BorderLayout.WEST);

        // Right panel: Metadata
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        connectionLabel = createStatusLabel();
        connectionLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

        poolStatsLabel = createStatusLabel();
        poolStatsLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

        pythonVersionLabel = createStatusLabel();
        pythonVersionLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

        cursorPositionLabel = createStatusLabel();
        cursorPositionLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);

        rightPanel.add(createSeparator());
        rightPanel.add(connectionLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(poolStatsLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(pythonVersionLabel);
        rightPanel.add(createSeparator());
        rightPanel.add(cursorPositionLabel);

        add(rightPanel, BorderLayout.EAST);

        // Set default text
        setStatus("Ready", MessageType.INFO);
        setConnection("Not Connected");
        setPoolStats("Pool: --");
        setPythonVersion("Python: --");
        setCursorPosition(1, 1);

        setPreferredSize(new Dimension(0, 28));
    }

    /**
     * Creates a label with consistent styling.
     */
    private JLabel createStatusLabel() {
        JLabel label = new JLabel();
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        return label;
    }

    /**
     * Creates a visual separator.
     */
    private JLabel createSeparator() {
        JLabel separator = new JLabel("|");
        separator.setForeground(ModernTheme.BORDER_DEFAULT);
        separator.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        return separator;
    }

    // === Status Updates ===

    /**
     * Sets the main status message.
     *
     * @param message the status message
     * @param type    the message type
     */
    public void setStatus(String message, MessageType type) {
        statusLabel.setText(message);
        statusLabel.setForeground(type.getColor());
    }

    /**
     * Sets the connection status.
     *
     * @param status the connection status
     */
    public void setConnection(String status) {
        connectionLabel.setText("⬤ " + status);
    }

    /**
     * Sets the connection status with a color indicator.
     *
     * @param status the connection status
     * @param color  the indicator color
     */
    public void setConnection(String status, Color color) {
        connectionLabel.setText("⬤ " + status);
        connectionLabel.setForeground(color);
    }

    /**
     * Sets the pool statistics display.
     *
     * @param stats the pool statistics text
     */
    public void setPoolStats(String stats) {
        poolStatsLabel.setText(stats);
    }

    /**
     * Sets the pool statistics with a color.
     *
     * @param stats the pool statistics text
     * @param color the text color
     */
    public void setPoolStats(String stats, Color color) {
        poolStatsLabel.setText(stats);
        poolStatsLabel.setForeground(color);
    }

    /**
     * Sets the Python version display.
     *
     * @param version the Python version
     */
    public void setPythonVersion(String version) {
        pythonVersionLabel.setText(version);
    }

    /**
     * Sets the cursor position display.
     *
     * @param line   the line number (1-based)
     * @param column the column number (1-based)
     */
    public void setCursorPosition(int line, int column) {
        cursorPositionLabel.setText(String.format("Ln %d, Col %d", line, column));
    }

    /**
     * Updates pool stats from PoolStats object.
     *
     * @param stats the pool statistics
     */
    public void updatePoolStats(PoolStats stats) {
        if (stats == null) {
            setPoolStats("Pool: Unavailable", ModernTheme.ERROR);
            return;
        }

        String text = String.format("Pool: %d/%d (%d avail)",
                stats.getHealthy(), stats.getTotalSize(), stats.getAvailable());

        Color color = stats.isHealthy() ? ModernTheme.SUCCESS : ModernTheme.WARNING;
        setPoolStats(text, color);
    }

    // === Message Types ===

    /**
     * Enum for status message types with associated colors.
     */
    public enum MessageType {
        SUCCESS(ModernTheme.SUCCESS),
        ERROR(ModernTheme.ERROR),
        WARNING(ModernTheme.WARNING),
        INFO(ModernTheme.INFO),
        NEUTRAL(ModernTheme.FOREGROUND_PRIMARY);

        private final Color color;

        MessageType(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }
}
