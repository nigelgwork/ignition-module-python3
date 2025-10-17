# Python3IDE Refactoring Plan

## Problem Statement

The file `Python3IDE_v1_9.java` has grown to **2,676 lines** and **~25,000 tokens**, making it:
- Difficult to read and maintain
- Slow to process in AI tools (exceeds 25,000 token limit)
- Violates Single Responsibility Principle
- Hard to test individual components
- Prone to merge conflicts in team environments

## Current Architecture Analysis

### Responsibilities in Python3IDE_v1_9.java (Current Monolith)

| Responsibility | Approx Lines | Description |
|---------------|--------------|-------------|
| **UI Component Creation** | ~500 | Creating toolbar, sidebar, editor panel, status bar |
| **Event Handling** | ~400 | Button clicks, tree selection, context menus, shortcuts |
| **Gateway Communication** | ~300 | REST API calls, connection management, execution |
| **Script Management** | ~800 | CRUD operations for scripts (save, load, delete, rename) |
| **Theme Management** | ~500 | Applying themes, updating UI colors, scrollbars |
| **Search/Replace** | ~150 | Find/replace dialog and handlers |
| **Drag-and-Drop** | ~300 | Tree DnD support, script/folder moving |
| **Utility Methods** | ~200 | Status updates, validation, formatting |
| **Fields/Constants** | ~100 | Instance variables, preferences |

**Total: ~3,150 lines across 8+ distinct responsibilities**

---

## Proposed Refactoring Strategy

### Phase 1: Extract Manager Classes (Non-UI Logic)
**Goal:** Move business logic out of the UI class into focused manager classes

#### 1.1 **ScriptManager.java** (~400 lines)
**Responsibility:** All script CRUD operations and metadata management

```java
package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Manages script operations (save, load, delete, rename, import, export).
 * Handles communication with Python3RestClient for script persistence.
 */
public class ScriptManager {
    private final Python3RestClient restClient;
    private String currentScriptName;
    private boolean isDirty;

    // Methods:
    // - saveScript(String name, String code, ScriptMetadata metadata)
    // - loadScript(String name) -> SavedScript
    // - deleteScript(String name)
    // - renameScript(String oldName, String newName)
    // - importScript(File file)
    // - exportScript(String name, File destination)
    // - listScripts() -> List<ScriptMetadata>
    // - getCurrentScriptName()
    // - isDirty()
    // - setDirty(boolean)
}
```

**Benefits:**
- Testable in isolation (unit tests)
- Reusable in other UI contexts
- ~400 lines removed from main class

---

#### 1.2 **ThemeManager.java** (~450 lines)
**Responsibility:** Theme application and UI component theming

```java
package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Manages IDE theme application and component styling.
 * Handles RSyntaxTextArea themes, Swing UIManager updates, and component traversal.
 */
public class ThemeManager {
    private String currentTheme;
    private final Preferences prefs;

    // Methods:
    // - applyTheme(String themeName, Component rootComponent, RSyntaxTextArea editor)
    // - applyDarkDialogTheme()
    // - applyLightDialogTheme()
    // - updateScrollPaneTheme(Component comp, boolean isDark)
    // - updateSplitPaneDividers(Component comp, boolean isDark)
    // - mapThemeNameToKey(String displayName)
    // - getCurrentTheme()
    // - saveThemePreference(String theme)
}
```

**Benefits:**
- Theme logic isolated and testable
- Easier to add new themes
- ~450 lines removed from main class

---

#### 1.3 **GatewayConnectionManager.java** (~350 lines)
**Responsibility:** Gateway connection, code execution, diagnostics

```java
package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Manages Gateway connection and Python code execution.
 * Handles REST client initialization, execution requests, and diagnostics.
 */
public class GatewayConnectionManager {
    private Python3RestClient restClient;
    private String gatewayUrl;
    private boolean isConnected;

    // Methods:
    // - connect(String url) -> boolean
    // - disconnect()
    // - executeCode(String code, String securityMode) -> ExecutionResult
    // - getPoolStats() -> PoolStats
    // - getPythonVersion() -> String
    // - setPoolSize(int size)
    // - isConnected()
    // - getRestClient()

    // Listeners for connection state changes
    private List<ConnectionListener> listeners;

    interface ConnectionListener {
        void onConnectionChanged(boolean connected);
    }
}
```

