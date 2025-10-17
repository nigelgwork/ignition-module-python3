# Developer Extension Guide - Python3IDE v2.0

**Version:** 2.0.0+
**Last Updated:** 2025-10-17
**Author:** Claude Code

## Overview

This guide shows developers how to extend Python3IDE v2.0 with new features using the refactored architecture.

### Prerequisites

- Familiarity with Java and Swing
- Understanding of Ignition Module SDK
- Knowledge of v2.0 architecture (see [V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md))

---

## Extension Patterns

### Pattern 1: Adding a New Manager

**When to use:** Adding new business logic that doesn't depend on UI

**Example:** Adding a code formatter manager

#### Step 1: Create Manager Class

```java
package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.Python3RestClient;
import java.io.IOException;

/**
 * Manages code formatting operations.
 *
 * Responsibilities:
 * - Format Python code via Gateway API
 * - Apply style guides (PEP 8, Black, etc.)
 * - Validate formatting results
 */
public class CodeFormatterManager {

    private final Python3RestClient restClient;
    private String currentStyle = "pep8";  // Default style

    /**
     * Creates code formatter manager.
     *
     * @param restClient REST client for Gateway communication
     */
    public CodeFormatterManager(Python3RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Formats Python code according to selected style.
     *
     * @param code Python code to format
     * @return formatted code
     * @throws IOException if formatting fails
     */
    public String formatCode(String code) throws IOException {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }

        // Call Gateway REST API for formatting
        // POST /api/v1/format-code
        FormattingResult result = restClient.formatCode(code, currentStyle);

        if (result.isSuccess()) {
            return result.getFormattedCode();
        } else {
            throw new IOException("Formatting failed: " + result.getError());
        }
    }

    /**
     * Sets formatting style.
     *
     * @param style Style name ("pep8", "black", etc.)
     */
    public void setStyle(String style) {
        this.currentStyle = style;
    }

    /**
     * Gets current formatting style.
     *
     * @return current style
     */
    public String getStyle() {
        return currentStyle;
    }

    /**
     * Lists available formatting styles.
     *
     * @return list of style names
     */
    public String[] getAvailableStyles() {
        return new String[] {"pep8", "black", "google", "custom"};
    }
}
```

#### Step 2: Add to Python3IDE_v2

```java
public class Python3IDE_v2 extends JPanel {

    // Add manager field
    private CodeFormatterManager formatterManager;

    public Python3IDE_v2(DesignerContext context) {
        // ... existing initialization

        // Initialize formatter manager in wireUpEvents()
        // after connection is established
    }

    private void connectToGateway() {
        String url = gatewayUrlField.getText().trim();
        boolean connected = connectionManager.connect(url);

        if (connected) {
            scriptManager = new ScriptManager(connectionManager.getRestClient());

            // Initialize formatter manager
            formatterManager = new CodeFormatterManager(connectionManager.getRestClient());

            // ... rest of connection logic
        }
    }

    // Add menu item or button to trigger formatting
    private void formatCode() {
        if (!connectionManager.isConnected()) {
            statusBar.setStatus("Not connected to Gateway", ModernStatusBar.MessageType.WARNING);
            return;
        }

        String code = editorPanel.getCode();

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return formatterManager.formatCode(code);
            }

            @Override
            protected void done() {
                try {
                    String formatted = get();
                    editorPanel.setCode(formatted);
                    statusBar.setStatus("Code formatted successfully", ModernStatusBar.MessageType.SUCCESS);
                } catch (Exception e) {
                    statusBar.setStatus("Formatting failed: " + e.getMessage(),
                                       ModernStatusBar.MessageType.ERROR);
                }
            }
        };

        worker.execute();
    }
}
```

#### Step 3: Add Unit Tests

