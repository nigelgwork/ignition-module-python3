# Python 3 IDE - UX Improvements Plan
**Version:** 1.11.0+ Roadmap
**Status:** Planning Phase
**Last Updated:** 2025-10-16

## Overview

This document outlines UX improvements to make the Python 3 IDE feel modern and professional like VS Code, Cursor, or Warp. Improvements are organized by effort level to help prioritize implementation.

## 🎯 Design Goals

- **Modern & Clean**: Flat design, subtle shadows, rounded corners
- **Professional**: Looks like a premium IDE, not a legacy Java app
- **Responsive**: Smooth interactions, hover states, visual feedback
- **Accessible**: High contrast options, keyboard navigation, clear hierarchy
- **Consistent**: Unified color palette, spacing system, typography

---

## ⚡ Quick Wins (Low Effort, High Impact)

### 1. **Modern Color Palette**
**Effort**: 4-6 hours | **Impact**: Very High

Replace current colors with a modern, flat design system.

**VS Code-Inspired Dark Theme**:
```java
// Background colors
EDITOR_BG = #1E1E1E          // Main editor background
SIDEBAR_BG = #252526          // Sidebar/tree background
PANEL_BG = #1E1E1E            // Output/diagnostics panels
BORDER = #3E3E42              // Subtle borders
SELECTION_BG = #264F78        // Selected items

// Text colors
TEXT_PRIMARY = #D4D4D4        // Main text
TEXT_SECONDARY = #9D9D9D      // Secondary text
TEXT_ACCENT = #4EC9B0         // Highlighted text
TEXT_ERROR = #F48771          // Errors
TEXT_SUCCESS = #89D185        // Success messages

// UI colors
BUTTON_BG = #0E639C           // Primary buttons
BUTTON_HOVER = #1177BB        // Button hover state
ACCENT = #007ACC              // Links, active states
WARNING = #CCA700             // Warnings
```

**Implementation**:
- Create `ModernTheme.java` with color constants
- Update all components to use theme colors
- Support light/dark theme switching

**Before/After Preview**:
```
BEFORE: Gray borders, white backgrounds, blue buttons
AFTER: Subtle dark backgrounds, accent colors, flat design
```

---

### 2. **Rounded Corners & Modern Borders**
**Effort**: 3-4 hours | **Impact**: High

Replace sharp edges with subtle rounded corners (4-8px radius).

**Changes**:
- Panels: 6px rounded corners
- Buttons: 4px rounded corners
- Input fields: 4px rounded corners
- Tree items: 4px rounded corners on selection
- Replace `LineBorder` with custom rounded borders

**Code Example**:
```java
panel.setBorder(BorderFactory.createCompoundBorder(
    new RoundedBorder(6, new Color(60, 60, 66)),  // Rounded 6px
    new EmptyBorder(10, 10, 10, 10)                // Inner padding
));

// Custom RoundedBorder class
public class RoundedBorder extends AbstractBorder {
    private final int radius;
    private final Color color;

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
}
```

---

### 3. **Better Button Styling**
**Effort**: 4-6 hours | **Impact**: High

Modern buttons with hover effects, icons, and better visual hierarchy.

**Features**:
- Flat design with subtle hover states
- Icon + text buttons
- Primary/secondary button styles
- Disabled state styling
- Pressed animation (subtle)

**Code Example**:
```java
public class ModernButton extends JButton {
    private boolean hovered = false;

    public ModernButton(String text, Icon icon) {
        super(text, icon);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        if (hovered) {
            g2.setColor(BUTTON_HOVER);
        } else {
            g2.setColor(BUTTON_BG);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

        super.paintComponent(g);
    }
}
```

**Button Styles**:
- **Primary**: Blue background, white text (Run, Save)
- **Secondary**: Gray background, light text (Clear, Cancel)
- **Danger**: Red background, white text (Delete)
- **Ghost**: Transparent, border only (Cancel)

---

### 4. **Enhanced Status Bar**
**Effort**: 3-4 hours | **Impact**: Medium-High

Bottom status bar with contextual information like VS Code.

**Layout**:
```
┌─────────────────────────────────────────────────────────────────┐
│ ⚡ Python 3.11.5 | ●●○ Pool: 2/3 | Line 42, Col 18 | UTF-8 | Ln │
└─────────────────────────────────────────────────────────────────┘
```

**Features**:
- Current cursor position (line, column)
- File encoding
- Python version
- Pool status (with color indicator)
- Execution status (idle/running)
- Click sections to change settings

