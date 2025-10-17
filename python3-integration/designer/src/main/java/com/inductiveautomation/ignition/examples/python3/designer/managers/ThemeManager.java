package com.inductiveautomation.ignition.examples.python3.designer.managers;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Manages IDE theme application.
 * Self-contained theme management extracted from Python3IDE.
 *
 * v2.0.0: Extracted from Python3IDE.java monolith
 */
public class ThemeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeManager.class);
    private static final String PREF_THEME = "python3ide.theme";

    private String currentTheme;
    private final Preferences prefs;

    public ThemeManager(Class<?> prefsClass) {
        this.prefs = Preferences.userNodeForPackage(prefsClass);
        this.currentTheme = prefs.get(PREF_THEME, "dark");
    }

    /**
     * Applies theme to IDE components.
     */
    public void applyTheme(String themeName, Component rootComponent, RSyntaxTextArea codeEditor,
                           JTextArea outputArea, JTextArea errorArea, JTree scriptTree) throws IOException {
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
            case "vs":
                theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/vs.xml"));
                isDarkTheme = true;
                break;
            default:
                theme = Theme.load(getClass().getResourceAsStream(
                        "/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
                isDarkTheme = false;
                break;
        }

        theme.apply(codeEditor);

        if (isDarkTheme) {
            applyDarkTheme(outputArea, errorArea, scriptTree, rootComponent);
        } else {
            applyLightTheme(outputArea, errorArea, scriptTree, rootComponent);
        }

        SwingUtilities.updateComponentTreeUI(rootComponent);
        updateScrollPaneTheme(rootComponent, isDarkTheme);
        updateSplitPaneDividers(rootComponent, isDarkTheme);

        currentTheme = themeName;
        prefs.put(PREF_THEME, themeName);
        LOGGER.info("Applied theme: {}", themeName);
    }

    private void applyDarkTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        outputArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);
        errorArea.setCaretColor(ModernTheme.ERROR);

        scriptTree.setBackground(ModernTheme.TREE_BACKGROUND);
        scriptTree.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        updateComponent(root, ModernTheme.BACKGROUND_DARK);
        applyDarkDialogTheme();
    }

    private void applyLightTheme(JTextArea outputArea, JTextArea errorArea, JTree scriptTree, Component root) {
        outputArea.setBackground(Color.WHITE);
        outputArea.setForeground(Color.BLACK);
        outputArea.setCaretColor(Color.BLACK);

        errorArea.setBackground(Color.WHITE);
        errorArea.setForeground(new Color(180, 0, 0));
        errorArea.setCaretColor(new Color(180, 0, 0));

        scriptTree.setBackground(Color.WHITE);
        scriptTree.setForeground(Color.BLACK);

        updateComponent(root, Color.WHITE);
        applyLightDialogTheme();
    }

    private void applyDarkDialogTheme() {
        // UX Fix v2.0.9: Enhanced dialog theming for consistent dark theme
        UIManager.put("OptionPane.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("OptionPane.messageForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("OptionPane.messagebackground", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("Panel.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("TextField.background", ModernTheme.BACKGROUND_DARKER);
        UIManager.put("TextField.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.caretForeground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("TextField.selectionBackground", ModernTheme.ACCENT_PRIMARY);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        UIManager.put("Label.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Label.background", ModernTheme.PANEL_BACKGROUND);
        UIManager.put("Button.background", ModernTheme.BUTTON_BACKGROUND);
        UIManager.put("Button.foreground", ModernTheme.FOREGROUND_PRIMARY);
        UIManager.put("Button.select", ModernTheme.BUTTON_HOVER);
    }

    private void applyLightDialogTheme() {
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", Color.BLACK);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", Color.BLACK);
        UIManager.put("Label.foreground", Color.BLACK);
        UIManager.put("Button.background", new Color(238, 238, 238));
        UIManager.put("Button.foreground", Color.BLACK);
    }

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

    private void updateScrollPaneTheme(Component comp, boolean isDarkTheme) {
        if (comp instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) comp;
            scrollPane.getViewport().setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
            scrollPane.setBackground(isDarkTheme ? ModernTheme.BACKGROUND_DARK : Color.WHITE);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateScrollPaneTheme(child, isDarkTheme);
            }
        }
    }

    private void updateSplitPaneDividers(Component comp, boolean isDarkTheme) {
        if (comp instanceof JSplitPane) {
            JSplitPane splitPane = (JSplitPane) comp;
            if (splitPane.getUI() instanceof BasicSplitPaneUI) {
                Color dividerColor = isDarkTheme ? ModernTheme.BACKGROUND_DARKER : new Color(200, 200, 200);
                ((BasicSplitPaneUI) splitPane.getUI()).getDivider().setBackground(dividerColor);
            }
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                updateSplitPaneDividers(child, isDarkTheme);
            }
        }
    }

    public String mapThemeNameToKey(String displayName) {
        switch (displayName) {
            case "Dark":
                return "dark";
            case "VS Code Dark+":
                return "vs";
            case "Monokai":
                return "monokai";
            default:
                return "dark";
        }
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public String getSavedThemePreference() {
        return prefs.get(PREF_THEME, "dark");
    }
}