```java
package com.inductiveautomation.ignition.examples.python3.designer.managers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CodeFormatterManagerTest {

    @Test
    public void testFormatCode() throws IOException {
        // Mock REST client
        Python3RestClient mockClient = mock(Python3RestClient.class);
        FormattingResult mockResult = new FormattingResult(true, "formatted code", null);
        when(mockClient.formatCode(anyString(), anyString())).thenReturn(mockResult);

        // Test formatter
        CodeFormatterManager manager = new CodeFormatterManager(mockClient);
        String result = manager.formatCode("unformatted code");

        assertEquals("formatted code", result);
        verify(mockClient).formatCode("unformatted code", "pep8");
    }

    @Test
    public void testSetStyle() {
        Python3RestClient mockClient = mock(Python3RestClient.class);
        CodeFormatterManager manager = new CodeFormatterManager(mockClient);

        manager.setStyle("black");

        assertEquals("black", manager.getStyle());
    }

    @Test
    public void testGetAvailableStyles() {
        Python3RestClient mockClient = mock(Python3RestClient.class);
        CodeFormatterManager manager = new CodeFormatterManager(mockClient);

        String[] styles = manager.getAvailableStyles();

        assertNotNull(styles);
        assertTrue(styles.length > 0);
        assertTrue(Arrays.asList(styles).contains("pep8"));
    }
}
```

---

### Pattern 2: Adding a New UI Panel

**When to use:** Adding new visual component

**Example:** Adding a code outline panel

#### Step 1: Create Panel Class

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel displaying code outline (classes, functions, etc.).
 *
 * Responsibilities:
 * - Display code structure as tree
 * - Allow navigation to code elements
 * - Update when code changes
 */
public class CodeOutlinePanel extends JPanel {

    private final JTree outlineTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private Consumer<Integer> lineNavigationListener;

    public CodeOutlinePanel() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);
        setBorder(BorderFactory.createTitledBorder("Code Outline"));

        // Create tree
        rootNode = new DefaultMutableTreeNode("Script");
        treeModel = new DefaultTreeModel(rootNode);
        outlineTree = new JTree(treeModel);

        // Style tree
        outlineTree.setBackground(ModernTheme.TREE_BACKGROUND);
        outlineTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outlineTree.setFont(ModernTheme.FONT_REGULAR);

        // Add selection listener
        outlineTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) outlineTree.getLastSelectedPathComponent();

            if (node != null && node.getUserObject() instanceof CodeElement) {
                CodeElement element = (CodeElement) node.getUserObject();
                if (lineNavigationListener != null) {
                    lineNavigationListener.accept(element.getLine());
                }
            }
        });

        // Add to panel
        JScrollPane scrollPane = new JScrollPane(outlineTree);
        scrollPane.setBackground(ModernTheme.TREE_BACKGROUND);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates outline based on code structure.
     *
     * @param elements list of code elements (classes, functions, etc.)
     */
    public void updateOutline(List<CodeElement> elements) {
        rootNode.removeAllChildren();

        for (CodeElement element : elements) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(element);
            rootNode.add(node);

            // Add nested elements (methods, etc.)
            for (CodeElement child : element.getChildren()) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
                node.add(childNode);
            }
        }

        treeModel.reload();
        outlineTree.expandRow(0);  // Expand root
    }

    /**
     * Sets callback for line navigation.
     *
     * @param listener callback accepting line number
     */
    public void setLineNavigationListener(Consumer<Integer> listener) {
        this.lineNavigationListener = listener;
    }

    /**
     * Gets tree for theming.
     *
     * @return outline tree
     */
    public JTree getTree() {
        return outlineTree;
    }

    /**
     * Represents a code element (class, function, etc.).
     */
    public static class CodeElement {
        private final String name;
        private final String type;  // "class", "function", "variable"
        private final int line;
        private final List<CodeElement> children;

        public CodeElement(String name, String type, int line, List<CodeElement> children) {
            this.name = name;
            this.type = type;
            this.line = line;
            this.children = children;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public int getLine() {
            return line;
        }

        public List<CodeElement> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return name + " (" + type + ")";
        }
    }
}
```

#### Step 2: Add to Python3IDE_v2

```java
public class Python3IDE_v2 extends JPanel {

