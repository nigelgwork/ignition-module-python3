package com.inductiveautomation.ignition.examples.python3.designer.ui;

import com.inductiveautomation.ignition.examples.python3.designer.ModernTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * Panel containing code editor and execution results.
 * Simplified for v2.0 architecture.
 *
 * v2.0.0: Extracted from Python3IDE_v1_9.java monolith
 */
public class EditorPanel extends JPanel {

    private final RSyntaxTextArea codeEditor;
    private final JTextArea outputArea;
    private final JTextArea errorArea;

    public EditorPanel() {
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BACKGROUND_DARK);

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
}