**Code Example**:
```java
public class StatusBar extends JPanel {
    private JLabel pythonVersion;
    private JLabel poolStatus;
    private JLabel cursorPosition;
    private JLabel encoding;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(new Color(0, 122, 204));  // VS Code blue
        setBorder(new EmptyBorder(2, 10, 2, 10));

        // Left side
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        pythonVersion = createStatusLabel("⚡ Python 3.11.5");
        poolStatus = createStatusLabel("●●○ Pool: 2/3");

        leftPanel.add(pythonVersion);
        leftPanel.add(poolStatus);

        // Right side
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        cursorPosition = createStatusLabel("Ln 42, Col 18");
        encoding = createStatusLabel("UTF-8");

        rightPanel.add(cursorPosition);
        rightPanel.add(encoding);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private JLabel createStatusLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return label;
    }
}
```

---

### 5. **Better Tree Styling**
**Effort**: 4-6 hours | **Impact**: Medium-High

Modern file tree like VS Code explorer.

**Features**:
- Subtle hover background on tree items
- Smooth expand/collapse animations
- Better folder/file icons (16x16 crisp icons)
- Indent guides (subtle vertical lines)
- Active file highlight
- Right-click context menu styling

**Code Example**:
```java
public class ModernTreeCellRenderer extends DefaultTreeCellRenderer {
    private boolean hovered = false;

    @Override
    public Component getTreeCellRendererComponent(...) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        // Modern styling
        setFont(new Font("SansSerif", Font.PLAIN, 13));
        setBorderSelectionColor(null);  // Remove border
        setBackgroundSelectionColor(new Color(37, 37, 38));  // Subtle selection
        setTextSelectionColor(new Color(212, 212, 212));

        // Hover effect
        if (hovered) {
            setBackgroundNonSelectionColor(new Color(42, 42, 43));
        }

        return this;
    }
}
```

---

### 6. **Icon Improvements**
**Effort**: 2-3 hours | **Impact**: Medium

Replace basic icons with modern, crisp vector-style icons.

**Icon Set** (use built-in Unicode symbols or custom SVG):
- ▶️ Run (green triangle)
- ⏹️ Stop (red square)
- 💾 Save (disk)
- 📁 Folder (yellow folder)
- 📄 Script (Python file)
- ⚙️ Settings (gear)
- 🔍 Search (magnifying glass)
- ↻ Refresh (circular arrow)

**Implementation**:
```java
public class ModernIcons {
    public static Icon createPlayIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

                // Green circle background
                g2.setColor(new Color(89, 209, 133));
                g2.fillOval(x, y, 16, 16);

                // White play triangle
                g2.setColor(Color.WHITE);
                int[] xPoints = {x + 6, x + 11, x + 6};
                int[] yPoints = {y + 4, y + 8, y + 12};
                g2.fillPolygon(xPoints, yPoints, 3);
            }

            @Override
            public int getIconWidth() { return 16; }

            @Override
            public int getIconHeight() { return 16; }
        };
    }
}
```

---

## 🚀 Medium Effort (Medium-High Impact)

### 7. **Command Palette (Ctrl+Shift+P)**
**Effort**: 8-12 hours | **Impact**: Very High

VS Code-style command palette for quick actions.

**Features**:
- Fuzzy search all commands
- Recent commands list
- Keyboard shortcuts shown
- Quick file switching
- Quick script access

**UI Design**:
```
┌─────────────────────────────────────────────┐
│ > run python script                         │
├─────────────────────────────────────────────┤
│ ▶ Run Python Script           Ctrl+Enter   │
│ 📄 New Python Script          Ctrl+N       │
│ 💾 Save Script                Ctrl+S       │
│ 🔍 Find in Scripts            Ctrl+F       │
│ ⚙️ Open Settings                           │
└─────────────────────────────────────────────┘
```

**Implementation**:
```java
public class CommandPalette extends JDialog {
    private JTextField searchField;
    private JList<Command> commandList;

    public CommandPalette(Frame owner) {
        super(owner, "Command Palette", true);
        setUndecorated(true);

        // Search field
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterCommands(searchField.getText());
            }
        });

        // Command list
        commandList = new JList<>(getAllCommands());
        commandList.setCellRenderer(new CommandCellRenderer());

        // Layout
        setLayout(new BorderLayout());
        add(searchField, BorderLayout.NORTH);
        add(new JScrollPane(commandList), BorderLayout.CENTER);

        // Size and position
        setSize(600, 400);
        setLocationRelativeTo(owner);
    }

    private void filterCommands(String query) {
        // Fuzzy search implementation
        List<Command> filtered = commands.stream()
            .filter(cmd -> fuzzyMatch(cmd.getName(), query))
            .sorted(byRelevance(query))
            .collect(Collectors.toList());

        commandList.setListData(filtered.toArray(new Command[0]));
    }
}
```