    // Add panel field
    private final CodeOutlinePanel outlinePanel;

    public Python3IDE_v2(DesignerContext context) {
        // ... existing initialization

        // Initialize outline panel
        this.outlinePanel = new CodeOutlinePanel();

        // ... rest of initialization
    }

    private void layoutComponents() {
        // ... existing layout code

        // Add outline panel to sidebar
        JSplitPane sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        sidebarSplit.setTopComponent(treePanel);

        // Add outline panel below tree panel
        JSplitPane outlineSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        outlineSplit.setTopComponent(metadataPanel);
        outlineSplit.setBottomComponent(outlinePanel);
        outlineSplit.setDividerLocation(280);
        outlineSplit.setResizeWeight(0.5);

        sidebarSplit.setBottomComponent(outlineSplit);
        sidebarSplit.setDividerLocation(400);
        sidebarSplit.setResizeWeight(0.5);

        // ... rest of layout
    }

    private void wireUpEvents() {
        // ... existing event wiring

        // Wire outline navigation
        outlinePanel.setLineNavigationListener(line -> {
            // Navigate editor to specified line
            editorPanel.getCodeEditor().setCaretPosition(
                editorPanel.getCodeEditor().getDocument().getDefaultRootElement()
                    .getElement(line - 1).getStartOffset()
            );
        });

        // Update outline when code changes
        editorPanel.getCodeEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleOutlineUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleOutlineUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleOutlineUpdate();
            }
        });
    }

    private void scheduleOutlineUpdate() {
        // Debounce updates (wait 500ms after last change)
        if (outlineUpdateTimer != null) {
            outlineUpdateTimer.stop();
        }

        outlineUpdateTimer = new Timer(500, e -> updateOutline());
        outlineUpdateTimer.setRepeats(false);
        outlineUpdateTimer.start();
    }

    private void updateOutline() {
        String code = editorPanel.getCode();

        SwingWorker<List<CodeOutlinePanel.CodeElement>, Void> worker =
            new SwingWorker<List<CodeOutlinePanel.CodeElement>, Void>() {
                @Override
                protected List<CodeOutlinePanel.CodeElement> doInBackground() throws Exception {
                    // Parse code structure (call Gateway API or use local parser)
                    return connectionManager.getCodeOutline(code);
                }

                @Override
                protected void done() {
                    try {
                        List<CodeOutlinePanel.CodeElement> elements = get();
                        outlinePanel.updateOutline(elements);
                    } catch (Exception e) {
                        // Handle error silently (outline is non-critical)
                        LOGGER.debug("Failed to update code outline", e);
                    }
                }
            };

        worker.execute();
    }
}
```

---

### Pattern 3: Adding a Toolbar Action

**When to use:** Adding a button/menu item that triggers an action

**Example:** Adding "Run in Debug Mode" button

#### Step 1: Create Action Method

```java
public class Python3IDE_v2 extends JPanel {

    private JButton debugButton;

    private void initComponents() {
        // ... existing components

        debugButton = new JButton("\uD83D\uDC1E Debug");  // Bug emoji
        debugButton.setFont(ModernTheme.FONT_REGULAR);
        debugButton.setEnabled(false);
        debugButton.setToolTipText("Run code in debug mode with breakpoints");
    }

    private void wireUpEvents() {
        // ... existing event wiring

        debugButton.addActionListener(e -> executeCodeInDebugMode());
    }

