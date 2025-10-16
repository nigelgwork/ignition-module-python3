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

        // Set readable font size and spacing
        setFont(new Font("SansSerif", Font.PLAIN, 13));
        setIconTextGap(6);  // Add spacing between icon and text

        if (value instanceof ScriptTreeNode) {
            ScriptTreeNode node = (ScriptTreeNode) value;

            if (node.isFolder()) {
                setIcon(expanded ? FOLDER_OPEN_ICON : FOLDER_ICON);
                setFont(new Font("SansSerif", Font.BOLD, 13));
            } else if (node.isScript()) {
                setIcon(SCRIPT_ICON);
                setFont(new Font("SansSerif", Font.PLAIN, 13));
            }
        }

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

                // Folder body
                g2.setColor(new Color(255, 200, 60));
                g2.fillRoundRect(x, y + 4, 14, 10, 2, 2);

                // Folder tab
                g2.fillRoundRect(x, y + 2, 6, 4, 2, 2);

                // Folder outline
                g2.setColor(new Color(200, 150, 40));
                g2.drawRoundRect(x, y + 4, 14, 10, 2, 2);
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

                // Folder body (open)
                int[] xPoints = {x, x + 14, x + 14, x + 2};
                int[] yPoints = {y + 6, y + 4, y + 14, y + 14};
                g2.setColor(new Color(255, 210, 80));
                g2.fillPolygon(xPoints, yPoints, 4);

                // Folder tab
                g2.setColor(new Color(255, 200, 60));
                g2.fillRoundRect(x, y + 2, 6, 4, 2, 2);

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

                // Document background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(x + 1, y, 13, 15, 2, 2);

                // Document outline
                g2.setColor(new Color(100, 100, 100));
                g2.drawRoundRect(x + 1, y, 13, 15, 2, 2);

                // Python logo-inspired colors (blue and yellow)
                g2.setColor(new Color(55, 118, 171));  // Python blue
                g2.fillOval(x + 4, y + 3, 3, 3);
                g2.fillRect(x + 4, y + 5, 3, 2);

                g2.setColor(new Color(255, 212, 59));  // Python yellow
                g2.fillOval(x + 9, y + 8, 3, 3);
                g2.fillRect(x + 9, y + 8, 3, 2);

                // "Py" text hint
                g2.setColor(new Color(80, 80, 80));
                g2.setFont(new Font("SansSerif", Font.BOLD, 6));
                g2.drawString("py", x + 4, y + 14);
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