---

### 8. **Tabs for Multiple Scripts**
**Effort**: 10-14 hours | **Impact**: Very High

Open multiple scripts in tabs like VS Code.

**Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│ [ script1.py X ]  [ script2.py X ]  [ script3.py* X ]  [+] │
├─────────────────────────────────────────────────────────────┤
│ Code editor for active tab                                  │
└─────────────────────────────────────────────────────────────┘
```

**Features**:
- Multiple open files
- Close button on each tab
- Unsaved indicator (*)
- Drag to reorder tabs
- Middle-click to close
- Tab context menu

**Implementation**:
```java
public class ScriptTabPanel extends JPanel {
    private JTabbedPane tabbedPane;
    private Map<String, RSyntaxTextArea> openScripts;

    public ScriptTabPanel() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Custom tab renderer with close buttons
        tabbedPane.setUI(new ModernTabbedPaneUI());

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    public void openScript(String name, String code) {
        if (openScripts.containsKey(name)) {
            // Switch to existing tab
            int index = findTabIndex(name);
            tabbedPane.setSelectedIndex(index);
        } else {
            // Create new tab
            RSyntaxTextArea editor = createEditor(code);
            openScripts.put(name, editor);

            JPanel tabComponent = createTabComponent(name);
            tabbedPane.addTab(name, new RTextScrollPane(editor));
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabComponent);
        }
    }

    private JPanel createTabComponent(String name) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel(name);
        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.addActionListener(e -> closeTab(name));

        panel.add(label);
        panel.add(closeButton);

        return panel;
    }
}
```

---

### 9. **Minimap (Code Overview)**
**Effort**: 12-16 hours | **Impact**: Medium

Small code preview on right side like VS Code.

**Features**:
- Scaled-down view of entire file
- Current viewport highlight
- Click to jump to section
- Syntax highlighting in minimap

**Layout**:
```
┌────────────────────────────┬──┐
│ Code Editor                │▓▓│
│                            │▓▓│ <- Minimap
│                            │  │
│                            │░░│ <- Current view
│                            │  │
└────────────────────────────┴──┘
```

---

### 10. **Split View**
**Effort**: 8-12 hours | **Impact**: High

Split editor horizontally or vertically.

**Features**:
- Split editor into 2+ panes
- Independent scrolling
- Same file or different files
- Drag to resize splits

**Layout**:
```
┌───────────────┬───────────────┐
│ script1.py    │ script2.py    │
│               │               │
│               │               │
└───────────────┴───────────────┘
```

---

### 11. **Breadcrumb Navigation**
**Effort**: 6-8 hours | **Impact**: Medium

Show current folder path like VS Code.

**Layout**:
```
┌─────────────────────────────────────────────┐
│ Scripts > Data Processing > Import Data     │
├─────────────────────────────────────────────┤
│ Code editor...                              │
└─────────────────────────────────────────────┘
```

**Features**:
- Click segments to navigate
- Dropdown to see siblings
- Current file highlight

---

### 12. **Inline Execution Results** (Jupyter-style)
**Effort**: 10-14 hours | **Impact**: Very High

Show execution results inline next to code, like Jupyter notebooks.

**Layout**:
```
1  import pandas as pd
2  df = pd.DataFrame({'x': [1,2,3]})
3  df.describe()
   ┌──────────────────────────┐
   │ Output:                  │
   │       x                  │
   │ count 3.0                │
   │ mean  2.0                │
   │ ...                      │
   └──────────────────────────┘ <- Inline result
