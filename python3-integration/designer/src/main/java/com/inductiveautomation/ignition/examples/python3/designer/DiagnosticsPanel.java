package com.inductiveautomation.ignition.examples.python3.designer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel displaying real-time performance diagnostics and metrics.
 * Shows execution statistics, pool usage, and gateway impact.
 */
public class DiagnosticsPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPanel.class);

    private final JLabel poolSizeLabel;
    private final JLabel healthyLabel;
    private final JLabel availableLabel;
    private final JLabel inUseLabel;
    private final JLabel impactLevelLabel;
    private final JLabel healthScoreLabel;

    private Python3RestClient restClient;
    private Timer refreshTimer;

    /**
     * Creates a new diagnostics panel.
     */
    public DiagnosticsPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Performance Diagnostics",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setPreferredSize(new Dimension(250, 200));
        setBackground(ModernTheme.PANEL_BACKGROUND);

        // Create labels
        poolSizeLabel = createValueLabel();
        healthyLabel = createValueLabel();
        availableLabel = createValueLabel();
        inUseLabel = createValueLabel();
        impactLevelLabel = createValueLabel();
        healthScoreLabel = createValueLabel();

        // Layout
        JPanel fieldsPanel = new JPanel(new GridLayout(6, 2, 5, 3));
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        fieldsPanel.add(createKeyLabel("Pool Size:"));
        fieldsPanel.add(poolSizeLabel);

        fieldsPanel.add(createKeyLabel("Healthy:"));
        fieldsPanel.add(healthyLabel);

        fieldsPanel.add(createKeyLabel("Available:"));
        fieldsPanel.add(availableLabel);

        fieldsPanel.add(createKeyLabel("In Use:"));
        fieldsPanel.add(inUseLabel);

        fieldsPanel.add(createKeyLabel("Impact Level:"));
        fieldsPanel.add(impactLevelLabel);

        fieldsPanel.add(createKeyLabel("Health Score:"));
        fieldsPanel.add(healthScoreLabel);

        add(fieldsPanel, BorderLayout.CENTER);

        // Initially show "Not connected"
        clear();

        // Auto-refresh timer (5 seconds)
        refreshTimer = new Timer(5000, e -> refreshMetrics());
        refreshTimer.setInitialDelay(1000);  // First refresh after 1 second
    }

    /**
     * Sets the REST client for fetching metrics.
     *
     * @param restClient the REST client
     */
    public void setRestClient(Python3RestClient restClient) {
        this.restClient = restClient;

        if (restClient != null) {
            // Start auto-refresh
            refreshTimer.start();
            refreshMetrics();
        } else {
            // Stop auto-refresh and clear display
            refreshTimer.stop();
            clear();
        }
    }

    /**
     * Refreshes metrics from the Gateway.
     */
    private void refreshMetrics() {
        if (restClient == null) {
            clear();
            return;
        }

        SwingWorker<DiagnosticsData, Void> worker = new SwingWorker<DiagnosticsData, Void>() {
            @Override
            protected DiagnosticsData doInBackground() throws Exception {
                // Fetch metrics and pool stats
                PoolStats poolStats = restClient.getPoolStats();

                // Fetch gateway impact (from v1.14.0 endpoint)
                GatewayImpact impact = restClient.getGatewayImpact();

                return new DiagnosticsData(poolStats, impact);
            }

            @Override
            protected void done() {
                try {
                    DiagnosticsData data = get();
                    displayDiagnostics(data);
                } catch (Exception e) {
                    LOGGER.warn("Failed to fetch diagnostics", e);
                    clear();
                }
            }
        };

        worker.execute();
    }

    /**
     * Displays diagnostics data.
     *
     * @param data the diagnostics data
     */
    private void displayDiagnostics(DiagnosticsData data) {
        if (data == null || data.poolStats == null) {
            clear();
            return;
        }

        PoolStats stats = data.poolStats;
        GatewayImpact impact = data.impact;

        // Pool size
        poolSizeLabel.setText(String.valueOf(stats.getTotalSize()));

        // Healthy executors
        healthyLabel.setText(String.valueOf(stats.getHealthy()));
        healthyLabel.setForeground(stats.isHealthy() ? ModernTheme.SUCCESS : ModernTheme.WARNING);

        // Available executors
        availableLabel.setText(String.valueOf(stats.getAvailable()));

        // In use executors
        int inUse = stats.getInUse();
        inUseLabel.setText(String.valueOf(inUse));

        // Calculate pool usage percentage
        int poolUsage = stats.getTotalSize() > 0 ? (inUse * 100 / stats.getTotalSize()) : 0;
        inUseLabel.setForeground(getPoolUsageColor(poolUsage));

        // Impact level
        if (impact != null) {
            impactLevelLabel.setText(impact.getImpactLevel());
            impactLevelLabel.setForeground(getImpactLevelColor(impact.getImpactLevel()));

            // Health score
            healthScoreLabel.setText(String.valueOf(impact.getHealthScore()));
            healthScoreLabel.setForeground(getHealthScoreColor(impact.getHealthScore()));
        } else {
            impactLevelLabel.setText("—");
            impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
            healthScoreLabel.setText("—");
            healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    /**
     * Clears all diagnostic fields.
     */
    private void clear() {
        poolSizeLabel.setText("—");
        healthyLabel.setText("—");
        availableLabel.setText("—");
        inUseLabel.setText("—");
        impactLevelLabel.setText("—");
        healthScoreLabel.setText("—");

        // Reset colors
        healthyLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        inUseLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        impactLevelLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        healthScoreLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
    }

    /**
     * Gets color for pool usage display.
     */
    private Color getPoolUsageColor(int usage) {
        if (usage <= 60) {
            return ModernTheme.SUCCESS;
        } else if (usage <= 85) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    /**
     * Gets color for impact level display.
     */
    private Color getImpactLevelColor(String level) {
        if (level == null) {
            return ModernTheme.FOREGROUND_PRIMARY;
        }

        switch (level.toUpperCase()) {
            case "LOW":
                return ModernTheme.SUCCESS;
            case "MODERATE":
                return ModernTheme.WARNING;
            case "HIGH":
            case "CRITICAL":
                return ModernTheme.ERROR;
            default:
                return ModernTheme.FOREGROUND_PRIMARY;
        }
    }

    /**
     * Gets color for health score display.
     */
    private Color getHealthScoreColor(int score) {
        if (score >= 80) {
            return ModernTheme.SUCCESS;
        } else if (score >= 60) {
            return ModernTheme.WARNING;
        } else {
            return ModernTheme.ERROR;
        }
    }

    /**
     * Creates a key label (e.g., "Total Executions:", "Success Rate:").
     *
     * @param text the label text
     * @return configured label
     */
    private JLabel createKeyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 10));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        return label;
    }

    /**
     * Creates a value label (displays actual metric values).
     *
     * @return configured label
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 10));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }

    /**
     * Container for diagnostics data.
     */
    private static class DiagnosticsData {
        final PoolStats poolStats;
        final GatewayImpact impact;

        DiagnosticsData(PoolStats poolStats, GatewayImpact impact) {
            this.poolStats = poolStats;
            this.impact = impact;
        }
    }

    /**
     * Stops the auto-refresh timer.
     * Call this when the panel is no longer visible.
     */
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}
