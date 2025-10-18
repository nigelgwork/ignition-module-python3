# Ignition Designer UX & Theme Guide

A comprehensive reference for implementing modern, theme-aware UX in Ignition Designer modules.

**Based on:** Lessons learned from extensive UX/theme development in Ignition 8.3 SDK
**Last Updated:** 2025-10-18
**Applies to:** Ignition Designer-scoped modules using Java Swing

---

## Table of Contents

1. [Overview](#overview)
2. [Core Challenges](#core-challenges)
3. [Architecture Patterns](#architecture-patterns)
4. [Theme Management](#theme-management)
5. [Component-Specific Solutions](#component-specific-solutions)
6. [Custom Components](#custom-components)
7. [Best Practices](#best-practices)
8. [Code Examples](#code-examples)
9. [Common Patterns](#common-patterns)
10. [Troubleshooting](#troubleshooting)

---

## Overview

### The Challenge

Ignition Designer runs inside a managed Swing environment where:
- UIManager settings are often overridden by Ignition's own theme system
- Standard theming approaches don't always work as expected
- Components need manual styling even when UIManager properties are set
- Dark theme support requires comprehensive, multi-layered theming

### Key Learnings

1. **UIManager alone is insufficient** - You must theme components at multiple levels
2. **Timing matters** - Apply themes AFTER component creation, not before
3. **Recursive updates required** - Component trees need explicit traversal and styling
4. **Custom components needed** - Some Swing components resist theming and need custom UI delegates
5. **Test exhaustively** - Theme issues appear in unexpected places (dialogs, popups, dividers)

---

## Core Challenges

### 1. UIManager Override Problem

**Issue:** Ignition Designer overrides UIManager properties, making standard Swing theming unreliable.

**Symptoms:**
- Setting `UIManager.put("Panel.background", darkColor)` doesn't affect all panels
- Dialog backgrounds remain light even after setting dark theme properties
- Inconsistent theming across different component types

**Root Cause:** Ignition's designer framework applies its own Look and Feel that takes precedence.

### 2. Component-Specific Resistance

**Issue:** Certain components actively resist theming attempts.

**Problem Components:**
- `JSplitPane` dividers (often stay light gray regardless of settings)
- `JScrollPane` viewports (show light backgrounds even when content is dark)
- `JOptionPane` dialogs (completely ignore UIManager in Ignition context)
- Context menus (`JPopupMenu`) (require explicit styling after creation)

### 3. Timing Issues

**Issue:** When you apply themes matters as much as how you apply them.

**Critical Discovery:**
- Theming popup menus BEFORE adding menu items → doesn't work
- Theming popup menus AFTER adding menu items → works correctly
- Must call `SwingUtilities.updateComponentTreeUI()` after theme changes
- Need to recursively update existing component trees

### 4. State Management

**Issue:** Theme state must persist across sessions and propagate to all components.

**Requirements:**
- Save user's theme preference
- Apply theme on startup
- Update all existing components when theme changes
- Ensure new dialogs/popups use current theme

---

## Architecture Patterns

### Pattern 1: Theme Manager (Recommended)

Extract all theme logic into a dedicated manager class.

**Benefits:**
- Single source of truth for theme state
- Centralized theme application logic
- Easy to test and maintain
- Reusable across projects

**Structure:**

```
project/
├── managers/
│   └── ThemeManager.java          # Theme orchestration
├── ModernTheme.java                # Color constants & utilities
└── ui/
    ├── DarkDialog.java             # Custom themed dialogs
    └── ThemedSplitPaneUI.java      # Custom UI delegates
```

### Pattern 2: Theme Constants Class

Define all colors, fonts, and spacing in a central constants class.

**Benefits:**
- Consistent color palette
- Easy to adjust theme
- Type-safe color references
- Utility methods for color manipulation

### Pattern 3: Custom Dialog System

Bypass `JOptionPane` entirely with custom dialog implementations.

**Why:**
- `JOptionPane` ignores UIManager in Ignition Designer
- Custom dialogs give you full control
- Can ensure consistent theming
- More flexibility in layout and interaction

### Pattern 4: Recursive Component Styling

Traverse component trees to apply themes comprehensively.

**Why:**
- UIManager doesn't affect existing components
- Nested components may be missed
- Container backgrounds need explicit updates
- Ensures complete theme coverage

---

## Theme Management

### ThemeManager Pattern

**Core Responsibilities:**

1. **State Management**
   - Current theme (dark/light)
   - Theme persistence (Preferences API)
   - Theme switching logic

2. **Application**
   - UIManager configuration
   - Component-specific styling
   - Recursive tree updates
   - Special component handling

3. **Integration**
   - Editor components (RSyntaxTextArea, etc.)
   - Output/error areas
   - Trees and lists
   - Panels and containers

**Key Methods:**

```java
public class ThemeManager {
    // Apply theme to all components
    public void applyTheme(String themeName, Component rootComponent, ...);

    // Dark theme UIManager setup
    private void applyDarkDialogTheme();

    // Light theme UIManager setup
    private void applyLightDialogTheme();

    // Recursive component updates
    private void updateComponent(Component comp, Color background);
    private void updateScrollPaneTheme(Component comp, boolean isDark);
    private void updateSplitPaneDividers(Component comp, boolean isDark);

    // Persistence
    public String getSavedThemePreference();
    public String getCurrentTheme();
}
```

### ModernTheme Constants

**Essential Color Categories:**

```java
public class ModernTheme {
    // Background layers (dark to light)
    public static final Color BACKGROUND_DARKER = new Color(23, 23, 23);
    public static final Color BACKGROUND_DARK = new Color(30, 30, 30);
    public static final Color BACKGROUND_LIGHT = new Color(37, 37, 38);

    // Foreground colors
    public static final Color FOREGROUND_PRIMARY = new Color(204, 204, 204);
    public static final Color FOREGROUND_SECONDARY = new Color(150, 150, 150);

    // Accent colors
    public static final Color ACCENT_PRIMARY = new Color(14, 99, 156);
    public static final Color ACCENT_HOVER = new Color(28, 113, 166);

    // Semantic colors
    public static final Color SUCCESS = new Color(76, 175, 80);
    public static final Color ERROR = new Color(244, 67, 54);
    public static final Color WARNING = new Color(255, 167, 38);

    // UI elements
    public static final Color BORDER_DEFAULT = new Color(64, 64, 64);
    public static final Color BUTTON_BACKGROUND = new Color(51, 51, 51);
    public static final Color BUTTON_HOVER = new Color(64, 64, 64);

    // Fonts
    public static final Font FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);
}
```

### UIManager Configuration (Dark Theme)

**Comprehensive Property List:**

```java
private void applyDarkDialogTheme() {
    // Dialog components
    UIManager.put("OptionPane.background", PANEL_BACKGROUND);
    UIManager.put("OptionPane.messageForeground", FOREGROUND_PRIMARY);

    // Panels and containers
    UIManager.put("Panel.background", PANEL_BACKGROUND);
    UIManager.put("control", PANEL_BACKGROUND);

    // Text fields - CRITICAL: Include all properties
    UIManager.put("TextField.background", BACKGROUND_DARKER);
    UIManager.put("TextField.foreground", FOREGROUND_PRIMARY);
    UIManager.put("TextField.caretForeground", FOREGROUND_PRIMARY);
    UIManager.put("TextField.selectionBackground", ACCENT_PRIMARY);
    UIManager.put("TextField.selectionForeground", Color.WHITE);
    UIManager.put("TextField.border", BorderFactory.createLineBorder(BORDER_DEFAULT));

    // Text areas - Same comprehensive approach
    UIManager.put("TextArea.background", BACKGROUND_DARKER);
    UIManager.put("TextArea.foreground", FOREGROUND_PRIMARY);
    UIManager.put("TextArea.caretForeground", FOREGROUND_PRIMARY);

    // Buttons
    UIManager.put("Button.background", BUTTON_BACKGROUND);
    UIManager.put("Button.foreground", FOREGROUND_PRIMARY);
    UIManager.put("Button.select", BUTTON_HOVER);

    // Trees
    UIManager.put("Tree.background", TREE_BACKGROUND);
    UIManager.put("Tree.foreground", FOREGROUND_PRIMARY);
    UIManager.put("Tree.selectionBackground", ACCENT_PRIMARY);
    UIManager.put("Tree.selectionForeground", Color.WHITE);

    // ScrollPane - CRITICAL FIX for light backgrounds
    UIManager.put("ScrollPane.background", BACKGROUND_DARK);
    UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER_DEFAULT));

    // ScrollBar
    UIManager.put("ScrollBar.background", BACKGROUND_DARKER);
    UIManager.put("ScrollBar.thumb", FOREGROUND_SECONDARY);
    UIManager.put("ScrollBar.track", BACKGROUND_DARKER);

    // SplitPane - CRITICAL FIX for light dividers
    UIManager.put("SplitPane.background", BACKGROUND_DARK);
    UIManager.put("SplitPane.dividerSize", 5);
    UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
    UIManager.put("SplitPane.dividerFocusColor", BACKGROUND_DARKER);

    // Viewport (scroll pane content area)
    UIManager.put("Viewport.background", BACKGROUND_DARK);

    // Menus and popups
    UIManager.put("PopupMenu.background", PANEL_BACKGROUND);
    UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER_DEFAULT));
    UIManager.put("MenuItem.background", PANEL_BACKGROUND);
    UIManager.put("MenuItem.foreground", FOREGROUND_PRIMARY);
    UIManager.put("MenuItem.selectionBackground", ACCENT_PRIMARY);
}
```

### Recursive Component Updates

**Critical Pattern:**

```java
// After setting UIManager properties, MUST update existing components
SwingUtilities.updateComponentTreeUI(rootComponent);

// Then apply component-specific fixes
updateScrollPaneTheme(rootComponent, isDarkTheme);
updateSplitPaneDividers(rootComponent, isDarkTheme);

// Recursive scroll pane updater
private void updateScrollPaneTheme(Component comp, boolean isDarkTheme) {
    if (comp instanceof JScrollPane) {
        JScrollPane scrollPane = (JScrollPane) comp;
        Color bg = isDarkTheme ? BACKGROUND_DARK : Color.WHITE;
        scrollPane.getViewport().setBackground(bg);
        scrollPane.setBackground(bg);
    }
    if (comp instanceof Container) {
        for (Component child : ((Container) comp).getComponents()) {
            updateScrollPaneTheme(child, isDarkTheme);
        }
    }
}

// Recursive split pane divider updater
private void updateSplitPaneDividers(Component comp, boolean isDarkTheme) {
    if (comp instanceof JSplitPane) {
        JSplitPane splitPane = (JSplitPane) comp;
        if (splitPane.getUI() instanceof BasicSplitPaneUI) {
            Color dividerColor = isDarkTheme
                ? BACKGROUND_DARKER
                : new Color(200, 200, 200);
            ((BasicSplitPaneUI) splitPane.getUI())
                .getDivider()
                .setBackground(dividerColor);
        }
    }
    if (comp instanceof Container) {
        for (Component child : ((Container) comp).getComponents()) {
            updateSplitPaneDividers(child, isDarkTheme);
        }
    }
}
```

---

## Component-Specific Solutions

### JScrollPane Viewports

**Problem:** Viewport backgrounds show light gray even when content is themed dark.

**Solution:** Explicitly set viewport background after creating scroll pane.

```java
JScrollPane scrollPane = new JScrollPane(textArea);

// Not enough - this doesn't affect the viewport
scrollPane.setBackground(darkColor);

// Required - set viewport explicitly
scrollPane.getViewport().setBackground(darkColor);
```

**Why:** The viewport is a separate component inside the scroll pane that has its own background.

### JSplitPane Dividers

**Problem:** Dividers remain light gray despite UIManager settings.

**Solution 1 (Simple):** Direct divider background update via UI delegate.

```java
JSplitPane splitPane = new JSplitPane();
if (splitPane.getUI() instanceof BasicSplitPaneUI) {
    ((BasicSplitPaneUI) splitPane.getUI())
        .getDivider()
        .setBackground(BACKGROUND_DARKER);
}
```

**Solution 2 (Robust):** Custom SplitPaneUI with themed divider.

```java
public class ThemedSplitPaneUI extends BasicSplitPaneUI {
    private final Color dividerColor;

    public ThemedSplitPaneUI(Color dividerColor) {
        this.dividerColor = dividerColor;
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this) {
            @Override
            public void paint(Graphics g) {
                g.setColor(dividerColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            @Override
            public void setBackground(Color bg) {
                // Prevent external color changes
                super.setBackground(dividerColor);
            }
        };
    }
}

// Usage
splitPane.setUI(new ThemedSplitPaneUI(BACKGROUND_DARKER));
```

### Context Menus (JPopupMenu)

**Problem:** Context menus ignore theme and show light backgrounds with invisible text.

**Critical Discovery:** Must style menu AFTER adding menu items, not before.

```java
JPopupMenu menu = new JPopupMenu();

// Add all menu items first
JMenuItem item1 = new JMenuItem("Action 1");
menu.add(item1);
JMenuItem item2 = new JMenuItem("Action 2");
menu.add(item2);

// Theme AFTER items are added - critical timing!
stylePopupMenu(menu);

// Show menu
menu.show(component, x, y);

// Styling method
private void stylePopupMenu(JPopupMenu menu) {
    boolean isDark = currentTheme.equals("dark");

    if (isDark) {
        menu.setBackground(BACKGROUND_DARK);
        menu.setForeground(FOREGROUND_PRIMARY);
        menu.setBorder(BorderFactory.createLineBorder(BORDER_DEFAULT, 1));

        // Style each menu item
        for (Component comp : menu.getComponents()) {
            if (comp instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) comp;
                item.setBackground(BACKGROUND_DARK);
                item.setForeground(FOREGROUND_PRIMARY);

                // Hover color
                item.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        item.setBackground(ACCENT_PRIMARY);
                    }
                    public void mouseExited(MouseEvent e) {
                        item.setBackground(BACKGROUND_DARK);
                    }
                });
            }
        }
    }
}
```

### JTree Selection Colors

**Problem:** Tree selection backgrounds don't respect theme.

**Solution:** Set both UIManager and component-specific properties.

```java
// UIManager (for future trees)
UIManager.put("Tree.selectionBackground", ACCENT_PRIMARY);
UIManager.put("Tree.selectionForeground", Color.WHITE);

// Existing tree instance
tree.setBackground(TREE_BACKGROUND);
tree.setForeground(FOREGROUND_PRIMARY);

// Custom renderer for full control
tree.setCellRenderer(new DefaultTreeCellRenderer() {
    @Override
    public Component getTreeCellRendererComponent(
            JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(
            tree, value, selected, expanded, leaf, row, hasFocus);

        if (selected) {
            setBackground(ACCENT_PRIMARY);
            setForeground(Color.WHITE);
        } else {
            setBackground(TREE_BACKGROUND);
            setForeground(FOREGROUND_PRIMARY);
        }

        return this;
    }
});
```

### Text Components (Caret Color)

**Problem:** Light caret invisible on light backgrounds in dark theme.

**Solution:** Always set caret color to match foreground.

```java
JTextArea textArea = new JTextArea();
textArea.setBackground(BACKGROUND_DARKER);
textArea.setForeground(FOREGROUND_PRIMARY);

// CRITICAL - set caret color
textArea.setCaretColor(FOREGROUND_PRIMARY);

// Same for selection colors
textArea.setSelectionColor(ACCENT_PRIMARY);
textArea.setSelectedTextColor(Color.WHITE);
```

---

## Custom Components

### DarkDialog - Theme-Aware Dialog System

**Why Needed:** `JOptionPane` completely ignores UIManager in Ignition Designer.

**Implementation Pattern:**

```java
public class DarkDialog {
    private static boolean useDarkTheme = true;

    // Theme toggle
    public static void setDarkTheme(boolean darkTheme) {
        useDarkTheme = darkTheme;
    }

    // Message dialog
    public static void showMessage(Component parent, String message, String title) {
        JDialog dialog = createBaseDialog(parent, title);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel messageLabel = new JLabel(
            "<html><body style='width: 300px'>" + message + "</body></html>");
        messageLabel.setForeground(getForeground());
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        JButton okButton = createThemedButton("OK");
        okButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(getBackground());
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // Confirmation dialog
    public static boolean showConfirm(Component parent, String message, String title) {
        JDialog dialog = createBaseDialog(parent, title);
        final boolean[] result = {false};

        // ... similar pattern with Yes/No buttons
        // Set result[0] = true on Yes click

        return result[0];
    }

    // Input dialog
    public static String showInput(Component parent, String message,
                                   String title, String initialValue) {
        JDialog dialog = createBaseDialog(parent, title);
        final String[] result = {null};

        JTextField inputField = createThemedTextField(initialValue, 30);

        // ... build dialog with OK/Cancel
        // Set result[0] = inputField.getText() on OK

        return result[0];
    }

    // Helper methods
    private static JDialog createBaseDialog(Component parent, String title) {
        Window owner = parent instanceof Window
            ? (Window) parent
            : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(getBackground());
        return dialog;
    }

    private static JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(getButtonBg());
        button.setForeground(getForeground());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(5, 15, 5, 15)
        ));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(getButtonHoverBg());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(getButtonBg());
            }
        });

        return button;
    }

    // Theme-aware color getters
    private static Color getBackground() {
        return useDarkTheme ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }
    // ... similar for other colors
}
```

**Usage:**

```java
// Instead of JOptionPane
DarkDialog.showMessage(this, "Operation completed!", "Success");

// Confirmation
if (DarkDialog.showConfirm(this, "Delete this item?", "Confirm")) {
    // User clicked Yes
}

// Input
String name = DarkDialog.showInput(this, "Enter name:", "Input", "");
```

### ThemedSplitPaneUI

**Full Implementation:**

```java
public class ThemedSplitPaneUI extends BasicSplitPaneUI {
    private final Color dividerColor;

    public ThemedSplitPaneUI(Color dividerColor) {
        this.dividerColor = dividerColor;
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this) {
            @Override
            public void paint(Graphics g) {
                // Paint entire divider with theme color
                g.setColor(dividerColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            @Override
            public void setBackground(Color bg) {
                // Override to prevent external color changes
                super.setBackground(dividerColor);
            }
        };
    }
}
```

**Usage:**

```java
JSplitPane splitPane = new JSplitPane(
    JSplitPane.HORIZONTAL_SPLIT,
    leftPanel,
    rightPanel
);

// Apply custom UI for proper divider theming
boolean isDark = currentTheme.equals("dark");
Color dividerColor = isDark ? BACKGROUND_DARKER : new Color(200, 200, 200);
splitPane.setUI(new ThemedSplitPaneUI(dividerColor));
```

---

## Best Practices

### 1. Multi-Layer Theming

**Always theme at ALL levels:**

```java
// Layer 1: UIManager (affects future components)
UIManager.put("Panel.background", BACKGROUND_DARK);

// Layer 2: Root component
rootPanel.setBackground(BACKGROUND_DARK);

// Layer 3: Recursive tree update
updateComponentTree(rootPanel, BACKGROUND_DARK);

// Layer 4: Component-specific fixes
updateScrollPanes(rootPanel);
updateSplitPanes(rootPanel);
```

### 2. Theme Application Timing

**Correct order:**

```java
// 1. Create components
JPanel panel = new JPanel();
JScrollPane scrollPane = new JScrollPane(textArea);

// 2. Add components to hierarchy
panel.add(scrollPane);

// 3. Apply theme
applyTheme("dark", panel, textArea);

// 4. Make visible
frame.setContentPane(panel);
frame.setVisible(true);
```

### 3. State Management

**Use Preferences API for persistence:**

```java
public class ThemeManager {
    private final Preferences prefs;
    private String currentTheme;

    public ThemeManager(Class<?> prefsClass) {
        this.prefs = Preferences.userNodeForPackage(prefsClass);
        this.currentTheme = prefs.get("theme", "dark");
    }

    public void applyTheme(String theme, ...) {
        // Apply theme logic
        this.currentTheme = theme;
        prefs.put("theme", theme);
    }

    public String getCurrentTheme() {
        return currentTheme;
    }
}
```

### 4. Theme Propagation

**Ensure all new components use current theme:**

```java
public class ThemeManager {
    // Single method that ALL dialog creation calls
    public void updateDialogTheme(boolean isDark) {
        DarkDialog.setDarkTheme(isDark);
    }

    // Call this whenever theme changes
    public void applyTheme(String theme, ...) {
        boolean isDark = !theme.equals("light");

        // Update all systems
        applyDarkDialogTheme(); // or light
        updateDialogTheme(isDark);
        updateAllComponents(...);
    }
}
```

### 5. Component Creation Helpers

**Centralize themed component creation:**

```java
public class ComponentFactory {
    private final ThemeManager themeManager;

    public JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        boolean isDark = themeManager.getCurrentTheme().equals("dark");

        button.setBackground(isDark ? BUTTON_BACKGROUND : BUTTON_LIGHT);
        button.setForeground(isDark ? FOREGROUND_PRIMARY : Color.BLACK);
        // ... other properties

        return button;
    }

    public JTextField createThemedTextField(String initialValue) {
        JTextField field = new JTextField(initialValue);
        // ... themed properties
        return field;
    }
}
```

### 6. Testing Checklist

**Verify theme in all contexts:**

- [ ] Main application window
- [ ] Dialog boxes (all types: message, confirm, input)
- [ ] Context menus (right-click menus)
- [ ] Popup windows
- [ ] Split pane dividers
- [ ] Scroll pane backgrounds
- [ ] Text field caret visibility
- [ ] Tree selection colors
- [ ] Button hover states
- [ ] Border colors
- [ ] Theme switching (light → dark → light)
- [ ] Theme persistence (restart application)

---

## Code Examples

### Complete ThemeManager Skeleton

```java
public class ThemeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);
    private static final String PREF_THEME = "app.theme";

    private String currentTheme;
    private final Preferences prefs;

    public ThemeManager(Class<?> prefsClass) {
        this.prefs = Preferences.userNodeForPackage(prefsClass);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
    }

    /**
     * Applies theme to all components.
     */
    public void applyTheme(String themeName, Component rootComponent) {
        boolean isDarkTheme = !themeName.equals("light");

        // Step 1: Configure UIManager
        if (isDarkTheme) {
            applyDarkDialogTheme();
        } else {
            applyLightDialogTheme();
        }

        // Step 2: Update component tree
        SwingUtilities.updateComponentTreeUI(rootComponent);

        // Step 3: Component-specific fixes
        updateScrollPanes(rootComponent, isDarkTheme);
        updateSplitPanes(rootComponent, isDarkTheme);

        // Step 4: Update custom dialog system
        DarkDialog.setDarkTheme(isDarkTheme);

        // Step 5: Save preference
        currentTheme = themeName;
        prefs.put(PREF_THEME, themeName);

        LOGGER.info("Applied theme: {}", themeName);
    }

    private void applyDarkDialogTheme() {
        // See UIManager Configuration section above
    }

    private void applyLightDialogTheme() {
        // Similar to dark, with light colors
    }

    private void updateScrollPanes(Component comp, boolean isDark) {
        // See Recursive Component Updates section above
    }

    private void updateSplitPanes(Component comp, boolean isDark) {
        // See Recursive Component Updates section above
    }

    public String getCurrentTheme() {
        return currentTheme;
    }
}
```

### Usage in Main Application

```java
public class MyDesignerModule extends JPanel {
    private final ThemeManager themeManager;

    public MyDesignerModule() {
        themeManager = new ThemeManager(getClass());

        // Build UI
        buildUI();

        // Apply saved theme
        try {
            themeManager.applyTheme(
                themeManager.getCurrentTheme(),
                this
            );
        } catch (Exception e) {
            LOGGER.error("Failed to apply theme", e);
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Create components
        JPanel topPanel = new JPanel();
        JTextArea textArea = new JTextArea();

        // Add to hierarchy
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    private void createThemeMenu() {
        JMenu themeMenu = new JMenu("Theme");

        JMenuItem darkItem = new JMenuItem("Dark");
        darkItem.addActionListener(e -> changeTheme("dark"));

        JMenuItem lightItem = new JMenuItem("Light");
        lightItem.addActionListener(e -> changeTheme("light"));

        themeMenu.add(darkItem);
        themeMenu.add(lightItem);

        return themeMenu;
    }

    private void changeTheme(String theme) {
        try {
            themeManager.applyTheme(theme, this);
            DarkDialog.showMessage(this,
                "Theme changed to " + theme,
                "Theme");
        } catch (Exception e) {
            LOGGER.error("Failed to change theme", e);
        }
    }
}
```

---

## Common Patterns

### Pattern: Themed Button with Hover

```java
public static JButton createThemedButton(String text, boolean isDark) {
    JButton button = new JButton(text);

    Color bgColor = isDark ? BUTTON_BACKGROUND : BUTTON_LIGHT;
    Color hoverColor = isDark ? BUTTON_HOVER : BUTTON_LIGHT_HOVER;
    Color fgColor = isDark ? FOREGROUND_PRIMARY : Color.BLACK;

    button.setBackground(bgColor);
    button.setForeground(fgColor);
    button.setFocusPainted(false);
    button.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
        new EmptyBorder(5, 15, 5, 15)
    ));

    button.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            button.setBackground(hoverColor);
        }
        public void mouseExited(MouseEvent e) {
            button.setBackground(bgColor);
        }
    });

    return button;
}
```

### Pattern: Themed TextField

```java
public static JTextField createThemedTextField(String initial, boolean isDark) {
    JTextField field = new JTextField(initial != null ? initial : "");

    if (isDark) {
        field.setBackground(BACKGROUND_DARKER);
        field.setForeground(FOREGROUND_PRIMARY);
        field.setCaretColor(FOREGROUND_PRIMARY);
    } else {
        field.setBackground(Color.WHITE);
        field.setForeground(Color.BLACK);
        field.setCaretColor(Color.BLACK);
    }

    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(BORDER_DEFAULT, 1),
        new EmptyBorder(5, 5, 5, 5)
    ));

    return field;
}
```

### Pattern: File Chooser Theming

```java
public static JFileChooser createThemedFileChooser(boolean isDark) {
    JFileChooser chooser = new JFileChooser();

    if (isDark) {
        // Update UIManager before showing chooser
        UIManager.put("FileChooser.background", BACKGROUND_DARK);
        UIManager.put("FileChooser.foreground", FOREGROUND_PRIMARY);

        // Force update
        SwingUtilities.updateComponentTreeUI(chooser);

        // Recursive fix for nested components
        updateAllComponents(chooser, BACKGROUND_DARK, FOREGROUND_PRIMARY);
    }

    return chooser;
}

private static void updateAllComponents(Component c, Color bg, Color fg) {
    if (c instanceof JComponent) {
        c.setBackground(bg);
        c.setForeground(fg);
    }
    if (c instanceof Container) {
        for (Component child : ((Container) c).getComponents()) {
            updateAllComponents(child, bg, fg);
        }
    }
}
```

---

## Troubleshooting

### Issue: Theme doesn't apply to dialogs

**Symptoms:** Dialogs show light backgrounds despite dark theme.

**Solutions:**
1. Use custom dialog system (`DarkDialog`) instead of `JOptionPane`
2. If must use `JOptionPane`, create wrapper that applies theme
3. Set UIManager properties immediately before showing dialog

### Issue: Split pane divider stays light gray

**Symptoms:** Divider color doesn't match theme.

**Solutions:**
1. Use `ThemedSplitPaneUI` custom UI delegate
2. Recursively update dividers after theme change
3. Set divider background via `BasicSplitPaneUI.getDivider()`

### Issue: Context menu text invisible

**Symptoms:** Right-click menu has dark text on dark background.

**Solutions:**
1. Style popup menu AFTER adding all menu items (timing critical!)
2. Explicitly set background/foreground on menu and all items
3. Add mouse listeners for hover effects

### Issue: Scroll pane shows light background

**Symptoms:** Viewport background is light gray.

**Solutions:**
1. Set viewport background explicitly: `scrollPane.getViewport().setBackground(color)`
2. Include `Viewport.background` in UIManager
3. Recursively update all scroll panes after theme change

### Issue: Theme doesn't persist

**Symptoms:** Theme resets to default on restart.

**Solutions:**
1. Use `Preferences` API to save theme choice
2. Load saved theme in constructor before building UI
3. Ensure preference key is unique and scoped correctly

### Issue: New components don't respect theme

**Symptoms:** Components created after initial theme application are unstyled.

**Solutions:**
1. Create components via factory methods that apply current theme
2. Call `applyTheme()` after adding new components dynamically
3. Set UIManager before creating new components

### Issue: Text caret invisible

**Symptoms:** Can't see cursor in text fields.

**Solutions:**
1. Always set `setCaretColor()` to match foreground
2. Include in UIManager: `TextField.caretForeground`
3. Update when theme changes

---

## Summary

### Key Takeaways

1. **Multi-layer approach required** - UIManager + component styling + recursive updates
2. **Timing matters** - Apply themes after component creation and hierarchy assembly
3. **Custom components needed** - Some Swing components need custom UI delegates
4. **Test exhaustively** - Theme issues appear in unexpected places
5. **Centralize theme logic** - Use ThemeManager pattern for maintainability

### Essential Classes

- `ThemeManager` - Orchestrates all theme application
- `ModernTheme` - Color and style constants
- `DarkDialog` - Custom dialog system (bypasses JOptionPane)
- `ThemedSplitPaneUI` - Custom split pane divider

### Critical Patterns

- Recursive component tree updates
- UIManager configuration (comprehensive properties)
- Theme persistence via Preferences API
- Component-specific styling after UIManager

### Common Pitfalls to Avoid

- ❌ Relying solely on UIManager
- ❌ Styling popup menus before adding items
- ❌ Forgetting to set caret colors
- ❌ Not updating scroll pane viewports
- ❌ Ignoring split pane dividers
- ❌ Using JOptionPane in Ignition Designer
- ❌ Not testing theme switching

### Success Checklist

- ✅ Theme manager implemented
- ✅ Color constants defined
- ✅ Custom dialog system created
- ✅ UIManager comprehensively configured
- ✅ Recursive updates implemented
- ✅ Split pane custom UI created
- ✅ Context menus styled correctly
- ✅ Theme persistence working
- ✅ All components tested in both themes
- ✅ Theme switching works without restart

---

## Additional Resources

### Recommended Reading

- Java Swing UIManager properties reference
- Ignition SDK documentation on Designer modules
- Look and Feel developer guide

### Tools

- UIManager property inspector (for debugging)
- Swing component hierarchy viewer
- Theme testing checklist

### Version Notes

This guide is based on lessons learned from extensive UX/theme work in:
- Ignition 8.3 SDK
- Java Swing (various versions)
- Multiple Designer-scoped modules

---

**Document Version:** 1.0
**Last Updated:** 2025-10-18
**Maintained by:** Project team

**License:** Use freely in your Ignition Designer projects
