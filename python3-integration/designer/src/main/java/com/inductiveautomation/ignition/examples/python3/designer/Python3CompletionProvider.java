package com.inductiveautomation.ignition.examples.python3.designer;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom completion provider for Python code that uses the Gateway's Jedi-powered
 * completion engine to provide intelligent auto-completion suggestions.
 */
public class Python3CompletionProvider extends DefaultCompletionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3CompletionProvider.class);

    private final Python3RestClient restClient;

    public Python3CompletionProvider(Python3RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    protected List<Completion> getCompletionsImpl(JTextComponent comp) {
        List<Completion> completions = new ArrayList<>();

        if (!(comp instanceof RSyntaxTextArea)) {
            return completions;
        }

        RSyntaxTextArea textArea = (RSyntaxTextArea) comp;

        try {
            // Get cursor position
            int caretPos = textArea.getCaretPosition();
            int lineNum = textArea.getLineOfOffset(caretPos);
            int lineStart = textArea.getLineStartOffset(lineNum);
            int column = caretPos - lineStart;

            // Get all code in the editor
            String code = textArea.getText();

            // Convert to 1-based line number for Python
            int pythonLine = lineNum + 1;

            LOGGER.debug("Getting completions at line {}, column {}", pythonLine, column);

            // Call REST API to get completions
            List<CompletionResult> results = restClient.getCompletions(code, pythonLine, column);

            // Convert CompletionResult objects to RSyntaxTextArea Completion objects
            for (CompletionResult result : results) {
                String replacementText = result.getComplete() != null ? result.getComplete() : result.getText();
                String shortDesc = result.getDescription();
                String summary = buildSummary(result);

                BasicCompletion completion = new BasicCompletion(this, replacementText, shortDesc, summary);
                completions.add(completion);
            }

            LOGGER.debug("Providing {} completions", completions.size());

        } catch (BadLocationException e) {
            LOGGER.error("Failed to get cursor position", e);
        } catch (Exception e) {
            LOGGER.warn("Failed to get completions from Gateway", e);
            // Return empty list on error - don't break the user experience
        }

        return completions;
    }

    /**
     * Builds a rich HTML summary for the completion popup.
     *
     * @param result the completion result
     * @return HTML-formatted summary
     */
    private String buildSummary(CompletionResult result) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='width: 300px; padding: 5px;'>");

        // Type badge
        if (result.getType() != null) {
            String typeColor = getTypeColor(result.getType());
            html.append("<span style='background-color: ").append(typeColor)
                .append("; color: white; padding: 2px 6px; border-radius: 3px; font-size: 10px; font-weight: bold;'>")
                .append(result.getType().toUpperCase())
                .append("</span> ");
        }

        // Completion text
        html.append("<b>").append(escapeHtml(result.getText())).append("</b>");

        // Signature
        if (result.getSignature() != null && !result.getSignature().isEmpty()) {
            html.append("<br><code style='color: #666;'>")
                .append(escapeHtml(result.getSignature()))
                .append("</code>");
        }

        // Description
        if (result.getDescription() != null && !result.getDescription().isEmpty()) {
            html.append("<br><p style='margin-top: 5px; color: #333;'>")
                .append(escapeHtml(result.getDescription()))
                .append("</p>");
        }

        // Docstring (first 200 chars)
        if (result.getDocstring() != null && !result.getDocstring().isEmpty()) {
            String docstring = result.getDocstring();
            if (docstring.length() > 200) {
                docstring = docstring.substring(0, 200) + "...";
            }
            html.append("<br><p style='margin-top: 5px; color: #555; font-size: 11px; font-style: italic;'>")
                .append(escapeHtml(docstring))
                .append("</p>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Gets a color for the completion type badge.
     *
     * @param type the completion type (function, class, module, etc.)
     * @return hex color code
     */
    private String getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "function":
            case "method":
                return "#3776AB";  // Python blue
            case "class":
                return "#FFD43B";  // Python yellow
            case "module":
                return "#646464";  // Gray
            case "keyword":
                return "#FF6B6B";  // Red
            case "variable":
            case "instance":
                return "#4ECDC4";  // Teal
            default:
                return "#95A5A6";  // Default gray
        }
    }

    /**
     * Escapes HTML special characters.
     *
     * @param text the text to escape
     * @return escaped HTML
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    @Override
    public String getAlreadyEnteredText(JTextComponent comp) {
        String text = "";
        try {
            // Get the text from the start of the current word to the cursor
            int caretPos = comp.getCaretPosition();
            int start = caretPos;

            // Find the start of the current word (letter, digit, or underscore)
            while (start > 0) {
                char ch = comp.getText(start - 1, 1).charAt(0);
                if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.') {
                    break;
                }
                start--;
            }

            text = comp.getText(start, caretPos - start);

        } catch (BadLocationException e) {
            LOGGER.error("Failed to get already entered text", e);
        }

        return text;
    }
}