**Benefits:**
- Centralized connection logic
- Event-driven architecture
- ~350 lines removed from main class

---

### Phase 2: Extract UI Component Factories
**Goal:** Move UI component creation into focused factory classes

#### 2.1 **IDEToolbarFactory.java** (~200 lines)
**Responsibility:** Create and configure toolbar with all buttons

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

/**
 * Factory for creating the IDE toolbar with run, save, load, etc. buttons.
 */
public class IDEToolbarFactory {
    public static JPanel createToolbar(
            ActionListener runListener,
            ActionListener saveListener,
            ActionListener loadListener,
            ActionListener clearListener,
            ActionListener newScriptListener,
            ActionListener refreshListener
    ) {
        // Create toolbar with modern styling
        // Return configured panel
    }
}
```

---

#### 2.2 **ScriptTreePanel.java** (~400 lines)
**Responsibility:** Script tree UI, context menu, drag-and-drop

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

/**
 * Panel containing the script tree with folder navigation, context menus,
 * and drag-and-drop support.
 */
public class ScriptTreePanel extends JPanel {
    private final JTree scriptTree;
    private final ScriptManager scriptManager;

    // Methods:
    // - refreshTree()
    // - buildTree(List<ScriptMetadata> scripts)
    // - getSelectedScriptName()
    // - showContextMenu(MouseEvent e)
    // - setupDragAndDrop()

    // Nested classes:
    // - ScriptTreeTransferHandler
    // - ScriptTreeNode (move to separate file if needed)

    // Events:
    // - addTreeSelectionListener(Consumer<String> listener)
}
```

**Benefits:**
- Encapsulates tree logic
- Drag-and-drop contained in one place
- ~400 lines removed from main class

---

#### 2.3 **EditorPanel.java** (~300 lines)
**Responsibility:** Code editor with syntax highlighting and execution results

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

/**
 * Panel containing the code editor (RSyntaxTextArea) and execution results tabs.
 */
public class EditorPanel extends JPanel {
    private final RSyntaxTextArea codeEditor;
    private final JTextArea outputArea;
    private final JTextArea errorArea;
    private final JTabbedPane outputTabs;

    // Methods:
    // - getCode()
    // - setCode(String code)
    // - appendOutput(String text)
    // - appendError(String text)
    // - clearOutput()
    // - getCodeEditor() -> RSyntaxTextArea
    // - setupEditorFeatures() (autocomplete, syntax checking)
}
```

---

#### 2.4 **MetadataPanel.java** (~250 lines)
**Responsibility:** Script metadata editing (name, author, description)

```java
package com.inductiveautomation.ignition.examples.python3.designer.ui;

/**
 * Panel for editing script metadata (name, author, description, tags).
 */
public class MetadataPanel extends JPanel {
    private JTextField nameField;
    private JTextField authorField;
    private JTextArea descriptionArea;

    // Methods:
    // - getMetadata() -> ScriptMetadata
    // - setMetadata(ScriptMetadata metadata)
    // - clearMetadata()
    // - addMetadataChangeListener(Consumer<ScriptMetadata> listener)
}
```

---

### Phase 3: Extract Event Coordinators
**Goal:** Centralize event handling and coordination between components

#### 3.1 **IDEEventCoordinator.java** (~300 lines)
**Responsibility:** Wire up all events between managers and UI components

```java
package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Coordinates events between UI components, managers, and user actions.
 * Acts as the "Controller" in an MVC-like architecture.
 */
public class IDEEventCoordinator {
    private final ScriptManager scriptManager;
    private final GatewayConnectionManager connectionManager;
    private final ThemeManager themeManager;

