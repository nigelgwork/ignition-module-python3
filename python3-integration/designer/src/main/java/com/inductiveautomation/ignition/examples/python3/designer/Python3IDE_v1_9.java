package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.designer.model.DesignerContext;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Python 3 IDE panel for the Ignition Designer - Version 1.9.0.
 *
 * <p>Enhanced professional IDE with:</p>
 * <ul>
 *   <li>RSyntaxTextArea with Python syntax highlighting</li>
 *   <li>Left sidebar with folder tree for script organization</li>
 *   <li>Metadata panel showing script information</li>
 *   <li>Theme system (light and dark themes)</li>
 *   <li>Enhanced keyboard shortcuts</li>
 *   <li>Unsaved changes detection</li>
 *   <li>Export/import functionality</li>
 * </ul>
 */
public class Python3IDE_v1_9 extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3IDE_v1_9.class);
    private static final String PREF_THEME = "python3ide.theme";
    private static final String PREF_FONT_SIZE = "python3ide.fontsize";

    private final DesignerContext context;
    private Python3RestClient restClient;
    private PythonSyntaxChecker syntaxChecker;
    private AutoCompletion autoCompletion;

    // UI Components
    private JTextField gatewayUrlField;
    private JButton connectButton;
    private JComboBox<String> themeSelector;
    private RSyntaxTextArea codeEditor;
    private JLabel currentScriptLabel;
    private JTextArea outputArea;
    private JTextArea errorArea;
    private ModernStatusBar statusBar;
    private JButton executeButton;
    private JButton clearButton;
    private JButton saveButton;
    private JButton saveAsButton;
    private JButton importButton;
    private JButton exportButton;
    private JProgressBar progressBar;

    // Script Browser Components
    private JTree scriptTree;
    private DefaultTreeModel treeModel;
    private ScriptTreeNode rootNode;
    private ScriptMetadataPanel metadataPanel;
    private DiagnosticsPanel diagnosticsPanel;

    // Theme and Settings
    private String currentTheme;
    private int fontSize;

    // Find/Replace Dialogs
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;

    // Unsaved Changes Tracking
    private UnsavedChangesTracker changesTracker;
    private ScriptMetadata currentScript;

    private Python3ExecutionWorker currentWorker;

    /**
     * Creates a new Python 3 IDE panel.
     *
     * @param context the Designer context
     */
    public Python3IDE_v1_9(DesignerContext context) {
        this.context = context;
        this.restClient = null;

        // Load preferences
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE_v1_9.class);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
        this.fontSize = prefs.getInt(PREF_FONT_SIZE, 12);

        initComponents();
        layoutComponents();
        attachListeners();
        applyTheme(currentTheme);

        // Auto-connect to default Gateway on startup
        connectToGateway();
    }

    /**
     * Initializes all UI components.
     */
    private void initComponents() {
        // Gateway URL input
        gatewayUrlField = new JTextField("http://localhost:9088", 25);
        gatewayUrlField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        connectButton = ModernButton.createPrimary("Connect");

        // Theme selector dropdown
        String[] themes = {"Dark", "VS Code Dark+", "Monokai", "Dracula", "Default (Light)", "IntelliJ Light", "Eclipse"};
        themeSelector = new JComboBox<>(themes);
        themeSelector.setSelectedItem("Dark");  // Match default theme
        themeSelector.setFont(ModernTheme.FONT_REGULAR);
        themeSelector.setBackground(ModernTheme.PANEL_BACKGROUND);
        themeSelector.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        themeSelector.setPreferredSize(new Dimension(180, 28));  // Expanded width to avoid text cutoff (Issue 2 - v1.17.1)

        // Code editor with RSyntaxTextArea
        codeEditor = new RSyntaxTextArea(20, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setAutoIndentEnabled(true);
        codeEditor.setMarkOccurrences(true);
        codeEditor.setPaintTabLines(true);
        codeEditor.setTabSize(4);
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        codeEditor.setText("# Python 3.11 Code Editor\n# Enhanced with syntax highlighting\n\nresult = 2 + 2\nprint(f\"Result: {result}\")");

        // Enable parser notifications for real-time error checking
        codeEditor.setMarkOccurrences(true);

        // Unsaved changes tracker
        changesTracker = new UnsavedChangesTracker(codeEditor);
        changesTracker.addChangeListener(this::onDirtyStateChanged);

        // Current script indicator label
        currentScriptLabel = new JLabel("No script selected");
        currentScriptLabel.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));
        currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        currentScriptLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

        // Output area
        outputArea = new JTextArea(8, 80);
        outputArea.setFont(ModernTheme.FONT_MONOSPACE);
        outputArea.setEditable(false);
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        // Error area
        errorArea = new JTextArea(8, 80);
        errorArea.setFont(ModernTheme.FONT_MONOSPACE);
        errorArea.setEditable(false);
        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);

        // Status bar
        statusBar = new ModernStatusBar();

        // Pool click listener for adjusting pool size (v1.17.2)
        statusBar.setPoolClickListener(this::handlePoolClicked);

        // Buttons
        executeButton = ModernButton.createPrimary("Execute (Ctrl+Enter)");
        clearButton = ModernButton.createDefault("Clear");
        saveButton = ModernButton.createSuccess("Save (Ctrl+S)");
        saveAsButton = ModernButton.createDefault("Save As...");
        importButton = ModernButton.createDefault("Import...");
        exportButton = ModernButton.createDefault("Export...");

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);

        // Script Browser Tree (Ignition Tag Browser style)
        rootNode = new ScriptTreeNode("Scripts");
        treeModel = new DefaultTreeModel(rootNode);
        scriptTree = new JTree(treeModel);
        scriptTree.setRootVisible(true);
        scriptTree.setShowsRootHandles(true);
        scriptTree.setCellRenderer(new ScriptTreeCellRenderer());
        scriptTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        scriptTree.setRowHeight(20);  // Compact 20px rows (16px icon + 4px padding)
        scriptTree.setDragEnabled(true);
        scriptTree.setDropMode(DropMode.ON_OR_INSERT);
        scriptTree.setTransferHandler(new ScriptTreeTransferHandler());

        // Ignition Tag Browser-style tree appearance
        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        scriptTree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        scriptTree.putClientProperty("JTree.lineStyle", "None");  // Clean look without connecting lines
        scriptTree.setToggleClickCount(1);  // Single-click to expand (like Tag Browser)

        // Metadata Panel
        metadataPanel = new ScriptMetadataPanel();

        // Diagnostics Panel (v1.15.0 - displays performance metrics)
        diagnosticsPanel = new DiagnosticsPanel();

        // Find/Replace Dialogs (lazy initialized when first used)
        findDialog = null;
        replaceDialog = null;
    }

    /**
     * Lays out all components in the panel.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Top area: Gateway Connection with theme selector
        JPanel gatewayPanel = new JPanel(new BorderLayout(10, 0));
        gatewayPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        gatewayPanel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Gateway Connection",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)  // Reduced top/bottom padding for 25% height reduction (Issue 5 - v1.15.1)
        ));

        // Left side: URL and Connect button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));  // Reduced vertical gap for 25% height reduction (Issue 5 - v1.15.1)
        leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        JLabel urlLabel = new JLabel("URL:");
        urlLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        leftPanel.add(urlLabel);
        leftPanel.add(gatewayUrlField);
        leftPanel.add(connectButton);
        gatewayPanel.add(leftPanel, BorderLayout.WEST);

        // Right side: Theme selector
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 1));  // Reduced vertical gap for 25% height reduction (Issue 5 - v1.15.1)
        rightPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        themeLabel.setFont(ModernTheme.FONT_REGULAR);
        rightPanel.add(themeLabel);
        rightPanel.add(themeSelector);
        gatewayPanel.add(rightPanel, BorderLayout.EAST);

        add(gatewayPanel, BorderLayout.NORTH);

        // Create main split pane (sidebar | editor)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setBackground(ModernTheme.BACKGROUND_DARK);
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look

        // Fix divider color for dark theme using custom UI (Issue 1 - v1.15.1)
        mainSplit.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this);
                divider.setBackground(new Color(60, 63, 65));
                divider.setBorder(null);
                return divider;
            }
        });

        // Left sidebar: Script browser + metadata
        JPanel sidebar = createSidebar();
        mainSplit.setLeftComponent(sidebar);

        // Right side: Editor + output
        JPanel editorPanel = createEditorPanel();
        mainSplit.setRightComponent(editorPanel);

        add(mainSplit, BorderLayout.CENTER);

        // Status bar at the bottom
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Creates the left sidebar with script tree and metadata.
     */
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(5, 5));
        sidebar.setPreferredSize(new Dimension(250, 600));
        sidebar.setBackground(ModernTheme.BACKGROUND_DARK);

        // Script tree
        JScrollPane treeScroll = new JScrollPane(scriptTree);
        treeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);  // Hide when not needed (Issue 4 - v1.15.1)
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScroll.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Script Browser",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        treeScroll.setBackground(ModernTheme.TREE_BACKGROUND);
        treeScroll.getViewport().setBackground(ModernTheme.TREE_BACKGROUND);

        // Toolbar above tree
        JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        treeToolbar.setBackground(ModernTheme.PANEL_BACKGROUND);

        ModernButton newFolderBtn = ModernButton.createSmall("+Folder");
        newFolderBtn.setToolTipText("New Folder");
        newFolderBtn.addActionListener(e -> createNewFolder());

        ModernButton newScriptBtn = ModernButton.createSmall("+Script");
        newScriptBtn.setToolTipText("New Script");
        newScriptBtn.addActionListener(e -> createNewScript());

        ModernButton refreshBtn = ModernButton.createSmall("Refresh");
        refreshBtn.setToolTipText("Refresh Scripts");
        refreshBtn.addActionListener(e -> refreshScriptTree());

        treeToolbar.add(newFolderBtn);
        treeToolbar.add(newScriptBtn);
        treeToolbar.add(refreshBtn);

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(ModernTheme.BACKGROUND_DARK);
        treePanel.add(treeToolbar, BorderLayout.NORTH);
        treePanel.add(treeScroll, BorderLayout.CENTER);

        // Bottom panel: metadata only (diagnostics moved to execution results panel - v1.17.2)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(ModernTheme.BACKGROUND_DARK);
        bottomPanel.add(metadataPanel, BorderLayout.CENTER);

        // Split tree and bottom panel (metadata only)
        JSplitPane sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sidebarSplit.setTopComponent(treePanel);
        sidebarSplit.setBottomComponent(bottomPanel);
        sidebarSplit.setDividerLocation(400);  // More space for tree since diagnostics moved (v1.17.2)
        sidebarSplit.setResizeWeight(0.6);  // More weight to tree
        sidebarSplit.setBackground(ModernTheme.BACKGROUND_DARK);
        sidebarSplit.setBorder(null);
        sidebarSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look

        // Fix divider color for dark theme using custom UI (Issue 1 - v1.15.1)
        sidebarSplit.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this);
                divider.setBackground(new Color(60, 63, 65));
                divider.setBorder(null);
                return divider;
            }
        });

        sidebar.add(sidebarSplit, BorderLayout.CENTER);

        return sidebar;
    }

    /**
     * Creates the editor panel.
     */
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(ModernTheme.BACKGROUND_DARK);

        // Editor with line numbers
        RTextScrollPane editorScroll = new RTextScrollPane(codeEditor);
        editorScroll.setLineNumbersEnabled(true);

        JPanel editorContainer = new JPanel(new BorderLayout());
        editorContainer.setBackground(ModernTheme.PANEL_BACKGROUND);
        editorContainer.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Python 3 Code Editor",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Add current script indicator at top
        editorContainer.add(currentScriptLabel, BorderLayout.NORTH);
        editorContainer.add(editorScroll, BorderLayout.CENTER);

        panel.add(editorContainer, BorderLayout.CENTER);

        // Toolbar - aligned with tree toolbar (3px gaps between buttons, padding around panel)
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        toolbar.setBackground(ModernTheme.PANEL_BACKGROUND);
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));  // Add vertical padding (Issue 3 - v1.17.1)
        toolbar.add(executeButton);
        toolbar.add(clearButton);
        toolbar.add(saveButton);
        toolbar.add(saveAsButton);
        toolbar.add(importButton);
        toolbar.add(exportButton);
        toolbar.add(progressBar);

        panel.add(toolbar, BorderLayout.NORTH);

        // Output tabs
        JTabbedPane outputTabs = new JTabbedPane();
        outputTabs.setBackground(ModernTheme.PANEL_BACKGROUND);
        outputTabs.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);  // Hide when not needed (Issue 4 - v1.15.1)
        outputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());  // Remove border to prevent white bars
        outputScroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        outputTabs.addTab("Output", outputScroll);

        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);  // Hide when not needed (Issue 4 - v1.15.1)
        errorScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        errorScroll.setBorder(BorderFactory.createEmptyBorder());  // Remove border to prevent white bars
        errorScroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        outputTabs.addTab("Errors", errorScroll);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        outputPanel.setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Execution Results",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        outputPanel.add(outputTabs, BorderLayout.CENTER);

        // Split execution results (left 75%) and diagnostics (right 25%) - v1.17.2
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        bottomSplit.setLeftComponent(outputPanel);
        bottomSplit.setRightComponent(diagnosticsPanel);
        bottomSplit.setResizeWeight(0.75);  // 75% for execution results, 25% for diagnostics
        bottomSplit.setBackground(ModernTheme.BACKGROUND_DARK);
        bottomSplit.setBorder(null);
        bottomSplit.setDividerSize(4);  // Reduced from 8 to 4 for cleaner look
        bottomSplit.setPreferredSize(new Dimension(600, 200));

        // Fix divider color for dark theme using custom UI (v1.17.2)
        bottomSplit.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this);
                divider.setBackground(new Color(60, 63, 65));
                divider.setBorder(null);
                return divider;
            }
        });

        panel.add(bottomSplit, BorderLayout.SOUTH);

        return panel;
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

        // Save button
        saveButton.addActionListener(e -> saveCurrentScript());

        // Save As button
        saveAsButton.addActionListener(e -> saveScriptAs());

        // Import button
        importButton.addActionListener(e -> importScript());

        // Export button
        exportButton.addActionListener(e -> exportCurrentScript());

        // Theme selector
        themeSelector.addActionListener(e -> {
            String selected = (String) themeSelector.getSelectedItem();
            if (selected != null) {
                String themeKey = mapThemeNameToKey(selected);
                applyTheme(themeKey);
            }
        });

        // Keyboard shortcuts
        setupKeyboardShortcuts();

        // Tree selection
        scriptTree.addTreeSelectionListener(e -> onTreeSelectionChanged());

        // Tree double-click
        scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelectedScript();
                }
            }
        });

        // Tree right-click
        scriptTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });

        // Gateway URL enter key
        gatewayUrlField.addActionListener(e -> connectToGateway());

        // Cursor position tracking
        codeEditor.addCaretListener(e -> {
            int line = codeEditor.getCaretLineNumber() + 1;  // Convert to 1-based
            int col = codeEditor.getCaretOffsetFromLineStart() + 1;  // Convert to 1-based
            statusBar.setCursorPosition(line, col);
        });
    }

    /**
     * Sets up keyboard shortcuts.
     */
    private void setupKeyboardShortcuts() {
        InputMap inputMap = codeEditor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = codeEditor.getActionMap();

        // Ctrl+Enter: Execute
        KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
        inputMap.put(ctrlEnter, "execute");
        actionMap.put("execute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCode();
            }
        });

        // Ctrl+S: Save
        KeyStroke ctrlS = KeyStroke.getKeyStroke("control S");
        inputMap.put(ctrlS, "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentScript();
            }
        });

        // Ctrl+Shift+S: Save As
        KeyStroke ctrlShiftS = KeyStroke.getKeyStroke("control shift S");
        inputMap.put(ctrlShiftS, "saveAs");
        actionMap.put("saveAs", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveScriptAs();
            }
        });

        // Ctrl+N: New Script
        KeyStroke ctrlN = KeyStroke.getKeyStroke("control N");
        inputMap.put(ctrlN, "newScript");
        actionMap.put("newScript", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewScript();
            }
        });

        // Ctrl++: Increase font
        KeyStroke ctrlPlus = KeyStroke.getKeyStroke("control PLUS");
        inputMap.put(ctrlPlus, "increaseFontSize");
        actionMap.put("increaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(1);
            }
        });

        // Ctrl+-: Decrease font
        KeyStroke ctrlMinus = KeyStroke.getKeyStroke("control MINUS");
        inputMap.put(ctrlMinus, "decreaseFontSize");
        actionMap.put("decreaseFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeFontSize(-1);
            }
        });

        // Ctrl+0: Reset font
        KeyStroke ctrl0 = KeyStroke.getKeyStroke("control 0");
        inputMap.put(ctrl0, "resetFontSize");
        actionMap.put("resetFontSize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFontSize(12);
            }
        });

        // Ctrl+F: Find
        KeyStroke ctrlF = KeyStroke.getKeyStroke("control F");
        inputMap.put(ctrlF, "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });

        // Ctrl+H: Replace
        KeyStroke ctrlH = KeyStroke.getKeyStroke("control H");
        inputMap.put(ctrlH, "replace");
        actionMap.put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReplaceDialog();
            }
        });
    }

    /**
     * Connects to the Gateway.
     */
    private void connectToGateway() {
        String url = gatewayUrlField.getText().trim();

        if (url.isEmpty()) {
            setStatus("Please enter a Gateway URL", Color.RED);
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            gatewayUrlField.setText(url);
        }

        try {
            restClient = new Python3RestClient(url);
            statusBar.setStatus("Connected to " + url, ModernStatusBar.MessageType.SUCCESS);
            statusBar.setConnection("Connected", ModernTheme.SUCCESS);
            statusBar.setPoolStats("Pool: Checking...", ModernTheme.INFO);

            // Initialize diagnostics panel with REST client (v1.15.0)
            diagnosticsPanel.setRestClient(restClient);

            LOGGER.info("Connected to Gateway: {}", url);

            // Initialize syntax checker for real-time error checking
            if (syntaxChecker != null) {
                syntaxChecker.dispose();
                codeEditor.removeParser(syntaxChecker);
            }
            syntaxChecker = new PythonSyntaxChecker(codeEditor, restClient);
            codeEditor.addParser(syntaxChecker);
            LOGGER.info("Real-time syntax checking enabled");

            // Initialize auto-completion with Jedi-powered completions
            if (autoCompletion != null) {
                autoCompletion.uninstall();
            }
            CompletionProvider provider = new Python3CompletionProvider(restClient);
            autoCompletion = new AutoCompletion(provider);
            autoCompletion.setAutoActivationEnabled(true);
            autoCompletion.setAutoCompleteSingleChoices(false);
            autoCompletion.setAutoActivationDelay(500);  // 500ms delay after typing
            autoCompletion.setShowDescWindow(true);
            autoCompletion.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK));  // Ctrl+Space
            autoCompletion.install(codeEditor);
            LOGGER.info("Auto-completion enabled: Ctrl+Space to trigger, auto-activates on typing");

            refreshDiagnostics();
            refreshPythonVersion();
            refreshScriptTree();

        } catch (Exception e) {
            statusBar.setStatus("Connection failed: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
            statusBar.setConnection("Not Connected", ModernTheme.ERROR);
            statusBar.setPoolStats("Pool: Not Connected", ModernTheme.ERROR);
            statusBar.setPythonVersion("Python: --");
            LOGGER.error("Failed to connect to Gateway: {}", url, e);
        }
    }

    /**
     * Executes the Python code in the editor.
     */
    private void executeCode() {
        if (restClient == null) {
            setStatus("Not connected to Gateway", Color.RED);
            return;
        }

        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            setStatus("No code to execute", Color.ORANGE);
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        clearOutput();

        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        setStatus("Executing...", Color.BLUE);

        currentWorker = new Python3ExecutionWorker(
                restClient,
                code,
                new HashMap<>(),
                this::handleSuccess,
                this::handleError
        );

        currentWorker.execute();
    }

    /**
     * Handles successful execution.
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

        refreshDiagnostics();
    }

    /**
     * Handles execution errors.
     */
    private void handleError(Exception error) {
        executeButton.setEnabled(true);
        progressBar.setVisible(false);

        errorArea.setText("Connection error: " + error.getMessage());
        setStatus("Execution failed", Color.RED);

        LOGGER.error("Execution error", error);
    }

    /**
     * Clears output areas.
     */
    private void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }

    /**
     * Refreshes diagnostics.
     */
    private void refreshDiagnostics() {
        if (restClient == null) {
            return;
        }

        SwingWorker<PoolStats, Void> worker = new SwingWorker<PoolStats, Void>() {
            @Override
            protected PoolStats doInBackground() throws Exception {
                return restClient.getPoolStats();
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

    /**
     * Refreshes Python version display in status bar.
     */
    private void refreshPythonVersion() {
        if (restClient == null) {
            statusBar.setPythonVersion("Python: --");
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return restClient.getPythonVersion();
            }

            @Override
            protected void done() {
                try {
                    String version = get();
                    statusBar.setPythonVersion("Python: " + version);
                } catch (Exception e) {
                    statusBar.setPythonVersion("Python: --");
                    LOGGER.warn("Failed to get Python version", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Handles pool stats click event to adjust pool size.
     *
     * v1.17.2: Allow user to adjust pool size (1-20)
     */
    private void handlePoolClicked() {
        if (restClient == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Get current pool size
        int currentSize = 3;  // Default
        try {
            PoolStats stats = restClient.getPoolStats();
            currentSize = stats.getTotalSize();
        } catch (Exception e) {
            LOGGER.warn("Failed to get current pool size", e);
        }

        // Show input dialog to adjust pool size
        String input = JOptionPane.showInputDialog(
                this,
                "Enter new pool size (1-20):",
                "Adjust Pool Size",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input == null || input.trim().isEmpty()) {
            return;  // User cancelled
        }

        try {
            int newSize = Integer.parseInt(input.trim());

            if (newSize < 1 || newSize > 20) {
                JOptionPane.showMessageDialog(
                        this,
                        "Pool size must be between 1 and 20",
                        "Invalid Pool Size",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Set the new pool size via REST API
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    restClient.setPoolSize(newSize);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        setStatus("Pool size changed to " + newSize, new Color(0, 128, 0));
                        refreshDiagnostics();  // Refresh to show new pool size
                    } catch (Exception e) {
                        LOGGER.error("Failed to set pool size", e);
                        JOptionPane.showMessageDialog(
                                Python3IDE_v1_9.this,
                                "Failed to set pool size: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };

            worker.execute();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid number",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Sets status message with color.
     */
    private void setStatus(String message, Color color) {
        // Map Color to MessageType
        ModernStatusBar.MessageType type = ModernStatusBar.MessageType.INFO;
        if (color.equals(Color.RED) || color.equals(ModernTheme.ERROR)) {
            type = ModernStatusBar.MessageType.ERROR;
        } else if (color.equals(Color.ORANGE) || color.equals(ModernTheme.WARNING)) {
            type = ModernStatusBar.MessageType.WARNING;
        } else if (color.equals(new Color(0, 128, 0)) || color.equals(ModernTheme.SUCCESS)) {
            type = ModernStatusBar.MessageType.SUCCESS;
        }
        statusBar.setStatus(message, type);
    }

    // Script Management Methods

    /**
     * Validates a name for illegal characters.
     * Script and folder names cannot contain: / \ : * ? " < > |
     *
     * @param name the name to validate
     * @return true if valid, false if contains illegal characters
     */
    private boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        // Check for illegal characters
        String illegalChars = "/\\:*?\"<>|";
        for (char c : illegalChars.toCharArray()) {
            if (name.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows an error dialog for invalid name.
     */
    private void showInvalidNameError(String name) {
        JOptionPane.showMessageDialog(
                this,
                "Invalid name: '" + name + "'\n\n" +
                "Names cannot contain the following characters:\n" +
                "/ \\ : * ? \" < > |",
                "Invalid Name",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Refreshes the script tree from the Gateway.
     */
    private void refreshScriptTree() {
        if (restClient == null) {
            return;
        }

        SwingWorker<List<ScriptMetadata>, Void> worker = new SwingWorker<List<ScriptMetadata>, Void>() {
            @Override
            protected List<ScriptMetadata> doInBackground() throws Exception {
                return restClient.listScripts();
            }

            @Override
            protected void done() {
                try {
                    List<ScriptMetadata> scripts = get();
                    buildScriptTree(scripts);
                    LOGGER.info("Loaded {} scripts", scripts.size());
                } catch (Exception e) {
                    LOGGER.error("Failed to load scripts", e);
                }
            }
        };

        worker.execute();
    }

    /**
     * Builds the script tree from a list of scripts.
     */
    private void buildScriptTree(List<ScriptMetadata> scripts) {
        rootNode.removeAllChildren();

        // Build folder structure
        Map<String, ScriptTreeNode> folders = new HashMap<>();

        for (ScriptMetadata script : scripts) {
            String folderPath = script.getFolderPath();

            if (folderPath == null || folderPath.isEmpty()) {
                // Script at root level
                rootNode.add(new ScriptTreeNode(script));
            } else {
                // Create folder hierarchy
                ScriptTreeNode parent = getOrCreateFolder(folderPath, folders);
                parent.add(new ScriptTreeNode(script));
            }
        }

        treeModel.reload();
        scriptTree.expandRow(0);  // Expand root
    }

    /**
     * Gets or creates a folder node at the specified path.
     */
    private ScriptTreeNode getOrCreateFolder(String folderPath, Map<String, ScriptTreeNode> folders) {
        if (folders.containsKey(folderPath)) {
            return folders.get(folderPath);
        }

        String[] parts = folderPath.split("/");
        ScriptTreeNode currentParent = rootNode;
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);

            String pathStr = currentPath.toString();

            if (folders.containsKey(pathStr)) {
                currentParent = folders.get(pathStr);
            } else {
                ScriptTreeNode folderNode = new ScriptTreeNode(part);
                currentParent.add(folderNode);
                folders.put(pathStr, folderNode);
                currentParent = folderNode;
            }
        }

        return currentParent;
    }

    /**
     * Called when tree selection changes.
     */
    private void onTreeSelectionChanged() {
        TreePath path = scriptTree.getSelectionPath();

        if (path == null) {
            metadataPanel.clear();
            return;
        }

        Object node = path.getLastPathComponent();

        if (node instanceof ScriptTreeNode) {
            ScriptTreeNode scriptNode = (ScriptTreeNode) node;

            if (scriptNode.isScript()) {
                metadataPanel.displayMetadata(scriptNode.getScriptMetadata());
            } else {
                metadataPanel.clear();
            }
        }
    }

    /**
     * Loads the selected script into the editor.
     */
    private void loadSelectedScript() {
        TreePath path = scriptTree.getSelectionPath();

        if (path == null) {
            return;
        }

        Object node = path.getLastPathComponent();

        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;

        if (!scriptNode.isScript()) {
            return;  // Can't load a folder
        }

        // Check for unsaved changes
        if (changesTracker.isDirty()) {
            int choice = showUnsavedChangesDialog();

            if (choice == JOptionPane.YES_OPTION) {
                // Save current script
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                // Cancel loading
                return;
            }
            // NO_OPTION falls through to discard changes
        }

        // Load the script
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        loadScript(metadata.getName());
    }

    /**
     * Loads a script by name.
     */
    private void loadScript(String name) {
        if (restClient == null) {
            return;
        }

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(name);
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();
                    changesTracker.loadContent(script.getCode());
                    currentScript = convertToMetadata(script);
                    updateCurrentScriptLabel();
                    setStatus("Loaded: " + script.getName(), new Color(0, 128, 0));
                } catch (Exception e) {
                    LOGGER.error("Failed to load script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
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
     * Saves the current script.
     * If script metadata exists (already saved), does a quick save.
     * Otherwise, shows the save dialog.
     */
    private void saveCurrentScript() {
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

        // If script already has metadata, do a quick save
        if (currentScript != null && currentScript.getName() != null && !currentScript.getName().isEmpty()) {
            String name = currentScript.getName();
            String author = currentScript.getAuthor() != null ? currentScript.getAuthor() : "Unknown";
            String version = currentScript.getVersion() != null ? currentScript.getVersion() : "1.0";
            String folder = currentScript.getFolderPath() != null ? currentScript.getFolderPath() : "";
            String description = currentScript.getDescription() != null ? currentScript.getDescription() : "";

            saveScript(name, code, description, author, folder, version);
        } else {
            // New script - show save dialog
            saveScriptAs();
        }
    }

    /**
     * Shows the Save As dialog to save script with a new name.
     */
    private void saveScriptAs() {
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

        // Show save dialog
        showSaveDialog();
    }

    /**
     * Shows the save script dialog.
     */
    private void showSaveDialog() {
        // Apply dark theme to dialogs BEFORE showing them
        applyDialogDarkTheme();

        JTextField nameField = new JTextField(currentScript != null ? currentScript.getName() : "", 20);
        JTextField authorField = new JTextField(currentScript != null ? currentScript.getAuthor() : "Unknown", 20);
        JTextField versionField = new JTextField(currentScript != null ? currentScript.getVersion() : "1.0", 10);
        JTextField folderField = new JTextField(currentScript != null ? currentScript.getFolderPath() : "", 20);
        JTextField descField = new JTextField(currentScript != null ? currentScript.getDescription() : "", 30);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.add(new JLabel("Script Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);
        panel.add(new JLabel("Version:"));
        panel.add(versionField);
        panel.add(new JLabel("Folder Path:"));
        panel.add(folderField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);

        // Apply dark theme to panel components
        setComponentsDark(panel);

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
        String author = authorField.getText().trim();
        String version = versionField.getText().trim();
        String folder = folderField.getText().trim();
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

        // Validate script name
        if (!isValidName(name)) {
            showInvalidNameError(name);
            return;
        }

        saveScript(name, codeEditor.getText(), description, author, folder, version);
    }

    /**
     * Saves a script to the Gateway.
     */
    private void saveScript(String name, String code, String description,
                           String author, String folderPath, String version) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.saveScript(name, code, description, author, folderPath, version);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    changesTracker.markSaved();

                    // Update currentScript metadata for future quick saves
                    if (currentScript == null) {
                        currentScript = new ScriptMetadata();
                    }
                    currentScript.setName(name);
                    currentScript.setDescription(description);
                    currentScript.setAuthor(author);
                    currentScript.setFolderPath(folderPath);
                    currentScript.setVersion(version);

                    setStatus("Script saved: " + name, new Color(0, 128, 0));
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to save script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
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
     * Creates a new folder.
     */
    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(
                this,
                "Enter folder name:",
                "New Folder",
                JOptionPane.PLAIN_MESSAGE
        );

        if (folderName == null || folderName.trim().isEmpty()) {
            return;
        }

        String trimmedName = folderName.trim();

        // Validate folder name
        if (!isValidName(trimmedName)) {
            showInvalidNameError(trimmedName);
            return;
        }

        // Create folder in tree
        ScriptTreeNode newFolder = new ScriptTreeNode(trimmedName);
        rootNode.add(newFolder);
        treeModel.reload();
        scriptTree.expandPath(new TreePath(rootNode.getPath()));
    }

    /**
     * Creates a new script.
     */
    private void createNewScript() {
        // Check for unsaved changes
        if (changesTracker.isDirty()) {
            int choice = showUnsavedChangesDialog();

            if (choice == JOptionPane.YES_OPTION) {
                saveCurrentScript();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        // Clear editor for new script
        changesTracker.loadContent("# New Python Script\n\n");
        currentScript = null;
        updateCurrentScriptLabel();
        metadataPanel.clear();
        setStatus("New script", Color.BLUE);
    }

    /**
     * Shows context menu for tree items.
     */
    private void showContextMenu(MouseEvent e) {
        TreePath path = scriptTree.getPathForLocation(e.getX(), e.getY());

        if (path == null) {
            return;
        }

        scriptTree.setSelectionPath(path);
        Object node = path.getLastPathComponent();

        if (!(node instanceof ScriptTreeNode)) {
            return;
        }

        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
        JPopupMenu menu = new JPopupMenu();

        if (scriptNode.isScript()) {
            // Script context menu
            JMenuItem loadItem = new JMenuItem("Load");
            loadItem.addActionListener(ev -> loadSelectedScript());
            menu.add(loadItem);

            JMenuItem exportItem = new JMenuItem("Export...");
            exportItem.addActionListener(ev -> exportScript(scriptNode));
            menu.add(exportItem);

            JMenuItem renameItem = new JMenuItem("Rename...");
            renameItem.addActionListener(ev -> renameScript(scriptNode));
            menu.add(renameItem);

            menu.addSeparator();

            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(ev -> deleteScript(scriptNode));
            menu.add(deleteItem);

        } else {
            // Folder context menu
            JMenuItem newScriptItem = new JMenuItem("New Script Here");
            newScriptItem.addActionListener(ev -> createNewScript());
            menu.add(newScriptItem);

            JMenuItem newFolderItem = new JMenuItem("New Subfolder");
            newFolderItem.addActionListener(ev -> createNewFolder());
            menu.add(newFolderItem);

            // Only allow renaming non-root folders
            if (scriptNode != rootNode) {
                menu.addSeparator();

                JMenuItem renameFolderItem = new JMenuItem("Rename...");
                renameFolderItem.addActionListener(ev -> renameFolder(scriptNode));
                menu.add(renameFolderItem);
            }
        }

        menu.show(scriptTree, e.getX(), e.getY());
    }

    /**
     * Exports a script to a .py file.
     */
    private void exportScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();

        SwingWorker<SavedScript, Void> worker = new SwingWorker<SavedScript, Void>() {
            @Override
            protected SavedScript doInBackground() throws Exception {
                return restClient.loadScript(metadata.getName());
            }

            @Override
            protected void done() {
                try {
                    SavedScript script = get();

                    // Apply dark theme to file chooser dialog
                    applyDialogDarkTheme();

                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(script.getName() + ".py"));

                    int result = fileChooser.showSaveDialog(Python3IDE_v1_9.this);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();

                        try (FileWriter writer = new FileWriter(file)) {
                            writer.write(script.getCode());
                            setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to export script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
                            "Failed to export script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Renames a script.
     */
    private void renameScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();
        String oldName = metadata.getName();

        String newName = JOptionPane.showInputDialog(
                this,
                "Enter new name for script:",
                oldName
        );

        if (newName == null || newName.trim().isEmpty()) {
            return;  // User cancelled or entered empty name
        }

        final String finalNewName = newName.trim();

        // Validate new name
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }

        if (finalNewName.equals(oldName)) {
            return;  // No change
        }

        // Rename by: load script -> delete old -> save with new name
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private SavedScript script;

            @Override
            protected Void doInBackground() throws Exception {
                // Load the script
                script = restClient.loadScript(oldName);

                // Delete old script
                restClient.deleteScript(oldName);

                // Save with new name
                restClient.saveScript(
                        finalNewName,  // New name
                        script.getCode(),
                        script.getDescription(),
                        script.getAuthor(),
                        script.getFolderPath(),
                        script.getVersion()
                );

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Renamed '" + oldName + "' to '" + finalNewName + "'", new Color(0, 128, 0));
                    refreshScriptTree();

                    // Update current script metadata if this is the currently loaded script
                    if (currentScript != null && currentScript.getName().equals(oldName)) {
                        currentScript.setName(finalNewName);
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to rename script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
                            "Failed to rename script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Renames a folder.
     */
    private void renameFolder(ScriptTreeNode folderNode) {
        String oldName = folderNode.toString();
        String oldPath = getFolderPathForNode(folderNode);

        String newName = JOptionPane.showInputDialog(
                this,
                "Enter new name for folder:",
                oldName
        );

        if (newName == null || newName.trim().isEmpty()) {
            return;  // User cancelled or entered empty name
        }

        final String finalNewName = newName.trim();

        // Validate new name
        if (!isValidName(finalNewName)) {
            showInvalidNameError(finalNewName);
            return;
        }

        if (finalNewName.equals(oldName)) {
            return;  // No change
        }

        // Calculate new path
        String parentPath = "";
        if (folderNode.getParent() != rootNode && folderNode.getParent() != null) {
            parentPath = getFolderPathForNode((ScriptTreeNode) folderNode.getParent());
        }
        final String newPath = parentPath.isEmpty() ? finalNewName : parentPath + "/" + finalNewName;

        // Rename by updating all scripts in this folder (and subfolders)
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Get all scripts
                List<ScriptMetadata> allScripts = restClient.listScripts();

                // Find scripts that need to be moved
                for (ScriptMetadata script : allScripts) {
                    String scriptFolder = script.getFolderPath();
                    if (scriptFolder != null) {
                        // Check if script is in this folder or subfolder
                        if (scriptFolder.equals(oldPath) || scriptFolder.startsWith(oldPath + "/")) {
                            // Update the folder path
                            String updatedPath = scriptFolder.equals(oldPath) ?
                                    newPath :
                                    newPath + scriptFolder.substring(oldPath.length());

                            // Load full script and save with new path
                            SavedScript fullScript = restClient.loadScript(script.getName());
                            restClient.saveScript(
                                    fullScript.getName(),
                                    fullScript.getCode(),
                                    fullScript.getDescription(),
                                    fullScript.getAuthor(),
                                    updatedPath,
                                    fullScript.getVersion()
                            );
                        }
                    }
                }

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Renamed folder '" + oldName + "' to '" + finalNewName + "'", new Color(0, 128, 0));
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to rename folder", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
                            "Failed to rename folder: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Gets the full folder path for a folder node.
     */
    private String getFolderPathForNode(ScriptTreeNode folderNode) {
        if (folderNode == rootNode) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        Object[] pathArray = folderNode.getPath();

        // Skip root node (index 0)
        for (int i = 1; i < pathArray.length; i++) {
            if (path.length() > 0) {
                path.append("/");
            }
            path.append(pathArray[i].toString());
        }

        return path.toString();
    }

    /**
     * Deletes a script.
     */
    private void deleteScript(ScriptTreeNode scriptNode) {
        ScriptMetadata metadata = scriptNode.getScriptMetadata();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + metadata.getName() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                restClient.deleteScript(metadata.getName());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Deleted: " + metadata.getName(), new Color(0, 128, 0));
                    refreshScriptTree();
                } catch (Exception e) {
                    LOGGER.error("Failed to delete script", e);
                    JOptionPane.showMessageDialog(
                            Python3IDE_v1_9.this,
                            "Failed to delete script: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }

    /**
     * Imports a .py file into the script library.
     */
    private void importScript() {
        if (restClient == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please connect to a Gateway first",
                    "Not Connected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Apply dark theme to file chooser dialog
        applyDialogDarkTheme();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".py");
            }

            @Override
            public String getDescription() {
                return "Python Files (*.py)";
            }
        });

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                // Read file content
                String code = new String(Files.readAllBytes(file.toPath()));

                // Get script name without extension
                String fileName = file.getName();
                String scriptName = fileName.endsWith(".py") ?
                    fileName.substring(0, fileName.length() - 3) : fileName;

                // Show save dialog with imported content
                JTextField nameField = new JTextField(scriptName, 20);
                JTextField authorField = new JTextField(System.getProperty("user.name", "Unknown"), 20);
                JTextField versionField = new JTextField("1.0", 10);
                JTextField folderField = new JTextField("", 20);
                JTextField descField = new JTextField("Imported from " + fileName, 30);

                JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
                panel.add(new JLabel("Script Name:"));
                panel.add(nameField);
                panel.add(new JLabel("Author:"));
                panel.add(authorField);
                panel.add(new JLabel("Version:"));
                panel.add(versionField);
                panel.add(new JLabel("Folder Path:"));
                panel.add(folderField);
                panel.add(new JLabel("Description:"));
                panel.add(descField);

                int dialogResult = JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        "Import Script",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (dialogResult == JOptionPane.OK_OPTION) {
                    String name = nameField.getText().trim();
                    String author = authorField.getText().trim();
                    String version = versionField.getText().trim();
                    String folder = folderField.getText().trim();
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

                    // Save imported script
                    saveScript(name, code, description, author, folder, version);
                    setStatus("Imported: " + fileName, new Color(0, 128, 0));
                }

            } catch (IOException e) {
                LOGGER.error("Failed to import script", e);
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to import script: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Exports the current code in the editor to a .py file.
     */
    private void exportCurrentScript() {
        String code = codeEditor.getText().trim();

        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot export empty code",
                    "Empty Code",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Apply dark theme to file chooser dialog
        applyDialogDarkTheme();

        JFileChooser fileChooser = new JFileChooser();

        // Default filename based on current script name or generic
        String defaultName = (currentScript != null && currentScript.getName() != null) ?
                currentScript.getName() + ".py" : "script.py";
        fileChooser.setSelectedFile(new File(defaultName));

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Ensure .py extension
            if (!file.getName().endsWith(".py")) {
                file = new File(file.getAbsolutePath() + ".py");
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(code);
                setStatus("Exported: " + file.getName(), new Color(0, 128, 0));
                LOGGER.info("Exported script to: {}", file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to export script", e);
                JOptionPane.showMessageDialog(
                        this,
                        "Failed to export script: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Shows unsaved changes dialog.
     */
    private int showUnsavedChangesDialog() {
        return JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Do you want to save them?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Called when dirty state changes.
     */
    private void onDirtyStateChanged(boolean isDirty) {
        String title = "Python 3 IDE";

        if (isDirty) {
            title += " *";  // Indicate unsaved changes
        }

        // Update current script label to show unsaved changes indicator
        updateCurrentScriptLabel();

        // Could update window title or status here
        setStatus(isDirty ? "Unsaved changes" : "All changes saved",
                  isDirty ? Color.ORANGE : new Color(0, 128, 0));
    }

    /**
     * Updates the current script label to show the selected script name and folder path.
     */
    private void updateCurrentScriptLabel() {
        if (currentScript == null || currentScript.getName() == null || currentScript.getName().isEmpty()) {
            currentScriptLabel.setText("No script selected");
            currentScriptLabel.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        } else {
            StringBuilder labelText = new StringBuilder();

            // Add folder path if exists
            if (currentScript.getFolderPath() != null && !currentScript.getFolderPath().isEmpty()) {
                labelText.append(currentScript.getFolderPath()).append(" / ");
            }

            // Add script name
            labelText.append(currentScript.getName());

            // Add unsaved changes indicator
            if (changesTracker.isDirty()) {
                labelText.append(" *");
            }

            currentScriptLabel.setText(labelText.toString());
            currentScriptLabel.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }
    }

    // Theme Management

    /**
     * Maps user-friendly theme names to internal theme keys.
     */
    private String mapThemeNameToKey(String displayName) {
        switch (displayName) {
            case "Dark":
                return "dark";
            case "VS Code Dark+":
                return "vs";
            case "Monokai":
                return "monokai";
            case "Dracula":
                return "druid";
            case "Default (Light)":
                return "default";
            case "IntelliJ Light":
                return "idea";
            case "Eclipse":
                return "eclipse";
            default:
                return "dark";
        }
    }

    /**
     * Applies a theme to the editor and entire IDE.
     */
    private void applyTheme(String themeName) {
        try {
            Theme theme;
            boolean isDarkTheme = false;

            switch (themeName.toLowerCase()) {
                case "dark":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                    isDarkTheme = true;
                    break;
                case "monokai":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
                    isDarkTheme = true;
                    break;
                case "eclipse":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
                    isDarkTheme = false;
                    break;
                case "idea":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));
                    isDarkTheme = false;
                    break;
                case "vs":
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
                    isDarkTheme = true;
                    break;
                case "druid":  // Dracula-like theme
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/druid.xml"));
                    isDarkTheme = true;
                    break;
                default:  // "default" or "light"
                    theme = Theme.load(getClass().getResourceAsStream(
                            "/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                    isDarkTheme = false;
                    break;
            }

            // Apply theme to code editor
            theme.apply(codeEditor);

            // Apply theme colors to output and error areas
            if (isDarkTheme) {
                // Apply scrollbar theming
                applyDarkScrollbarTheme();

                // Output/Error areas
                outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
                outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

                errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
                errorArea.setForeground(ModernTheme.ERROR);
                errorArea.setCaretColor(ModernTheme.ERROR);

                // Tree
                scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
                scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);

                // Panels
                updateComponent(this, ModernTheme.BACKGROUND_DARK);

                // Apply dark theme to popup dialogs
                applyDarkDialogTheme();
            } else {
                // Apply light scrollbar theming
                applyLightScrollbarTheme();

                // Output/Error areas
                outputArea.setBackground(Color.WHITE);
                outputArea.setForeground(Color.BLACK);
                outputArea.setCaretColor(Color.BLACK);

                errorArea.setBackground(Color.WHITE);
                errorArea.setForeground(new Color(180, 0, 0));
                errorArea.setCaretColor(new Color(180, 0, 0));

                // Tree
                scriptTree.setBackground(Color.WHITE);
                scriptTree.setForeground(Color.BLACK);

                // Panels
                updateComponent(this, Color.WHITE);

                // Apply light theme to popup dialogs
                applyLightDialogTheme();
            }

            // Force repaint of all components
            SwingUtilities.updateComponentTreeUI(this);

            // Force update of all scrollbar UI delegates
            updateScrollPaneTheme(this, isDarkTheme);

            // Force update of all JSplitPane dividers (Issue 8 - v1.17.1)
            updateSplitPaneDividers(this, isDarkTheme);

            currentTheme = themeName;

            // Save preference
            Preferences prefs = Preferences.userNodeForPackage(Python3IDE_v1_9.class);
            prefs.put(PREF_THEME, themeName);

            setStatus("Theme changed: " + themeName, new Color(0, 128, 0));
            LOGGER.info("Applied theme: {}", themeName);

        } catch (IOException e) {
            LOGGER.error("Failed to apply theme: {}", themeName, e);
            setStatus("Failed to apply theme: " + themeName, Color.RED);
        }
    }

    /**
     * Applies dark theme styling to popup dialogs (JOptionPane).
     *
     * v1.17.1: Enhanced with ComboBox and additional component theming
     */
    private void applyDarkDialogTheme() {
        UIManager.put("OptionPane.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("OptionPane.messageForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Panel.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("TextField.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TextField.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.caretForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.inactiveForeground", ModernTheme.FOREGROUND_SECONDARY);
        UIManager.put("Label.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Label.disabledForeground", ModernTheme.FOREGROUND_SECONDARY);
        UIManager.put("Button.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("Button.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ComboBox.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ComboBox.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ModernTheme.INFO);
        UIManager.put("ComboBox.selectionForeground", ModernTheme.FOREGROUND_PRIMARY);
    }

    /**
     * Applies light theme styling to popup dialogs (JOptionPane).
     *
     * v1.17.1: Enhanced with ComboBox and additional component theming
     */
    private void applyLightDialogTheme() {
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Color.BLACK);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", Color.BLACK);
        UIManager.put("TextField.caretForeground", Color.BLACK);
        UIManager.put("TextField.inactiveForeground", new Color(128, 128, 128));
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put("Label.disabledForeground", new Color(128, 128, 128));
        UIManager.put("Button.background", new Color(238, 238, 238));
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", new Color(184, 207, 229));
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
    }

    /**
     * Applies dark theme styling to scrollbars.
     */
    private void applyDarkScrollbarTheme() {
        UIManager.put("ScrollBar.track", ModernTheme.BACKGROUND_DARK);
        UIManager.put("ScrollBar.thumb", ModernTheme.BORDER_DEFAULT);
        UIManager.put("ScrollBar.thumbDarkShadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ScrollBar.thumbHighlight", ModernTheme.FOREGROUND_SECONDARY);
        UIManager.put("ScrollBar.thumbShadow", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("ScrollBar.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("ScrollBar.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("ScrollPane.background", ModernTheme.BACKGROUND_DARK);
        UIManager.put("Viewport.background", ModernTheme.BACKGROUND_DARK);
    }

    /**
     * Applies light theme styling to scrollbars.
     */
    private void applyLightScrollbarTheme() {
        UIManager.put("ScrollBar.track", Color.WHITE);
        UIManager.put("ScrollBar.thumb", new Color(200, 200, 200));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(150, 150, 150));
        UIManager.put("ScrollBar.thumbHighlight", new Color(230, 230, 230));
        UIManager.put("ScrollBar.thumbShadow", new Color(180, 180, 180));
        UIManager.put("ScrollBar.background", Color.WHITE);
        UIManager.put("ScrollBar.foreground", Color.BLACK);
        UIManager.put("ScrollPane.background", Color.WHITE);
        UIManager.put("Viewport.background", Color.WHITE);
    }

    /**
     * Applies dialog dark theme via UIManager for JOptionPane and JFileChooser components.
     * Must be called BEFORE showing any JOptionPane dialog or JFileChooser.
     */
    private void applyDialogDarkTheme() {
        // JOptionPane dark theme
        UIManager.put("OptionPane.background", new Color(43, 43, 43));
        UIManager.put("Panel.background", new Color(43, 43, 43));
        UIManager.put("OptionPane.messageForeground", new Color(224, 224, 224));
        UIManager.put("TextField.background", new Color(30, 30, 30));
        UIManager.put("TextField.foreground", new Color(224, 224, 224));
        UIManager.put("TextField.caretForeground", new Color(224, 224, 224));
        UIManager.put("Button.background", new Color(60, 63, 65));
        UIManager.put("Button.foreground", new Color(224, 224, 224));
        UIManager.put("Label.foreground", new Color(224, 224, 224));

        // JFileChooser dark theme
        UIManager.put("FileChooser.background", new Color(43, 43, 43));
        UIManager.put("FileChooser.foreground", new Color(224, 224, 224));
        UIManager.put("List.background", new Color(30, 30, 30));
        UIManager.put("List.foreground", new Color(224, 224, 224));
        UIManager.put("List.selectionBackground", new Color(60, 63, 65));
        UIManager.put("List.selectionForeground", new Color(224, 224, 224));
        UIManager.put("Table.background", new Color(30, 30, 30));
        UIManager.put("Table.foreground", new Color(224, 224, 224));
    }

    /**
     * Recursively applies dark theme colors to all components in a container.
     * This ensures dialog panels, labels, and text fields have proper dark styling.
     *
     * @param container the container to apply styling to
     */
    private static void setComponentsDark(Container container) {
        container.setBackground(new Color(43, 43, 43));
        Component[] components = container.getComponents();
        for (Component component : components) {
            if (component instanceof JTextField || component instanceof JTextArea) {
                component.setBackground(new Color(30, 30, 30));
                component.setForeground(new Color(224, 224, 224));
                if (component instanceof JTextField) {
                    ((JTextField) component).setCaretColor(new Color(224, 224, 224));
                }
            } else if (component instanceof JLabel) {
                component.setForeground(new Color(224, 224, 224));
            } else if (component instanceof JPanel) {
                component.setBackground(new Color(43, 43, 43));
            } else if (component instanceof JButton) {
                component.setBackground(new Color(60, 63, 65));
                component.setForeground(new Color(224, 224, 224));
            }
            if (component instanceof Container) {
                setComponentsDark((Container) component);
            }
        }
    }

    /**
     * Recursively updates component backgrounds.
     * Traverses the component tree and applies theme colors to all panels.
     *
     * @param comp the component to update
     * @param background the background color to apply
     */
    private void updateComponent(Component comp, Color background) {
        if (comp instanceof JPanel) {
            comp.setBackground(background);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateComponent(child, background);
            }
        }
    }

    /**
     * Recursively updates all scrollbar UI delegates to match the current theme.
     * Forces scrollbars to pick up the theme changes by updating their UI components.
     *
     * @param comp the component to traverse
     * @param isDarkTheme true if dark theme, false if light theme
     */
    private void updateScrollPaneTheme(Component comp, boolean isDarkTheme) {
        if (comp instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) comp;

            // Force update scrollbar UI delegates
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();

            if (verticalBar != null) {
                verticalBar.updateUI();
                if (isDarkTheme) {
                    verticalBar.setBackground(ModernTheme.BACKGROUND_DARK);
                    verticalBar.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                } else {
                    verticalBar.setBackground(Color.WHITE);
                    verticalBar.setForeground(Color.BLACK);
                }
            }

            if (horizontalBar != null) {
                horizontalBar.updateUI();
                if (isDarkTheme) {
                    horizontalBar.setBackground(ModernTheme.BACKGROUND_DARK);
                    horizontalBar.setForeground(ModernTheme.FOREGROUND_PRIMARY);
                } else {
                    horizontalBar.setBackground(Color.WHITE);
                    horizontalBar.setForeground(Color.BLACK);
                }
            }

            // Update viewport background
            scrollPane.getViewport().setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
            scrollPane.setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
        }

        // Recursively traverse child components
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateScrollPaneTheme(child, isDarkTheme);
            }
        }
    }

    /**
     * Recursively updates all JSplitPane dividers to match the current theme.
     * Forces dividers to pick up the theme colors by updating their background colors.
     *
     * @param comp the component to traverse
     * @param isDarkTheme true if dark theme, false if light theme
     *
     * v1.17.1: Fix for Issue 8 - ensure dividers match theme
     */
    private void updateSplitPaneDividers(Component comp, boolean isDarkTheme) {
        if (comp instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) comp;

            // Update divider colors
            if (isDarkTheme) {
                splitPane.setDividerSize(8);
                splitPane.setBorder(null);
                // Access the divider UI component
                if (splitPane.getUI() instanceof BasicSplitPaneUI) {
                    ((BasicSplitPaneUI) splitPane.getUI()).getDivider().setBackground(ModernTheme.BACKGROUND_DARKER);
                }
            } else {
                splitPane.setDividerSize(8);
                splitPane.setBorder(null);
                // Access the divider UI component
                if (splitPane.getUI() instanceof BasicSplitPaneUI) {
                    ((BasicSplitPaneUI) splitPane.getUI()).getDivider().setBackground(new Color(200, 200, 200));
                }
            }

            splitPane.updateUI();
        }

        // Recursively traverse child components
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateSplitPaneDividers(child, isDarkTheme);
            }
        }
    }

    // Find/Replace Management

    /**
     * Shows the Find dialog.
     */
    private void showFindDialog() {
        if (findDialog == null) {
            // Lazy initialization
            findDialog = new FindDialog((Frame) SwingUtilities.getWindowAncestor(this), new SearchListenerImpl());
            findDialog.setSearchContext(new SearchContext());
        }

        findDialog.setVisible(true);
    }

    /**
     * Shows the Replace dialog.
     */
    private void showReplaceDialog() {
        if (replaceDialog == null) {
            // Lazy initialization
            replaceDialog = new ReplaceDialog((Frame) SwingUtilities.getWindowAncestor(this), new SearchListenerImpl());
            replaceDialog.setSearchContext(new SearchContext());
        }

        replaceDialog.setVisible(true);
    }

    /**
     * Search listener implementation for Find/Replace dialogs.
     */
    private class SearchListenerImpl implements SearchListener {
        @Override
        public void searchEvent(org.fife.rsta.ui.search.SearchEvent e) {
            org.fife.rsta.ui.search.SearchEvent.Type type = e.getType();
            SearchContext context = e.getSearchContext();

            switch (type) {
                case FIND:
                    org.fife.ui.rtextarea.SearchResult result = SearchEngine.find(codeEditor, context);
                    if (!result.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;

                case REPLACE:
                    org.fife.ui.rtextarea.SearchResult replaceResult = SearchEngine.replace(codeEditor, context);
                    if (!replaceResult.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;

                case REPLACE_ALL:
                    org.fife.ui.rtextarea.SearchResult replaceAllResult = SearchEngine.replaceAll(codeEditor, context);
                    setStatus("Replaced " + replaceAllResult.getCount() + " occurrences", new Color(0, 128, 0));
                    break;

                case MARK_ALL:
                    org.fife.ui.rtextarea.SearchResult markResult = SearchEngine.markAll(codeEditor, context);
                    if (!markResult.wasFound()) {
                        setStatus("Text not found: " + context.getSearchFor(), Color.ORANGE);
                    }
                    break;
            }
        }

        @Override
        public String getSelectedText() {
            return codeEditor.getSelectedText();
        }
    }

    /**
     * Changes font size.
     */
    private void changeFontSize(int delta) {
        setFontSize(fontSize + delta);
    }

    /**
     * Sets font size.
     */
    private void setFontSize(int newSize) {
        if (newSize < 8 || newSize > 24) {
            return;  // Reasonable bounds
        }

        fontSize = newSize;
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, fontSize));

        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(Python3IDE_v1_9.class);
        prefs.putInt(PREF_FONT_SIZE, fontSize);

        LOGGER.info("Font size: {}", fontSize);
    }

    /**
     * Converts SavedScript to ScriptMetadata.
     */
    private ScriptMetadata convertToMetadata(SavedScript script) {
        ScriptMetadata metadata = new ScriptMetadata();
        metadata.setId(script.getId());
        metadata.setName(script.getName());
        metadata.setDescription(script.getDescription());
        metadata.setAuthor(script.getAuthor());
        metadata.setCreatedDate(script.getCreatedDate());
        metadata.setLastModified(script.getLastModified());
        metadata.setFolderPath(script.getFolderPath());
        metadata.setVersion(script.getVersion());
        return metadata;
    }

    // Public accessor methods (for external access if needed)

    public RSyntaxTextArea getCodeEditor() {
        return codeEditor;
    }

    public JTextArea getOutputArea() {
        return outputArea;
    }

    public JTextArea getErrorArea() {
        return errorArea;
    }

    /**
     * Transfer handler for drag and drop in the script tree.
     */
    private class ScriptTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof JTree) {
                JTree tree = (JTree) c;
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof ScriptTreeNode) {
                        ScriptTreeNode scriptNode = (ScriptTreeNode) node;
                        // Allow dragging scripts and folders (but not root)
                        if (scriptNode.isScript()) {
                            return new StringSelection("SCRIPT:" + scriptNode.getScriptMetadata().getName());
                        } else if (scriptNode.isFolder() && scriptNode != rootNode) {
                            return new StringSelection("FOLDER:" + getFolderPathForNode(scriptNode));
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }

            // Check if we're dropping on a folder
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            TreePath path = dropLocation.getPath();

            if (path == null) {
                return false;
            }

            Object targetNode = path.getLastPathComponent();
            if (!(targetNode instanceof ScriptTreeNode)) {
                return false;
            }

            ScriptTreeNode target = (ScriptTreeNode) targetNode;
            // Can only drop on folders or root
            return target.isFolder() || target == rootNode;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                // Get the data being dragged
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

                // Get the target folder
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                ScriptTreeNode targetFolder = (ScriptTreeNode) path.getLastPathComponent();

                // Calculate new folder path
                String newFolderPath;
                if (targetFolder == rootNode) {
                    newFolderPath = "";  // Root level
                } else {
                    newFolderPath = getFolderPath(targetFolder);
                }

                // Determine if we're moving a script or folder
                if (data.startsWith("SCRIPT:")) {
                    String scriptName = data.substring(7);  // Remove "SCRIPT:" prefix
                    moveScript(scriptName, newFolderPath);
                } else if (data.startsWith("FOLDER:")) {
                    String folderPath = data.substring(7);  // Remove "FOLDER:" prefix

                    // Prevent moving folder into itself or its own subfolder
                    if (newFolderPath.equals(folderPath) || newFolderPath.startsWith(folderPath + "/")) {
                        setStatus("Cannot move folder into itself", Color.ORANGE);
                        return false;
                    }

                    moveFolder(folderPath, newFolderPath);
                } else {
                    // Backward compatibility - assume it's a script name without prefix
                    moveScript(data, newFolderPath);
                }

                return true;

            } catch (Exception e) {
                LOGGER.error("Failed to import", e);
                setStatus("Failed to move: " + e.getMessage(), Color.RED);
                return false;
            }
        }

        /**
         * Gets the full folder path for a folder node.
         */
        private String getFolderPath(ScriptTreeNode folderNode) {
            if (folderNode == rootNode) {
                return "";
            }

            StringBuilder path = new StringBuilder();
            Object[] pathArray = folderNode.getPath();

            // Skip root node (index 0)
            for (int i = 1; i < pathArray.length; i++) {
                if (path.length() > 0) {
                    path.append("/");
                }
                path.append(pathArray[i].toString());
            }

            return path.toString();
        }

        /**
         * Moves a script to a new folder.
         */
        private void moveScript(String scriptName, String newFolderPath) {
            if (restClient == null) {
                return;
            }

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                private SavedScript script;

                @Override
                protected Void doInBackground() throws Exception {
                    // Load the script
                    script = restClient.loadScript(scriptName);

                    // Update folder path
                    restClient.saveScript(
                            script.getName(),
                            script.getCode(),
                            script.getDescription(),
                            script.getAuthor(),
                            newFolderPath,  // New folder path
                            script.getVersion()
                    );

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        setStatus("Moved '" + scriptName + "' to " +
                                (newFolderPath.isEmpty() ? "root" : newFolderPath), new Color(0, 128, 0));
                        refreshScriptTree();
                    } catch (Exception e) {
                        LOGGER.error("Failed to move script", e);
                        JOptionPane.showMessageDialog(
                                Python3IDE_v1_9.this,
                                "Failed to move script: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };

            worker.execute();
        }

        /**
         * Moves a folder to a new parent folder.
         */
        private void moveFolder(String oldFolderPath, String newParentPath) {
            if (restClient == null) {
                return;
            }

            // Calculate the new folder path
            // Extract the folder name from the old path
            String folderName;
            int lastSlash = oldFolderPath.lastIndexOf('/');
            if (lastSlash >= 0) {
                folderName = oldFolderPath.substring(lastSlash + 1);
            } else {
                folderName = oldFolderPath;
            }

            // Combine with new parent path
            final String newFolderPath = newParentPath.isEmpty() ? folderName : newParentPath + "/" + folderName;

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Get all scripts
                    List<ScriptMetadata> allScripts = restClient.listScripts();

                    // Find scripts that need to be moved
                    for (ScriptMetadata script : allScripts) {
                        String scriptFolder = script.getFolderPath();
                        if (scriptFolder != null) {
                            // Check if script is in this folder or subfolder
                            if (scriptFolder.equals(oldFolderPath) || scriptFolder.startsWith(oldFolderPath + "/")) {
                                // Update the folder path
                                String updatedPath = scriptFolder.equals(oldFolderPath) ?
                                        newFolderPath :
                                        newFolderPath + scriptFolder.substring(oldFolderPath.length());

                                // Load full script and save with new path
                                SavedScript fullScript = restClient.loadScript(script.getName());
                                restClient.saveScript(
                                        fullScript.getName(),
                                        fullScript.getCode(),
                                        fullScript.getDescription(),
                                        fullScript.getAuthor(),
                                        updatedPath,
                                        fullScript.getVersion()
                                );
                            }
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        setStatus("Moved folder '" + folderName + "' to " +
                                (newParentPath.isEmpty() ? "root" : newParentPath), new Color(0, 128, 0));
                        refreshScriptTree();
                    } catch (Exception e) {
                        LOGGER.error("Failed to move folder", e);
                        JOptionPane.showMessageDialog(
                                Python3IDE_v1_9.this,
                                "Failed to move folder: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };

            worker.execute();
        }
    }
}
