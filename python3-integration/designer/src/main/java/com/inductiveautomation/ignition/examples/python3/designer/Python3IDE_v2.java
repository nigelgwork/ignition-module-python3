package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.ignition.examples.python3.designer.managers.GatewayConnectionManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ScriptManager;
import com.inductiveautomation.ignition.examples.python3.designer.managers.ThemeManager;
import com.inductiveautomation.ignition.examples.python3.designer.ui.EditorPanel;
import com.inductiveautomation.ignition.examples.python3.designer.ui.ScriptTreePanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;

/**
 * Python 3 IDE - Version 2.0.0 (Refactored Architecture).
 *
 * <p>Demonstrates new architecture with separated concerns:</p>
 * <ul>
 *   <li><b>Managers:</b> GatewayConnectionManager, ScriptManager, ThemeManager</li>
 *   <li><b>UI Panels:</b> EditorPanel, ScriptTreePanel</li>
 *   <li><b>Main Class:</b> Assembly and event coordination (THIS FILE)</li>
 * </ul>
 *
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Each file ~100-400 lines (vs 2,676 in v1.9)</li>
 *   <li>Token usage 1K-4K per file (vs 25K in v1.9)</li>
 *   <li>Easier to test, maintain, and extend</li>
 * </ul>
 *
 * <p><b>Note:</b> This is a simplified version demonstrating the architecture.
 * Python3IDE_v1_9 remains the full-featured version until v2.x achieves feature parity.</p>
 *
 * v2.0.0: Refactored from Python3IDE_v1_9.java monolith
 */