    private final ScriptTreePanel treePanel;
    private final EditorPanel editorPanel;
    private final MetadataPanel metadataPanel;
    private final ModernStatusBar statusBar;

    // Event handlers:
    // - onRunCode()
    // - onSaveScript()
    // - onLoadScript(String name)
    // - onDeleteScript(String name)
    // - onTreeSelectionChanged(String scriptName)
    // - onThemeChanged(String theme)
    // - onConnectionRequested(String url)

    public void wireUpEvents() {
        // Connect all listeners
    }
}
```

**Benefits:**
- Clear event flow
- Easy to debug event chains
- ~300 lines removed from main class

---

### Phase 4: Refactored Main Class
**Goal:** Slim main class that assembles components

#### 4.1 **Python3IDE.java** (NEW, ~200-300 lines)
**Responsibility:** Assembly point - creates managers, factories, and coordinates

```java
package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Main IDE window for Python 3 script development.
 * Assembles all components and managers.
 *
 * v2.0.0: Refactored from Python3IDE_v1_9.java monolith
 */
public class Python3IDE extends JPanel {
    // Managers
    private final ScriptManager scriptManager;
    private final ThemeManager themeManager;
    private final GatewayConnectionManager connectionManager;

    // UI Components
    private final ScriptTreePanel treePanel;
    private final EditorPanel editorPanel;
    private final MetadataPanel metadataPanel;
    private final DiagnosticsPanel diagnosticsPanel;
    private final ModernStatusBar statusBar;

    // Event Coordinator
    private final IDEEventCoordinator eventCoordinator;

    public Python3IDE(DesignerContext context) {
        // 1. Initialize managers
        this.scriptManager = new ScriptManager();
        this.themeManager = new ThemeManager();
        this.connectionManager = new GatewayConnectionManager();

        // 2. Create UI panels
        this.treePanel = new ScriptTreePanel(scriptManager);
        this.editorPanel = new EditorPanel();
        this.metadataPanel = new MetadataPanel();
        this.diagnosticsPanel = new DiagnosticsPanel();
        this.statusBar = new ModernStatusBar();

        // 3. Create event coordinator
        this.eventCoordinator = new IDEEventCoordinator(
            scriptManager, connectionManager, themeManager,
            treePanel, editorPanel, metadataPanel, statusBar
        );

        // 4. Layout components
        layoutComponents();

        // 5. Wire up events
        eventCoordinator.wireUpEvents();

        // 6. Apply initial theme
        themeManager.applyTheme("dark", this, editorPanel.getCodeEditor());
    }

    private void layoutComponents() {
        // Simple layout code - all complexity moved to UI classes
    }
}
```

---

## Proposed File Structure

```
python3-integration/designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/

├── Python3IDE.java                          # Main assembly class (~250 lines)
│
├── managers/                                # Business logic managers
│   ├── ScriptManager.java                   # Script CRUD operations (~400 lines)
│   ├── ThemeManager.java                    # Theme application (~450 lines)
│   └── GatewayConnectionManager.java        # Gateway communication (~350 lines)
│
├── ui/                                      # UI component classes
│   ├── ScriptTreePanel.java                 # Script tree with DnD (~400 lines)
│   ├── EditorPanel.java                     # Code editor + results (~300 lines)
│   ├── MetadataPanel.java                   # Script metadata editing (~250 lines)
│   ├── DiagnosticsPanel.java                # Diagnostics display (~200 lines)
│   └── IDEToolbarFactory.java               # Toolbar creation (~200 lines)
│
├── events/                                  # Event coordination
│   └── IDEEventCoordinator.java             # Event wiring (~300 lines)
│
├── search/                                  # Search/replace functionality
│   └── SearchManager.java                   # Find/replace logic (~150 lines)
│
├── model/                                   # Data models
│   ├── ScriptMetadata.java                  # (already exists)
│   ├── SavedScript.java                     # (already exists)
│   └── ScriptTreeNode.java                  # Tree node model (~150 lines)
│
└── util/                                    # Shared utilities
    └── SwingUtils.java                      # Swing helper methods (~100 lines)