    private void executeCodeInDebugMode() {
        if (!connectionManager.isConnected()) {
            statusBar.setStatus("Not connected to Gateway",
                               ModernStatusBar.MessageType.WARNING);
            return;
        }

        String code = editorPanel.getCode().trim();

        if (code.isEmpty()) {
            statusBar.setStatus("No code to debug",
                               ModernStatusBar.MessageType.WARNING);
            return;
        }

        // Disable buttons
        debugButton.setEnabled(false);
        executeButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusBar.setStatus("Debugging...", ModernStatusBar.MessageType.INFO);

        // Create debug worker
        SwingWorker<DebugResult, Void> worker = new SwingWorker<DebugResult, Void>() {
            @Override
            protected DebugResult doInBackground() throws Exception {
                // Call Gateway debug API
                return connectionManager.executeInDebugMode(code, new HashMap<>());
            }

            @Override
            protected void done() {
                try {
                    DebugResult result = get();
                    handleDebugResult(result);
                } catch (Exception e) {
                    handleDebugError(e);
                } finally {
                    debugButton.setEnabled(true);
                    executeButton.setEnabled(true);
                    progressBar.setVisible(false);
                }
            }
        };

        worker.execute();
    }

    private void handleDebugResult(DebugResult result) {
        // Display debug output
        editorPanel.setOutput(result.getOutput());

        // Highlight breakpoint lines
        for (int lineNum : result.getBreakpoints()) {
            highlightLine(lineNum);
        }

        // Show variables panel
        if (result.getVariables() != null) {
            showVariablesPanel(result.getVariables());
        }

        statusBar.setStatus(String.format("Debug completed (%d breakpoints)",
                                         result.getBreakpoints().size()),
                           ModernStatusBar.MessageType.SUCCESS);
    }
}
```

#### Step 2: Add to Toolbar

```java
private JPanel createTopPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    // ... existing panel code

    // Left: Connection + Execution
    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    leftPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
    leftPanel.add(new JLabel("URL:"));
    leftPanel.add(gatewayUrlField);
    leftPanel.add(connectButton);
    leftPanel.add(executeButton);
    leftPanel.add(debugButton);  // ✅ Add debug button
    leftPanel.add(saveButton);
    leftPanel.add(loadButton);
    leftPanel.add(progressBar);

    // ... rest of panel code
}
```

---

### Pattern 4: Adding a Settings Dialog

**When to use:** Adding configuration options

**Example:** Adding module settings dialog

#### Step 1: Create Settings Dialog

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

/**
 * Settings dialog for Python3IDE configuration.
 */
public class SettingsDialog extends JDialog {

    private final JComboBox<String> themeComboBox;
    private final JSpinner poolSizeSpinner;
    private final JCheckBox autoSaveCheckBox;
    private final JCheckBox syntaxCheckCheckBox;
    private boolean accepted = false;

    public SettingsDialog(Window parent) {
        super(parent, "Python3IDE Settings", ModalityType.APPLICATION_MODAL);

        // Create components
        themeComboBox = new JComboBox<>(new String[] {
            "Dark", "VS Code Dark+", "Monokai", "Light"
        });

        poolSizeSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        autoSaveCheckBox = new JCheckBox("Auto-save scripts", true);
        syntaxCheckCheckBox = new JCheckBox("Real-time syntax checking", true);

        // Layout
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Settings grid
        JPanel settingsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        settingsPanel.add(new JLabel("Theme:"));
        settingsPanel.add(themeComboBox);
        settingsPanel.add(new JLabel("Pool Size:"));
        settingsPanel.add(poolSizeSpinner);
        settingsPanel.add(new JLabel("Auto-save:"));
        settingsPanel.add(autoSaveCheckBox);
        settingsPanel.add(new JLabel("Syntax Checking:"));
        settingsPanel.add(syntaxCheckCheckBox);

        contentPanel.add(settingsPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            accepted = true;
            dispose();
        });

        cancelButton.addActionListener(e -> {
            accepted = false;
            dispose();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(contentPanel);
        setSize(400, 250);
        setLocationRelativeTo(parent);
    }

    // Getters
    public String getSelectedTheme() {
        return (String) themeComboBox.getSelectedItem();
    }

    public int getPoolSize() {
        return (Integer) poolSizeSpinner.getValue();
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveCheckBox.isSelected();
    }

    public boolean isSyntaxCheckingEnabled() {
        return syntaxCheckCheckBox.isSelected();
    }

    public boolean isAccepted() {
        return accepted;
    }

    // Setters (for loading saved settings)
    public void setSelectedTheme(String theme) {
        themeComboBox.setSelectedItem(theme);
    }

    public void setPoolSize(int size) {
        poolSizeSpinner.setValue(size);
    }

    public void setAutoSaveEnabled(boolean enabled) {
        autoSaveCheckBox.setSelected(enabled);
    }

    public void setSyntaxCheckingEnabled(boolean enabled) {
        syntaxCheckCheckBox.setSelected(enabled);
    }
}
```

