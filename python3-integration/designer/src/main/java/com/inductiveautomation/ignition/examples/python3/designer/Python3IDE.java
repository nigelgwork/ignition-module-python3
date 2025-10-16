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
    private Python3RestClient restClient;

    // UI Components
    private JTextField gatewayUrlField;
    private JButton connectButton;
    private JTextArea codeEditor;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private JLabel statusLabel;
    private JLabel poolStatsLabel;
    private JButton executeButton;
    private JButton clearButton;
    private JProgressBar progressBar;

    // Saved Scripts UI Components
    private JComboBox<String> savedScriptsCombo;
    private JButton saveScriptButton;
    private JButton loadScriptButton;
    private JButton deleteScriptButton;
    private JButton refreshScriptsButton;

    private Python3ExecutionWorker currentWorker;

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE(DesignerContext context) {
        this.context = context;
        this.restClient = null;  // Will be created when user connects

        initComponents();
        layoutComponents();
        attachListeners();

        // Auto-connect to default Gateway on startup
        connectToGateway();
    }

    /**
     * Initializes all UI components.
     */
    private void initComponents() {
        // Gateway URL input
        gatewayUrlField = new JTextField("http://localhost:9088", 30);
        gatewayUrlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        gatewayUrlField.setToolTipText("Gateway URL (e.g., http://localhost:9088)");

        connectButton = new JButton("Connect");
        connectButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        connectButton.setToolTipText("Connect to the specified Gateway");

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
        poolStatsLabel = new JLabel("Pool: Not Connected");
        poolStatsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        poolStatsLabel.setForeground(Color.GRAY);

        // Buttons
        executeButton = new JButton("Execute (Ctrl+Enter)");
        executeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        clearButton = new JButton("Clear Output");
        clearButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        // Saved Scripts Components
        savedScriptsCombo = new JComboBox<>();
        savedScriptsCombo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        savedScriptsCombo.setPreferredSize(new Dimension(200, 25));
        savedScriptsCombo.setToolTipText("Select a saved script to load");

        saveScriptButton = new JButton("Save Script");
        saveScriptButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        saveScriptButton.setToolTipText("Save the current script");

        loadScriptButton = new JButton("Load");
        loadScriptButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        loadScriptButton.setToolTipText("Load the selected script");

        deleteScriptButton = new JButton("Delete");
        deleteScriptButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        deleteScriptButton.setToolTipText("Delete the selected script");

        refreshScriptsButton = new JButton("Refresh");
        refreshScriptsButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        refreshScriptsButton.setToolTipText("Refresh the list of saved scripts");
    }

    /**
     * Lays out all components in the panel.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top panel: Gateway URL selector
        JPanel gatewayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        gatewayPanel.setBorder(new TitledBorder("Gateway Connection"));
        gatewayPanel.add(new JLabel("Gateway URL:"));
        gatewayPanel.add(gatewayUrlField);
        gatewayPanel.add(connectButton);

        // Saved Scripts panel
        JPanel savedScriptsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        savedScriptsPanel.setBorder(new TitledBorder("Saved Scripts"));
        savedScriptsPanel.add(new JLabel("Scripts:"));
        savedScriptsPanel.add(savedScriptsCombo);
        savedScriptsPanel.add(loadScriptButton);
        savedScriptsPanel.add(saveScriptButton);
        savedScriptsPanel.add(deleteScriptButton);
        savedScriptsPanel.add(refreshScriptsButton);

        // Center panel: Code editor
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(new TitledBorder("Python 3 Code Editor"));

        JScrollPane editorScroll = new JScrollPane(codeEditor);
        editorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorPanel.add(editorScroll, BorderLayout.CENTER);

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

        // Combine top panels
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel connectionAndScripts = new JPanel(new BorderLayout());
        connectionAndScripts.add(gatewayPanel, BorderLayout.NORTH);
        connectionAndScripts.add(savedScriptsPanel, BorderLayout.CENTER);
        topContainer.add(connectionAndScripts, BorderLayout.NORTH);
        topContainer.add(toolbarPanel, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);
        add(editorPanel, BorderLayout.CENTER);

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
        // Connect button
        connectButton.addActionListener(e -> connectToGateway());

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

        // Enter key in Gateway URL field = connect
        gatewayUrlField.addActionListener(e -> connectToGateway());

        // Saved Scripts listeners
        saveScriptButton.addActionListener(e -> saveScript());
        loadScriptButton.addActionListener(e -> loadScript());
        deleteScriptButton.addActionListener(e -> deleteScript());
        refreshScriptsButton.addActionListener(e -> refreshSavedScripts());
    }

    /**
     * Connects to the Gateway at the specified URL.
     */
    private void connectToGateway() {
        String url = gatewayUrlField.getText().trim();

        if (url.isEmpty()) {
            setStatus("Please enter a Gateway URL", Color.RED);
            return;
        }

        // Add http:// if no protocol specified
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            gatewayUrlField.setText(url);
        }

        try {
            // Create new REST client with the specified URL
            restClient = new Python3RestClient(url);

            setStatus("Connected to " + url, new Color(0, 128, 0));
            poolStatsLabel.setText("Pool: Checking...");
            poolStatsLabel.setForeground(Color.BLUE);

            LOGGER.info("Connected to Gateway: {}", url);

            // Refresh diagnostics in background
            refreshDiagnostics();

            // Load saved scripts list
            refreshSavedScripts();

        } catch (Exception e) {
            setStatus("Connection failed: " + e.getMessage(), Color.RED);
            poolStatsLabel.setText("Pool: Not Connected");
            poolStatsLabel.setForeground(Color.RED);
            LOGGER.error("Failed to connect to Gateway: {}", url, e);
        }
    }

    /**
     * Executes the Python code in the editor.
     */
    private void executeCode() {
        if (restClient == null) {
            setStatus("Not connected to Gateway. Please connect first.", Color.RED);
            return;
        }

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
        if (restClient == null) {
            poolStatsLabel.setText("Pool: Not Connected");
            poolStatsLabel.setForeground(Color.GRAY);
            return;
        }

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

    // Saved Scripts Management

    /**
     * Refreshes the list of saved scripts from the Gateway.
     */
    private void refreshSavedScripts() {
        if (restClient == null) {
            LOGGER.warn("Cannot refresh scripts: not connected to Gateway");
            return;
        }

        SwingWorker<java.util.List<ScriptMetadata>, Void> worker = new SwingWorker<java.util.List<ScriptMetadata>, Void>() {
            @Override
            protected java.util.List<ScriptMetadata> doInBackground() throws Exception {
                return restClient.listScripts();
            }

            @Override
            protected void done() {
                try {
                    java.util.List<ScriptMetadata> scripts = get();

                    savedScriptsCombo.removeAllItems();
                    for (ScriptMetadata script : scripts) {
                        savedScriptsCombo.addItem(script.getName());
                    }

                    LOGGER.info("Loaded {} saved scripts", scripts.size());

                } catch (Exception e) {
                    LOGGER.error("Failed to load scripts list", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE.this,
                            "Failed to load scripts: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Saves the current script to the Gateway.
     */
    private void saveScript() {
        if (restClient == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String code = codeEditor.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot save empty script",
                    "Empty Script",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Prompt for script name and description
        JTextField nameField = new JTextField(20);
        JTextField descField = new JTextField(30);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Script Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Save Script",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        String description = descField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Script name cannot be empty",
                    "Invalid Name",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Save in background
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(name, code, description);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Script saved: " + name, new Color(0, 128, 0));
                    refreshSavedScripts();

                } catch (Exception e) {
                    LOGGER.error("Failed to save script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE.this,
                            "Failed to save script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Loads the selected script from the Gateway.
     */
    private void loadScript() {
        if (restClient == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String selectedName = (String) savedScriptsCombo.getSelectedItem();
        if (selectedName == null || selectedName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a script to load",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Load in background
        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(selectedName);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    codeEditor.setText(script.getCode());
                    setStatus("Script loaded: " + script.getName(), new Color(0, 128, 0));

                } catch (Exception e) {
                    LOGGER.error("Failed to load script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE.this,
                            "Failed to load script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Deletes the selected script from the Gateway.
     */
    private void deleteScript() {
        if (restClient == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String selectedName = (String) savedScriptsCombo.getSelectedItem();
        if (selectedName == null || selectedName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a script to delete",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + selectedName + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Delete in background
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.deleteScript(selectedName);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Script deleted: " + selectedName, new Color(0, 128, 0));
                    refreshSavedScripts();

                } catch (Exception e) {
                    LOGGER.error("Failed to delete script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE.this,
                            "Failed to delete script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }
}