# Keep existing classes:
├── DesignerHook.java
├── Python3RestClient.java
├── ModernStatusBar.java
├── ModernTheme.java
└── ... (other existing classes)
```

---

## Token Reduction Analysis

### Before Refactoring
| File | Lines | Est. Tokens | Issue |
|------|-------|-------------|-------|
| Python3IDE_v1_9.java | 2,676 | ~25,000 | ❌ Exceeds 25K token limit |

### After Refactoring
| File | Lines | Est. Tokens | Status |
|------|-------|-------------|--------|
| Python3IDE.java | 250 | ~2,500 | ✅ Easy to read |
| ScriptManager.java | 400 | ~4,000 | ✅ Focused |
| ThemeManager.java | 450 | ~4,500 | ✅ Focused |
| GatewayConnectionManager.java | 350 | ~3,500 | ✅ Focused |
| ScriptTreePanel.java | 400 | ~4,000 | ✅ Focused |
| EditorPanel.java | 300 | ~3,000 | ✅ Focused |
| MetadataPanel.java | 250 | ~2,500 | ✅ Focused |
| IDEEventCoordinator.java | 300 | ~3,000 | ✅ Focused |
| Others (toolbar, search, utils) | 500 | ~5,000 | ✅ Small utilities |

**Total: ~3,200 lines split across 10-12 files (~2,500-4,500 tokens each)**

**Benefits:**
- ✅ All files under 5,000 tokens (well within limits)
- ✅ Can read entire files without truncation
- ✅ Faster AI processing (smaller context windows)
- ✅ Better code organization and maintainability

---

## Implementation Timeline

### Option 1: Big Bang Refactoring (3-4 hours)
- Extract all classes at once
- Risky: High chance of breaking changes
- Requires comprehensive testing after

### Option 2: Incremental Refactoring (Recommended, 6-8 hours spread over time)

#### Sprint 1: Extract Managers (2 hours)
- [ ] Create ScriptManager.java
- [ ] Create ThemeManager.java
- [ ] Create GatewayConnectionManager.java
- [ ] Update Python3IDE_v1_9.java to delegate to managers
- [ ] Test: All existing functionality works

#### Sprint 2: Extract UI Panels (2 hours)
- [ ] Create ScriptTreePanel.java
- [ ] Create EditorPanel.java
- [ ] Create MetadataPanel.java
- [ ] Update Python3IDE_v1_9.java to use new panels
- [ ] Test: UI layout and interactions work

#### Sprint 3: Create Event Coordinator (1 hour)
- [ ] Create IDEEventCoordinator.java
- [ ] Move all event wiring to coordinator
- [ ] Test: All events fire correctly

#### Sprint 4: Create New Main Class (1-2 hours)
- [ ] Create Python3IDE.java (v2.0.0)
- [ ] Update DesignerHook to use new Python3IDE
- [ ] Deprecate Python3IDE_v1_9.java (keep for reference)
- [ ] Test: Full IDE functionality
- [ ] Delete Python3IDE_v1_9.java

**Total: ~6-8 hours (can be spread over multiple sessions)**

---

## Testing Strategy

### Unit Tests (New)
```java
// Example: ScriptManagerTest.java
public class ScriptManagerTest {
    @Test
    public void testSaveAndLoadScript() {
        // Mock REST client
        // Test save/load roundtrip
    }

