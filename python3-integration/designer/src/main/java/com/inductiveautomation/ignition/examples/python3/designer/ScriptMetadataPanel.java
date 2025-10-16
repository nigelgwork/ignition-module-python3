package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Panel displaying metadata for a selected script.
 * Shows: name, author, created date, modified date, version, description.
 */
public class ScriptMetadataPanel extends JPanel {

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final JLabel nameLabel;
    private final JLabel authorLabel;
    private final JLabel createdLabel;
    private final JLabel modifiedLabel;
    private final JLabel versionLabel;
    private final JTextArea descriptionArea;

    public ScriptMetadataPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT),
                        "Script Information",
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        ModernTheme.FONT_REGULAR,
                        ModernTheme.FOREGROUND_PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setPreferredSize(new Dimension(250, 180));
        setBackground(ModernTheme.PANEL_BACKGROUND);

        // Create labels
        nameLabel = createValueLabel();
        authorLabel = createValueLabel();
        createdLabel = createValueLabel();
        modifiedLabel = createValueLabel();
        versionLabel = createValueLabel();

        // Description text area
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        descriptionArea.setBackground(ModernTheme.BACKGROUND_DARKER);
        descriptionArea.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        descriptionArea.setCaretColor(ModernTheme.FOREGROUND_PRIMARY);

        // Layout
        JPanel fieldsPanel = new JPanel(new GridLayout(5, 2, 5, 3));
        fieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        fieldsPanel.setBackground(ModernTheme.PANEL_BACKGROUND);

        fieldsPanel.add(createKeyLabel("Name:"));
        fieldsPanel.add(nameLabel);

        fieldsPanel.add(createKeyLabel("Author:"));
        fieldsPanel.add(authorLabel);

        fieldsPanel.add(createKeyLabel("Created:"));
        fieldsPanel.add(createdLabel);

        fieldsPanel.add(createKeyLabel("Modified:"));
        fieldsPanel.add(modifiedLabel);

        fieldsPanel.add(createKeyLabel("Version:"));
        fieldsPanel.add(versionLabel);

        add(fieldsPanel, BorderLayout.NORTH);

        // Description section
        JPanel descPanel = new JPanel(new BorderLayout(3, 3));
        descPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
        descPanel.setBackground(ModernTheme.PANEL_BACKGROUND);
        descPanel.add(createKeyLabel("Description:"), BorderLayout.NORTH);

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        descScroll.setBackground(ModernTheme.BACKGROUND_DARKER);
        descScroll.getViewport().setBackground(ModernTheme.BACKGROUND_DARKER);
        descScroll.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_DEFAULT));
        descPanel.add(descScroll, BorderLayout.CENTER);

        add(descPanel, BorderLayout.CENTER);

        // Initially show "No script selected"
        clear();
    }

    /**
     * Displays metadata for a script.
     *
     * @param metadata the script metadata
     */
    public void displayMetadata(ScriptMetadata metadata) {
        if (metadata == null) {
            clear();
            return;
        }

        nameLabel.setText(metadata.getName());
        authorLabel.setText(metadata.getAuthor() != null ? metadata.getAuthor() : "Unknown");

        createdLabel.setText(formatDate(metadata.getCreatedDate()));
        modifiedLabel.setText(formatDate(metadata.getLastModified()));

        versionLabel.setText(metadata.getVersion() != null ? metadata.getVersion() : "1.0");
        descriptionArea.setText(metadata.getDescription() != null ? metadata.getDescription() : "");
    }

    /**
     * Clears all metadata fields.
     */
    public void clear() {
        nameLabel.setText("—");
        authorLabel.setText("—");
        createdLabel.setText("—");
        modifiedLabel.setText("—");
        versionLabel.setText("—");
        descriptionArea.setText("");
    }

    /**
     * Formats an ISO 8601 date string to human-readable format.
     *
     * @param isoDate ISO 8601 date string
     * @return formatted date string
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "—";
        }

        try {
            Instant instant = Instant.parse(isoDate);
            return DATE_FORMAT.format(instant);
        } catch (Exception e) {
            // If parsing fails, return original string
            return isoDate;
        }
    }

    /**
     * Creates a key label (e.g., "Name:", "Author:").
     *
     * @param text the label text
     * @return configured label
     */
    private JLabel createKeyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 11));
        label.setForeground(ModernTheme.FOREGROUND_SECONDARY);
        return label;
    }

    /**
     * Creates a value label (displays actual metadata values).
     *
     * @return configured label
     */
    private JLabel createValueLabel() {
        JLabel label = new JLabel("—");
        label.setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 11));
        label.setForeground(ModernTheme.FOREGROUND_PRIMARY);
        return label;
    }
}