public class Python3IDE_v2 extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3IDE_v2.class);

    private final DesignerContext context;

    // === Managers (Business Logic) ===
    private final GatewayConnectionManager connectionManager;
    private ScriptManager scriptManager;  // Created after connection
    private final ThemeManager themeManager;

    // === UI Panels (Presentation) ===
    private final EditorPanel editorPanel;
    private final ScriptTreePanel treePanel;
    private final ScriptMetadataPanel metadataPanel;
    private final DiagnosticsPanel diagnosticsPanel;
    private final ModernStatusBar statusBar;

    // === Top-Level UI Components ===
    private JTextField gatewayUrlField;
    private JButton connectButton;
    private JButton executeButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton deleteButton;
    private JProgressBar progressBar;
    private JComboBox<String> themeSelector;

    // === State ===
    private Python3ExecutionWorker currentWorker;

    /**
     * Creates Python3IDE v2.0 with refactored architecture.
     */
    public Python3IDE_v2(DesignerContext context) {
        this.context = context;

        // Initialize managers
        this.connectionManager = new GatewayConnectionManager();
        this.themeManager = new ThemeManager(Python3IDE_v2.class);

        // Initialize UI panels
        this.editorPanel = new EditorPanel();
        this.treePanel = new ScriptTreePanel();
        this.metadataPanel = new ScriptMetadataPanel();
        this.diagnosticsPanel = new DiagnosticsPanel();
        this.statusBar = new ModernStatusBar();

        // Build UI
        initComponents();
        layoutComponents();
        wireUpEvents();

        // Apply initial theme
        applyTheme("dark");

        LOGGER.info("Python3IDE v2.0.0 initialized (refactored architecture)");
    }

    // === Initialization ===

    private void initComponents() {
        gatewayUrlField = new JTextField("http://localhost:8088", 30);
        gatewayUrlField.setFont(ModernTheme.FONT_REGULAR);

        connectButton = new JButton("Connect");
        connectButton.setFont(ModernTheme.FONT_REGULAR);

        executeButton = new JButton("▶ Run");
        executeButton.setFont(ModernTheme.FONT_REGULAR);
        executeButton.setEnabled(false);

        saveButton = new JButton("💾 Save");
        saveButton.setFont(ModernTheme.FONT_REGULAR);
        saveButton.setEnabled(false);

        loadButton = new JButton("📂 Load");
        loadButton.setFont(ModernTheme.FONT_REGULAR);
        loadButton.setEnabled(false);

        deleteButton = new JButton("🗑️ Delete");
        deleteButton.setFont(ModernTheme.FONT_REGULAR);
        deleteButton.setEnabled(false);

        progressBar = new JProgressBar();
        progressBar.setVisible(false);

        String[] themes = {"Dark", "VS Code Dark+", "Monokai"};
        themeSelector = new JComboBox<>(themes);
        themeSelector.setFont(ModernTheme.FONT_REGULAR);

        statusBar.setStatus("Ready - v2.0.0 (Refactored)", ModernStatusBar.MessageType.INFO);
        statusBar.setConnection("Not Connected");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Top: Connection + Toolbar
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center: Main split (tree | editor+diagnostics)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setLeftComponent(createSidebar());
        mainSplit.setRightComponent(createMainPanel());
        mainSplit.setDividerLocation(250);
        mainSplit.setResizeWeight(0.15);
        mainSplit.setBackground(ModernTheme.BACKGROUND_DARK);
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(8);

        if (mainSplit.getUI() instanceof BasicSplitPaneUI) {
            ((BasicSplitPaneUI) mainSplit.getUI()).getDivider().setBackground(ModernTheme.BACKGROUND_DARKER);
        }

        add(mainSplit, BorderLayout.CENTER);

        // Bottom: Status bar
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ModernTheme.PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Gateway Connection",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Left: Connection
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        leftPanel.add(new JLabel("URL:"));
        leftPanel.add(gatewayUrlField);
        leftPanel.add(connectButton);
        leftPanel.add(executeButton);
        leftPanel.add(saveButton);
        leftPanel.add(loadButton);
        leftPanel.add(deleteButton);
        leftPanel.add(progressBar);

        // Right: Theme
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        rightPanel.add(new JLabel("Theme:"));
        rightPanel.add(themeSelector);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JComponent createSidebar() {
        JSplitPane sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sidebarSplit.setTopComponent(treePanel);
        sidebarSplit.setBottomComponent(metadataPanel);
        sidebarSplit.setDividerLocation(400);
        sidebarSplit.setResizeWeight(0.6);
        sidebarSplit.setBackground(ModernTheme.BACKGROUND_DARK);
        sidebarSplit.setBorder(null);
        sidebarSplit.setDividerSize(8);

        if (sidebarSplit.getUI() instanceof BasicSplitPaneUI) {
            ((BasicSplitPaneUI) sidebarSplit.getUI()).getDivider().setBackground(ModernTheme.BACKGROUND_DARKER);
        }

        return sidebarSplit;
    }

    private JComponent createMainPanel() {
        JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.setLeftComponent(editorPanel);
        mainPanel.setRightComponent(diagnosticsPanel);
        mainPanel.setResizeWeight(0.75);
        mainPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        mainPanel.setBorder(null);
        mainPanel.setDividerSize(8);

        if (mainPanel.getUI() instanceof BasicSplitPaneUI) {
            ((BasicSplitPaneUI) mainPanel.getUI()).getDivider().setBackground(ModernTheme.BACKGROUND_DARKER);
        }

        return mainPanel;
    }

    private void wireUpEvents() {
        connectButton.addActionListener(e -> connectToGateway());
        executeButton.addActionListener(e -> executeCode());
        saveButton.addActionListener(e -> saveScript());
        loadButton.addActionListener(e -> loadSelectedScript());
        deleteButton.addActionListener(e -> deleteScript());

        themeSelector.addActionListener(e -> {
            String selected = (String) themeSelector.getSelectedItem();
            if (selected != null) {
                applyTheme(themeManager.mapThemeNameToKey(selected));
            }
        });

        treePanel.setSelectionListener(this::loadScript);
    }

    // === Event Handlers ===

    private void connectToGateway() {
        String url = gatewayUrlField.getText().trim();

        if (url.isEmpty()) {
            statusBar.setStatus("Please enter Gateway URL", ModernStatusBar.MessageType.ERROR);
            return;
        }

        boolean connected = connectionManager.connect(url);

        if (connected) {
            scriptManager = new ScriptManager(connectionManager.getRestClient());

            statusBar.setStatus("Connected to " + url, ModernStatusBar.MessageType.SUCCESS);
            statusBar.setConnection("Connected", ModernTheme.SUCCESS);

            diagnosticsPanel.setRestClient(connectionManager.getRestClient());

            executeButton.setEnabled(true);
            saveButton.setEnabled(true);
            loadButton.setEnabled(true);
            deleteButton.setEnabled(true);

            refreshScriptTree();
            refreshDiagnostics();
        } else {
            statusBar.setStatus("Connection failed", ModernStatusBar.MessageType.ERROR);
            statusBar.setConnection("Not Connected", ModernTheme.ERROR);
        }
    }

    private void executeCode() {
        if (!connectionManager.isConnected()) {
            return;
        }

        String code = editorPanel.getCode().trim();

        if (code.isEmpty()) {
            statusBar.setStatus("No code to execute", ModernStatusBar.MessageType.WARNING);
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        editorPanel.clearOutput();

        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusBar.setStatus("Executing...", ModernStatusBar.MessageType.INFO);

        currentWorker = new Python3ExecutionWorker(
                connectionManager.getRestClient(),
                code,
                new HashMap<>(),
                this::handleSuccess,
                this::handleError
        );

        currentWorker.execute();
    }

    private void handleSuccess(ExecutionResult result) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        if (result.isSuccess()) {
            String output = result.getResult() != null ? result.getResult() : "(no output)";
            editorPanel.setOutput(output);

            long time = result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0;
            statusBar.setStatus(String.format("Completed in %d ms", time), ModernStatusBar.MessageType.SUCCESS);
        } else {
            String error = result.getError() != null ? result.getError() : "Unknown error";
            editorPanel.setError(error);
            statusBar.setStatus("Execution failed", ModernStatusBar.MessageType.ERROR);
        }

        refreshDiagnostics();
    }

    private void handleError(Exception error) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        editorPanel.setError("Connection error: " + error.getMessage());
        statusBar.setStatus("Execution failed", ModernStatusBar.MessageType.ERROR);

        LOGGER.error("Execution error", error);
    }

    private void saveScript() {
        if (!connectionManager.isConnected() || scriptManager == null) {
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Enter script name:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                scriptManager.saveScript(name.trim(), editorPanel.getCode(), "", "Unknown", "", "1.0");
                statusBar.setStatus("Saved: " + name, ModernStatusBar.MessageType.SUCCESS);
                refreshScriptTree();
            } catch (Exception e) {
                LOGGER.error("Failed to save script", e);
                statusBar.setStatus("Save failed", ModernStatusBar.MessageType.ERROR);
            }
        }
    }

    private void loadSelectedScript() {
        // TODO: Implement script selection dialog
        statusBar.setStatus("Load feature: Use script tree", ModernStatusBar.MessageType.INFO);
    }

    private void deleteScript() {
        if (!connectionManager.isConnected() || scriptManager == null) {
            return;
        }

        String selectedScript = treePanel.getSelectedScriptName();
        if (selectedScript == null || selectedScript.isEmpty()) {
            statusBar.setStatus("No script selected", ModernStatusBar.MessageType.WARNING);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + selectedScript + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                scriptManager.deleteScript(selectedScript);
                statusBar.setStatus("Deleted: " + selectedScript, ModernStatusBar.MessageType.SUCCESS);
                refreshScriptTree();
                editorPanel.setCode("");
                metadataPanel.clear();
            } catch (Exception e) {
                LOGGER.error("Failed to delete script", e);
                statusBar.setStatus("Delete failed: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
            }
        }
    }

    private void loadScript(String name) {
        if (!connectionManager.isConnected() || scriptManager == null) {
            return;
        }

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return scriptManager.loadScript(name);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    editorPanel.setCode(script.getCode());

                    ScriptMetadata metadata = new ScriptMetadata();
                    metadata.setName(script.getName());
                    metadata.setDescription(script.getDescription());
                    metadata.setAuthor(script.getAuthor());
                    metadata.setCreatedDate(script.getCreatedDate());
                    metadata.setLastModified(script.getLastModified());
                    metadata.setVersion(script.getVersion());
                    metadataPanel.displayMetadata(metadata);

                    statusBar.setStatus("Loaded: " + script.getName(), ModernStatusBar.MessageType.SUCCESS);
                } catch (Exception e) {
                    LOGGER.error("Failed to load script", e);
                    statusBar.setStatus("Load failed", ModernStatusBar.MessageType.ERROR);
                }
            }
        };

        worker.execute();
    }

    private void refreshScriptTree() {
        if (!connectionManager.isConnected() || scriptManager == null) {
            return;
        }

        SwingWorker<List<ScriptMetadata>, Void> worker = new SwingWorker<List<ScriptMetadata>, Void>() {
            @Override
            protected List<ScriptMetadata> doInBackground() throws Exception {
                return scriptManager.listScripts();
            }

            @Override
            protected void done() {
                try {
                    List<ScriptMetadata> scripts = get();
                    treePanel.refreshTree(scripts);
                } catch (Exception e) {
                    LOGGER.error("Failed to refresh script tree", e);
                }
            }
        };

        worker.execute();
    }

    private void refreshDiagnostics() {
        if (!connectionManager.isConnected()) {
            return;
        }

        SwingWorker<PoolStats, Void> worker = new SwingWorker<PoolStats, Void>() {
            @Override
            protected PoolStats doInBackground() throws Exception {
                return connectionManager.getPoolStats();
            }

            @Override
            protected void done() {
                try {
                    PoolStats stats = get();
                    statusBar.updatePoolStats(stats);
                } catch (Exception e) {
                    statusBar.setPoolStats("Pool: Unavailable", ModernTheme.ERROR);
                }
            }
        };

        worker.execute();
    }

    private void applyTheme(String themeName) {
        try {
            themeManager.applyTheme(
                    themeName,
                    this,
                    editorPanel.getCodeEditor(),
                    editorPanel.getOutputArea(),
                    editorPanel.getErrorArea(),
                    treePanel.getTree()
            );
            statusBar.setStatus("Theme: " + themeName, ModernStatusBar.MessageType.SUCCESS);
        } catch (Exception e) {
            LOGGER.error("Failed to apply theme", e);
        }
    }

    // === Public API ===

    public RSyntaxTextArea getCodeEditor() {
        return editorPanel.getCodeEditor();
    }
}