    @Test
    public void testDirtyStateTracking() {
        // Test dirty flag management
    }
}
```

### Integration Tests
- Test IDE assembly in Python3IDE constructor
- Test event flow through coordinator
- Test theme switching across all components

### Manual Testing Checklist
- [ ] Connect to Gateway
- [ ] Create new script
- [ ] Save script
- [ ] Load script
- [ ] Execute code
- [ ] Switch themes
- [ ] Drag-and-drop scripts
- [ ] Context menu operations
- [ ] Search/replace
- [ ] Keyboard shortcuts

---

## Migration Strategy

### Version Numbering
- **v1.17.x**: Current architecture (Python3IDE_v1_9.java monolith)
- **v2.0.0**: Refactored architecture (new class structure)

### Compatibility
- DesignerHook remains compatible
- REST API unchanged
- User preferences (themes, layout) preserved
- Script storage format unchanged

### Rollback Plan
- Keep Python3IDE_v1_9.java in codebase for 1-2 releases
- Can revert DesignerHook to use old class if issues arise
- Git tags for easy rollback

---

## Benefits Summary

### Developer Experience
- ✅ **Token Usage**: 25K → 2.5-4.5K per file (10x reduction)
- ✅ **Readability**: Clear class responsibilities
- ✅ **Maintainability**: Easy to find and modify code
- ✅ **Testability**: Can unit test managers in isolation
- ✅ **Collaboration**: Reduced merge conflicts

### Code Quality
- ✅ **Single Responsibility Principle**: Each class has one job
- ✅ **Separation of Concerns**: UI vs. business logic
- ✅ **Composition**: Managers and panels composed together
- ✅ **Event-Driven**: Clear event flow through coordinator
- ✅ **Reusability**: Managers can be used in other contexts

### AI Tool Performance
- ✅ **Faster processing**: Smaller context windows
- ✅ **Full file reads**: No truncation
- ✅ **Better understanding**: AI can see entire class
- ✅ **Precise edits**: Changes to specific classes only

---

## Implementation Status

**Date:** 2025-10-17

**Status:** Refactoring exploration completed, comprehensive plan created

### What Was Completed

1. **REFACTORING_PLAN.md Created** ✅
   - Comprehensive analysis of Python3IDE_v1_9.java (2,676 lines, ~25,000 tokens)
   - Detailed architecture proposal with 10-12 focused classes
   - Token reduction analysis showing 10x improvement
   - Implementation timeline and testing strategy

2. **Exploration and Planning** ✅
   - Identified 8+ distinct responsibilities in monolith
   - Designed manager layer (ScriptManager, ThemeManager, GatewayConnectionManager)
   - Designed UI panel layer (EditorPanel, ScriptTreePanel, MetadataPanel)
   - Designed event coordination layer

### Why Refactoring Was Paused

During implementation, we discovered:

1. **API Limitations**: Python3RestClient doesn't have all methods assumed in manager design (e.g., renameScript, createFolder)
2. **Model Mismatches**: ScriptTreeNode API differs from assumptions
3. **Time vs. Benefit**: Full refactoring requires 6-8 hours for proper implementation
4. **Current Solution Works**: Python3IDE_v1_9 is functional, though large

### Recommendation

**Option A (Recommended)**: Defer v2.0 refactoring until API enhancements are in place

1. Continue using Python3IDE_v1_9.java (fully functional)
2. Enhance Python3RestClient with missing methods first
3. Then proceed with incremental refactoring when time permits

**Option B**: Incremental refactoring with feature flags

1. Create managers/UI classes gradually
2. Use feature flags to test new architecture alongside v1.9
3. Migrate features one by one

**Option C**: Address immediate pain points only

1. Extract only ThemeManager (self-contained, ~450 lines)
2. Extract only GatewayConnectionManager (clear boundaries)
3. Keep UI code in main class for now

### Value Delivered

Even without full implementation, this work provides:

- **Clear Roadmap**: REFACTORING_PLAN.md serves as comprehensive guide
- **Architectural Vision**: Well-defined separation of concerns
- **Token Reduction Strategy**: 10x improvement path documented
- **Implementation Guide**: Detailed sprints, file structure, and timelines

### Next Steps (When Ready)

1. Review and approve Option A, B, or C above
2. If proceeding: Start with Sprint 1 (Managers)
3. Build incrementally with testing between sprints
4. Maintain v1.9 as baseline until v2.0 achieves feature parity

---

**This plan remains valuable as a reference for future architectural improvements.**