#### Step 2: Show Settings Dialog

```java
public class Python3IDE_v2 extends JPanel {

    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog(SwingUtilities.getWindowAncestor(this));

        // Load current settings
        dialog.setSelectedTheme(themeManager.getCurrentTheme());
        dialog.setPoolSize(currentPoolSize);  // Get from connection manager
        dialog.setAutoSaveEnabled(autoSaveEnabled);
        dialog.setSyntaxCheckingEnabled(syntaxCheckingEnabled);

        // Show dialog
        dialog.setVisible(true);

        // Apply settings if accepted
        if (dialog.isAccepted()) {
            applySettings(dialog);
        }
    }

    private void applySettings(SettingsDialog dialog) {
        // Apply theme
        String theme = dialog.getSelectedTheme();
        applyTheme(themeManager.mapThemeNameToKey(theme));

        // Apply pool size
        int poolSize = dialog.getPoolSize();
        try {
            connectionManager.setPoolSize(poolSize);
            statusBar.setStatus("Pool size changed to " + poolSize,
                               ModernStatusBar.MessageType.SUCCESS);
        } catch (Exception e) {
            statusBar.setStatus("Failed to change pool size: " + e.getMessage(),
                               ModernStatusBar.MessageType.ERROR);
        }

        // Apply auto-save
        autoSaveEnabled = dialog.isAutoSaveEnabled();

        // Apply syntax checking
        syntaxCheckingEnabled = dialog.isSyntaxCheckingEnabled();
        if (syntaxCheckingEnabled) {
            enableSyntaxChecking();
        } else {
            disableSyntaxChecking();
        }

        // Save preferences
        savePreferences();
    }
}
```

---

## Best Practices

### 1. Separation of Concerns

**DO:**
- Managers handle business logic
- UI panels handle presentation
- Main class coordinates events

**DON'T:**
- Mix UI code in managers
- Put business logic in UI panels
- Create circular dependencies

---

### 2. Dependency Injection

**DO:**
```java
public class MyManager {
    private final Python3RestClient restClient;

    public MyManager(Python3RestClient restClient) {
        this.restClient = restClient;
    }
}
```

**DON'T:**
```java
public class MyManager {
    public MyManager() {
        this.restClient = new Python3RestClient();  // ❌ Hard to test!
    }
}
```

---

### 3. Threading

**DO:**
- Use `SwingWorker` for long-running operations
- Update UI on Event Dispatch Thread (EDT)
- Show progress indicators during background work

**DON'T:**
- Block EDT with long operations
- Update UI from background threads
- Forget to handle exceptions in `done()`

---

### 4. Error Handling

**DO:**
```java
try {
    Result result = manager.doSomething();
    handleSuccess(result);
} catch (IOException e) {
    LOGGER.error("Operation failed", e);
    statusBar.setStatus("Error: " + e.getMessage(), ModernStatusBar.MessageType.ERROR);
} catch (Exception e) {
    LOGGER.error("Unexpected error", e);
    statusBar.setStatus("Unexpected error occurred", ModernStatusBar.MessageType.ERROR);
}
```

**DON'T:**
```java
// Silent failures
try {
    manager.doSomething();
} catch (Exception e) {
    // ❌ No logging, no user feedback!
}
```

---

## Testing Extensions

### Unit Testing Managers

