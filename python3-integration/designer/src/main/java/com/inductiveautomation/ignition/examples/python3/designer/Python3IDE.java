package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;

/**
 * Python 3 IDE panel for the Ignition Designer.
 *
 * <p>Provides an interactive development environment for testing Python 3 code
 * that executes on the Gateway via REST API.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Code editor with syntax highlighting</li>
 *   <li>Async execution (non-blocking UI)</li>
 *   <li>Output and error display</li>
 *   <li>Real-time diagnostics (pool stats, health)</li>
 *   <li>Execution timing</li>
 * </ul>
 */
public class Python3IDE extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3IDE.class);

    private final DesignerContext context;
    private final Python3RestClient restClient;

    // UI Components
    private JTextArea codeEditor;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private JLabel statusLabel;
    private JLabel poolStatsLabel;
    private JButton executeButton;
    private JButton clearButton;
    private JProgressBar progressBar;

    private Python3ExecutionWorker currentWorker;

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE(DesignerContext context) {
        this.context = context;
        this.restClient = new Python3RestClient(context);

        initComponents();
        layoutComponents();
        attachListeners();
        refreshDiagnostics();
    }

    /**
     * Initializes all UI components.
     */
    private void initComponents() {
        // Code editor
        codeEditor = new JTextArea(15, 80);
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeEditor.setTabSize(4);
        codeEditor.setText("# Python 3 Code Editor\n# Write your code here and click Execute\n\nresult = 2 + 2\nprint(f\"Result: {result}\")");

        // Output area
        outputArea = new JTextArea(10, 80);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(245, 245, 245));

        // Error area
        errorArea = new JTextArea(10, 80);
        errorArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        errorArea.setEditable(false);
        errorArea.setBackground(new Color(255, 245, 245));
        errorArea.setForeground(Color.RED);

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        // Pool stats label
        poolStatsLabel = new JLabel("Pool: Loading...");
        poolStatsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        // Buttons
        executeButton = new JButton("Execute (Ctrl+Enter)");
        executeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        clearButton = new JButton("Clear Output");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
    }

    /**
     * Lays out all components in the panel.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel: Code editor
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(new TitledBorder("Python 3 Code Editor"));

        JScrollPane editorScroll = new JScrollPane(codeEditor);
        editorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorPanel.add(editorScroll, BorderLayout.CENTER);

        add(editorPanel, BorderLayout.CENTER);

        // Middle panel: Toolbar
        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 0));
        toolbarPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(progressBar);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        infoPanel.add(poolStatsLabel);
        infoPanel.add(statusLabel);

        toolbarPanel.add(buttonPanel, BorderLayout.WEST);
        toolbarPanel.add(infoPanel, BorderLayout.EAST);

        add(toolbarPanel, BorderLayout.NORTH);

        // Bottom panel: Output and Error tabs
        JTabbedPane outputTabs = new JTabbedPane();

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputTabs.addTab("Output", outputScroll);

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outputTabs.addTab("Errors", errorScroll);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new TitledBorder("Execution Results"));
        bottomPanel.add(outputTabs, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Attaches event listeners to UI components.
     */
    private void attachListeners() {
        // Execute button
        executeButton.addActionListener(e -> executeCode());

        // Clear button
        clearButton.addActionListener(e -> clearOutput());

        // Keyboard shortcut: Ctrl+Enter to execute
        InputMap inputMap = codeEditor.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = codeEditor.getActionMap();

        KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
        inputMap.put(ctrlEnter, "execute");
        actionMap.put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                executeCode();
            }
        });
    }

    /**
     * Executes the Python code in the editor.
     */
    private void executeCode() {
        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            setStatus("No code to execute", Color.ORANGE);
            return;
        }

        // Cancel any running worker
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        // Clear previous output
        clearOutput();

        // Show progress
        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setStatus("Executing...", Color.BLUE);

        // Create and execute worker
        currentWorker = new Python3ExecutionWorker(
                restClient,
                code,
                new HashMap<>(),  // TODO: Support variables input
                this::handleSuccess,
                this::handleError
        );

        currentWorker.execute();
    }

    /**
     * Handles successful execution.
     *
     * @param result the execution result
     */
    private void handleSuccess(ExecutionResult result) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        if (result.isSuccess()) {
            String output = result.getResult() != null ? result.getResult() : "(no output)";
            outputArea.setText(output);

            long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
            setStatus(String.format("Execution completed in %d ms", time), new Color(0, 128, 0));

        } else {
            String error = result.getError() != null ? result.getError() : "Unknown error";
            errorArea.setText(error);
            setStatus("Execution failed", Color.RED);
        }

        // Refresh diagnostics
        refreshDiagnostics();
    }

    /**
     * Handles execution errors.
     *
     * @param error the exception
     */
    private void handleError(Exception error) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        errorArea.setText("Connection error: " + error.getMessage());
        setStatus("Execution failed", Color.RED);

        LOGGER.error("Execution error", error);
    }

    /**
     * Clears the output and error areas.
     */
    private void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }

    /**
     * Refreshes the diagnostics (pool stats, health).
     */
    private void refreshDiagnostics() {
        // Run diagnostics in background to avoid blocking UI
        SwingWorker<PoolStats, Void> diagnosticsWorker = new SwingWorker<PoolStats, Void>() {
            @Override
            protected PoolStats doInBackground() throws Exception {
                return restClient.getPoolStats();
            }

            @Override
            protected void done() {
                try {
                    PoolStats stats = get();
                    updatePoolStatsDisplay(stats);
                } catch (Exception e) {
                    poolStatsLabel.setText("Pool: Unavailable");
                    poolStatsLabel.setForeground(Color.RED);
                    LOGGER.warn("Failed to get pool stats", e);
                }
            }
        };

        diagnosticsWorker.execute();
    }

    /**
     * Updates the pool stats display.
     *
     * @param stats the pool statistics
     */
    private void updatePoolStatsDisplay(PoolStats stats) {
        String text = String.format("Pool: %d/%d healthy, %d available, %d in use",
                stats.getHealthy(), stats.getTotalSize(), stats.getAvailable(), stats.getInUse());

        poolStatsLabel.setText(text);

        if (stats.isHealthy()) {
            poolStatsLabel.setForeground(new Color(0, 128, 0));
        } else {
            poolStatsLabel.setForeground(Color.ORANGE);
        }
    }

    /**
     * Sets the status label text and color.
     *
     * @param message the status message
     * @param color the text color
     */
    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    /**
     * Gets the code editor component (for external access).
     *
     * @return the code editor
     */
    public JTextArea getCodeEditor() {
        return codeEditor;
    }

    /**
     * Gets the output area component (for external access).
     *
     * @return the output area
     */
    public JTextArea getOutputArea() {
        return outputArea;
    }

    /**
     * Gets the error area component (for external access).
     *
     * @return the error area
     */
    public JTextArea getErrorArea() {
        return errorArea;
    }
}
