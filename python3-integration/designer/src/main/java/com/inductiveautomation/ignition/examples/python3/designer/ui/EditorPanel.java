package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Panel containing code editor and execution results.
 * Simplified for v2.0 architecture.
 *
 * v2.0.0: Extracted from Python3IDE_v1_9.java monolith
 * v2.0.6: Added find/replace toolbar
 */
public class EditorPanel extends JPanel {

    private final RSyntaxTextArea codeEditor;
    private final JTextArea outputArea;
    private final JTextArea errorArea;
    private final JTextField findField;
    private final JTextField replaceField;
    private final JCheckBox matchCaseCheckbox;

    public EditorPanel() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

        // Find/Replace toolbar
        JPanel findReplacePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        findReplacePanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        findField = new JTextField(15);
        findField.setFont(ModernTheme.FONT_REGULAR);

        replaceField = new JTextField(15);
        replaceField.setFont(ModernTheme.FONT_REGULAR);

        matchCaseCheckbox = new JCheckBox("Match Case");
        matchCaseCheckbox.setFont(ModernTheme.FONT_REGULAR);
        matchCaseCheckbox.setBackground(ModernTheme.PANEL_BACKGROUND);
        matchCaseCheckbox.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        JButton findNextButton = new JButton("Find Next");
        findNextButton.setFont(ModernTheme.FONT_REGULAR);
        findNextButton.addActionListener(e -> findNext());

        JButton replaceButton = new JButton("Replace");
        replaceButton.setFont(ModernTheme.FONT_REGULAR);
        replaceButton.addActionListener(e -> replace());

        JButton replaceAllButton = new JButton("Replace All");
        replaceAllButton.setFont(ModernTheme.FONT_REGULAR);
        replaceAllButton.addActionListener(e -> replaceAll());

        findReplacePanel.add(new JLabel("Find:"));
        findReplacePanel.add(findField);
        findReplacePanel.add(new JLabel("Replace:"));
        findReplacePanel.add(replaceField);
        findReplacePanel.add(matchCaseCheckbox);
        findReplacePanel.add(findNextButton);
        findReplacePanel.add(replaceButton);
        findReplacePanel.add(replaceAllButton);

        add(findReplacePanel, BorderLayout.NORTH);

        // Code editor
        codeEditor = new RSyntaxTextArea(20, 60);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setFont(ModernTheme.withSize(ModernTheme.FONT_MONOSPACE, 13));
        codeEditor.setTabSize(4);
        codeEditor.setBackground(ModernTheme.BACKGROUND_DARKER);
        codeEditor.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        RTextScrollPane editorScroll = new RTextScrollPane(codeEditor);
        editorScroll.setLineNumbersEnabled(true);

        // Output/error areas
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(ModernTheme.FONT_MONOSPACE);
        outputArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        outputArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);

        errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setFont(ModernTheme.FONT_MONOSPACE);
        errorArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        errorArea.setForeground(ModernTheme.ERROR);

        JTabbedPane outputTabs = new JTabbedPane();
        outputTabs.addTab("Output", new JScrollPane(outputArea));
        outputTabs.addTab("Errors", new JScrollPane(errorArea));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(editorScroll);
        splitPane.setBottomComponent(outputTabs);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);
    }

    public String getCode() {
        return codeEditor.getText();
    }

    public void setCode(String code) {
        codeEditor.setText(code != null ? code : "");
    }

    public void setOutput(String text) {
        outputArea.setText(text != null ? text : "");
    }

    public void setError(String text) {
        errorArea.setText(text != null ? text : "");
    }

    public void clearOutput() {
        outputArea.setText("");
        errorArea.setText("");
    }

    public RSyntaxTextArea getCodeEditor() {
        return codeEditor;
    }

    public JTextArea getOutputArea() {
        return outputArea;
    }

    public JTextArea getErrorArea() {
        return errorArea;
    }

    private void findNext() {
        String searchText = findField.getText();
        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.find(codeEditor, context);
    }

    private void replace() {
        String searchText = findField.getText();
        String replaceText = replaceField.getText();

        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setReplaceWith(replaceText != null ? replaceText : "");
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.replace(codeEditor, context);
    }

    private void replaceAll() {
        String searchText = findField.getText();
        String replaceText = replaceField.getText();

        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(searchText);
        context.setReplaceWith(replaceText != null ? replaceText : "");
        context.setMatchCase(matchCaseCheckbox.isSelected());
        context.setSearchForward(true);
        context.setWholeWord(false);

        SearchEngine.replaceAll(codeEditor, context);
    }
}
