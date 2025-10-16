package com.inductiveautomation.ignition.examples.python3.designer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Custom tree cell renderer for the script browser.
 * Displays custom icons for folders and Python scripts.
 */
public class ScriptTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final Icon FOLDER_ICON = createFolderIcon();
    private static final Icon FOLDER_OPEN_ICON = createFolderOpenIcon();
    private static final Icon SCRIPT_ICON = createScriptIcon();

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        // Apply modern theme colors
        if (selected) {
            setBackground(ModernTheme.ACCENT_PRIMARY);
            setForeground(Color.WHITE);
        } else {
            setBackground(ModernTheme.TREE_BACKGROUND);
            setForeground(ModernTheme.FOREGROUND_PRIMARY);
        }

        // Ensure proper alignment of icon and text
        setIconTextGap(8);  // Spacing between icon and text
        setHorizontalTextPosition(SwingConstants.RIGHT);  // Text to the right of icon
        setVerticalTextPosition(SwingConstants.CENTER);   // Text centered with icon
        setHorizontalAlignment(SwingConstants.LEFT);      // Align label to left
        setVerticalAlignment(SwingConstants.CENTER);      // Center content vertically

        if (value instanceof ScriptTreeNode) {
            ScriptTreeNode node = (ScriptTreeNode) value;

            if (node.isFolder()) {
                setIcon(expanded ? FOLDER_OPEN_ICON : FOLDER_ICON);
                setFont(ModernTheme.withSize(ModernTheme.FONT_BOLD, 14));
            } else if (node.isScript()) {
                setIcon(SCRIPT_ICON);
                setFont(ModernTheme.withSize(ModernTheme.FONT_REGULAR, 13));
            }
        } else {
            setFont(ModernTheme.FONT_REGULAR);
        }

        // Set consistent padding - reduced to minimize empty space
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        return this;
    }

    /**
     * Creates a folder icon (closed).
     */
    private static Icon createFolderIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Center vertically within 16px height
                int yOffset = 2;  // Center the 12px tall icon within 16px space

                // Folder body
                g2.setColor(new Color(255, 200, 60));
                g2.fillRoundRect(x + 1, y + yOffset + 4, 14, 10, 2, 2);

                // Folder tab
                g2.fillRoundRect(x + 1, y + yOffset + 2, 6, 4, 2, 2);

                // Folder outline
                g2.setColor(new Color(200, 150, 40));
                g2.drawRoundRect(x + 1, y + yOffset + 4, 14, 10, 2, 2);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    /**
     * Creates an open folder icon.
     */
    private static Icon createFolderOpenIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Center vertically within 16px height
                int yOffset = 2;  // Center the 12px tall icon within 16px space

                // Folder body (open)
                int[] xPoints = {x + 1, x + 15, x + 15, x + 3};
                int[] yPoints = {y + yOffset + 6, y + yOffset + 4, y + yOffset + 14, y + yOffset + 14};
                g2.setColor(new Color(255, 210, 80));
                g2.fillPolygon(xPoints, yPoints, 4);

                // Folder tab
                g2.setColor(new Color(255, 200, 60));
                g2.fillRoundRect(x + 1, y + yOffset + 2, 6, 4, 2, 2);

                // Folder outline
                g2.setColor(new Color(200, 150, 40));
                g2.drawPolygon(xPoints, yPoints, 4);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    /**
     * Creates a Python script icon.
     */
    private static Icon createScriptIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Center vertically within 16px height (15px tall icon + 1px offset)
                int yOffset = 0;  // Script icon is already 15px tall, fits well in 16px

                // Document background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x + 2, y + yOffset, 13, 15, 2, 2);

                // Document outline
                g2.setColor(new Color(100, 100, 100));
                g2.drawRoundRect(x + 2, y + yOffset, 13, 15, 2, 2);

                // Python logo-inspired colors (blue and yellow)
                g2.setColor(new Color(55, 118, 171));  // Python blue
                g2.fillOval(x + 5, y + yOffset + 3, 3, 3);
                g2.fillRect(x + 5, y + yOffset + 5, 3, 2);

                g2.setColor(new Color(255, 212, 59));  // Python yellow
                g2.fillOval(x + 10, y + yOffset + 8, 3, 3);
                g2.fillRect(x + 10, y + yOffset + 8, 3, 2);

                // "Py" text hint
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(new Font("SansSerif", Font.BOLD, 6));
                g2.drawString("py", x + 5, y + yOffset + 14);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }
}