```java
@Test
public void testCodeFormatterManager() {
    // Arrange
    Python3RestClient mockClient = mock(Python3RestClient.class);
    FormattingResult mockResult = new FormattingResult(true, "formatted", null);
    when(mockClient.formatCode(anyString(), anyString())).thenReturn(mockResult);

    CodeFormatterManager manager = new CodeFormatterManager(mockClient);

    // Act
    String result = manager.formatCode("unformatted code");

    // Assert
    assertEquals("formatted", result);
    verify(mockClient).formatCode("unformatted code", "pep8");
}
```

### Integration Testing UI Panels

```java
@Test
public void testCodeOutlinePanel() {
    // Arrange
    CodeOutlinePanel panel = new CodeOutlinePanel();
    List<CodeOutlinePanel.CodeElement> elements = Arrays.asList(
        new CodeElement("MyClass", "class", 1, Collections.emptyList()),
        new CodeElement("my_function", "function", 5, Collections.emptyList())
    );

    // Act
    panel.updateOutline(elements);

    // Assert
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) panel.getTree().getModel().getRoot();
    assertEquals(2, root.getChildCount());
}
```

---

## Common Extension Scenarios

### Scenario 1: Adding Keyboard Shortcuts

```java
private void setupKeyboardShortcuts() {
    // Ctrl+S for save
    editorPanel.getCodeEditor().getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
        "save-script"
    );
    editorPanel.getCodeEditor().getActionMap().put("save-script",
        new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveScript();
            }
        }
    );

    // F5 for run
    editorPanel.getCodeEditor().getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
        "run-script"
    );
    editorPanel.getCodeEditor().getActionMap().put("run-script",
        new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeCode();
            }
        }
    );
}
```

---

### Scenario 2: Adding Context Menus

```java
private void setupContextMenus() {
    JPopupMenu editorContextMenu = new JPopupMenu();

    editorContextMenu.add(new JMenuItem(new AbstractAction("Cut") {
        @Override
        public void actionPerformed(ActionEvent e) {
            editorPanel.getCodeEditor().cut();
        }
    }));

    editorContextMenu.add(new JMenuItem(new AbstractAction("Copy") {
        @Override
        public void actionPerformed(ActionEvent e) {
            editorPanel.getCodeEditor().copy();
        }
    }));

    editorContextMenu.add(new JMenuItem(new AbstractAction("Paste") {
        @Override
        public void actionPerformed(ActionEvent e) {
            editorPanel.getCodeEditor().paste();
        }
    }));

    editorContextMenu.addSeparator();

    editorContextMenu.add(new JMenuItem(new AbstractAction("Format Code") {
        @Override
        public void actionPerformed(ActionEvent e) {
            formatCode();
        }
    }));

    editorPanel.getCodeEditor().setComponentPopupMenu(editorContextMenu);
}
```

---

### Scenario 3: Adding Preferences

```java
private void savePreferences() {
    Preferences prefs = Preferences.userNodeForPackage(Python3IDE_v2.class);

    prefs.put("theme", themeManager.getCurrentTheme());
    prefs.putInt("pool_size", currentPoolSize);
    prefs.putBoolean("auto_save", autoSaveEnabled);
    prefs.putBoolean("syntax_checking", syntaxCheckingEnabled);
}

private void loadPreferences() {
    Preferences prefs = Preferences.userNodeForPackage(Python3IDE_v2.class);

    String theme = prefs.get("theme", "dark");
    applyTheme(theme);

    int poolSize = prefs.getInt("pool_size", 3);
    // Set pool size...

    autoSaveEnabled = prefs.getBoolean("auto_save", true);
    syntaxCheckingEnabled = prefs.getBoolean("syntax_checking", true);
}
```

---

## Resources

- [V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md) - Architecture overview
- [V2_MIGRATION_GUIDE.md](V2_MIGRATION_GUIDE.md) - Migration from v1.9
- [Ignition SDK Javadocs](https://sdk-docs.inductiveautomation.com/)
- [Module GitHub Repository](https://github.com/nigelgwork/ignition-module-python3)

---

**Document Version:** 1.0
**Module Version:** 2.0.2
**Generated:** 2025-10-17 by Claude Code