4
```

**Features**:
- Results appear inline
- Collapsible output
- Rich display (tables, images, plots)
- Clear button per cell

---

### 13. **Search & Replace in Files**
**Effort**: 8-10 hours | **Impact**: Medium-High

Search across all scripts.

**Features**:
- Regex support
- Case sensitive toggle
- Replace in multiple files
- Search history

**UI**:
```
┌─────────────────────────────────────────────┐
│ Find: [import pandas            ]  [×]      │
│      ☑ Match case  ☑ Regex  ☑ Whole word  │
├─────────────────────────────────────────────┤
│ 📄 script1.py (2 matches)                  │
│    Line 1: import pandas as pd             │
│    Line 5: # pandas dataframe              │
│ 📄 script2.py (1 match)                    │
│    Line 10: import pandas                  │
└─────────────────────────────────────────────┘
```

---

## 🎨 High Effort (Polish & Advanced)

### 14. **Smooth Animations**
**Effort**: 12-16 hours | **Impact**: Medium

Subtle animations for UI transitions.

**Features**:
- Fade in/out for dialogs
- Smooth panel resize
- Hover transitions
- Loading spinners
- Progress animations

**Example**:
```java
public class FadeTransition {
    public static void fadeIn(JComponent component, int duration) {
        Timer timer = new Timer(10, null);
        final float[] alpha = {0.0f};

        timer.addActionListener(e -> {
            alpha[0] += 0.05f;
            if (alpha[0] >= 1.0f) {
                alpha[0] = 1.0f;
                timer.stop();
            }
            component.setOpaque(alpha[0] == 1.0f);
            component.repaint();
        });

        timer.start();
    }
}
```

---

### 15. **Custom Font Rendering**
**Effort**: 10-14 hours | **Impact**: Medium

Better typography like modern IDEs.

**Features**:
- **Ligatures** for code (→, ≤, ≥, !=)
- Subpixel antialiasing
- Custom line height
- Letter spacing
- Better CJK font support

**Recommended Fonts**:
- JetBrains Mono (with ligatures)
- Fira Code
- Cascadia Code
- Source Code Pro

---

### 16. **Integrated Terminal**
**Effort**: 16-24 hours | **Impact**: High

Built-in terminal panel like VS Code.

**Features**:
- Run shell commands
- Python REPL
- Install packages (pip)
- Multiple terminal tabs

**Layout**:
```
┌─────────────────────────────────────────────┐
│ Code Editor                                 │
├─────────────────────────────────────────────┤
│ ▼ TERMINAL                            [×]   │
│ $ python --version                          │
│ Python 3.11.5                               │
│ $ _                                         │
└─────────────────────────────────────────────┘
```

---

### 17. **Advanced Theming System**
**Effort**: 14-20 hours | **Impact**: Medium-High

Complete theming like VS Code.

**Features**:
- 10+ built-in themes
- Custom theme editor
- Import VS Code themes (JSON)
- Per-element color customization
- Theme preview

**Popular Themes to Include**:
- Dark+ (VS Code default)
- Dracula
- One Dark Pro
- Solarized Dark/Light
- GitHub Dark/Light
- Monokai Pro

---

### 18. **Workspace Settings**
**Effort**: 10-14 hours | **Impact**: Medium

VS Code-style settings panel.

**Features**:
- User settings (global)
- Workspace settings (per project)
- Search settings
- JSON editor for advanced
- Settings sync

**UI**:
```
┌─────────────────────────────────────────────┐
│ [User] [Workspace]           [Search...]    │
├─────────────────────────────────────────────┤
│ Editor                                      │
│   Font Size: [12]                           │
│   Theme: [Dark+] ▼                          │
│   Line Numbers: ☑                           │
│                                             │
│ Python                                      │
│   Auto-format: ☑                            │
│   Max Line Length: [88]                     │
└─────────────────────────────────────────────┘
```

---

### 19. **Git Integration UI**
**Effort**: 20-30 hours | **Impact**: High (for teams)

VS Code-style Git integration.

**Features**:
- View changed files
- Stage/unstage changes
- Commit with message
- Push/pull
- Branch switching
- Diff viewer
- Merge conflict resolution

**UI**:
```
┌─────────────────────────────────────────────┐
│ 📂 SOURCE CONTROL              [Commit ✓]   │
├─────────────────────────────────────────────┤
│ Message: [Add new script...]                │
├─────────────────────────────────────────────┤
│ Changes (2)                                 │
│  M script1.py                               │
│  + script2.py                               │
└─────────────────────────────────────────────┘
```

---

### 20. **Keyboard Shortcuts Overlay**
**Effort**: 6-8 hours | **Impact**: Medium

Show keyboard shortcuts on demand.

**Features**:
- Chord keys (like Ctrl+K Ctrl+S in VS Code)
- Customizable shortcuts
- Conflict detection
- Cheat sheet overlay

**UI** (Press Ctrl+K Ctrl+S):
```
┌─────────────────────────────────────────────┐
│ KEYBOARD SHORTCUTS                    [×]   │
├─────────────────────────────────────────────┤
│ Run Script         Ctrl+Enter              │
│ Save               Ctrl+S                   │
│ Command Palette    Ctrl+Shift+P            │
│ Find               Ctrl+F                   │
│ New Script         Ctrl+N                   │
│ ...                                         │
└─────────────────────────────────────────────┘
```

---

## 📊 Comparison: Current vs. Improved

| Feature | Current (v1.10.1) | After Quick Wins | After All UX Improvements |
|---------|-------------------|------------------|---------------------------|
| **Color Scheme** | Basic grays | Modern dark/light | Full theme system |
| **Borders** | Sharp edges | Rounded corners | Animated, context-aware |
| **Buttons** | Default Swing | Hover effects, icons | Full interaction states |
| **Tree** | Basic | Modern styling | Indent guides, animations |
| **Status Bar** | Simple label | Info-rich | Clickable, contextual |
| **Icons** | Basic | Modern vectors | Animated, contextual |
| **Navigation** | Tree only | Breadcrumbs | Command palette, tabs |
| **Editing** | Single file | Tabs | Split view, minimap |
| **Results** | Output panel | Inline hints | Jupyter-style cells |
| **Customization** | Themes | Settings panel | Full workspace config |

---

## 🎯 Recommended Implementation Order

### Phase A: Visual Polish (v1.11.0)
**Total Effort**: ~20-30 hours
1. Modern Color Palette (6h)
2. Rounded Corners (4h)
3. Better Button Styling (6h)
4. Enhanced Status Bar (4h)
5. Better Tree Styling (6h)
6. Icon Improvements (3h)

**Result**: IDE looks modern and professional

### Phase B: Navigation & Productivity (v1.12.0)
**Total Effort**: ~28-40 hours
1. Command Palette (12h)
2. Tabs for Multiple Scripts (14h)
3. Breadcrumb Navigation (8h)
4. Search & Replace in Files (10h)

**Result**: IDE feels efficient and powerful

### Phase C: Advanced Features (v1.13.0+)
**Total Effort**: ~50-80 hours
1. Split View (12h)
2. Minimap (16h)
3. Inline Execution Results (14h)
4. Integrated Terminal (24h)
5. Advanced Theming System (20h)

**Result**: IDE competes with professional tools

---

## 🚀 Quick Start: Minimum Viable Polish (MVP)

If you want the **biggest visual impact** with **minimal effort**, implement these 3 items first:

1. **Modern Color Palette** (6h) - Makes everything look better instantly
2. **Rounded Corners** (4h) - Feels modern immediately
3. **Better Button Styling** (6h) - Most interacted-with elements

**Total: ~16 hours** for a dramatically improved look.

---

## 💡 Implementation Tips

### 1. Create a Theme System Early
```java
public class ModernTheme {
    // Colors
    public static final Color BG_EDITOR = new Color(30, 30, 30);
    public static final Color BG_SIDEBAR = new Color(37, 37, 38);
    public static final Color ACCENT = new Color(0, 122, 204);

    // Spacing
    public static final int PADDING_SMALL = 4;
    public static final int PADDING_MEDIUM = 8;
    public static final int PADDING_LARGE = 16;

    // Border radius
    public static final int RADIUS_SMALL = 4;
    public static final int RADIUS_MEDIUM = 6;

    // Fonts
    public static final Font FONT_UI = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_CODE = new Font("Monospaced", Font.PLAIN, 13);
}
```

### 2. Use Graphics2D for Custom Painting
```java
@Override
protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    // ... custom painting
}
```

### 3. Consistent Spacing System
Use multiples of 4: 4px, 8px, 12px, 16px, 24px

### 4. Test on Different DPI Screens
High-DPI displays (Retina, 4K) need special handling

---

## 📚 Reference: Modern IDE Design Patterns

### VS Code Design Language
- Flat colors, no gradients
- Subtle borders (#3E3E42)
- 4px rounded corners
- Monospace fonts with ligatures
- Hover states on everything
- Context-aware colors

### Cursor Design Language
- Similar to VS Code but with:
- More rounded corners (6-8px)
- Gradient accents
- Floating panels
- AI chat integration

### Warp Design Language
- Terminal-focused
- Heavy use of blur effects
- Command blocks with backgrounds
- Inline command suggestions
- Modern color palette (purple accents)

---

## ✅ Success Metrics

After implementing UX improvements, the IDE should feel:
- **Fast**: Responsive interactions (<100ms)
- **Modern**: Looks like a 2024 app, not 2004
- **Professional**: Developers trust it for serious work
- **Discoverable**: Features are easy to find
- **Customizable**: Users can make it their own

---

**Ready to make the IDE beautiful!** Start with Phase A for maximum visual impact.
