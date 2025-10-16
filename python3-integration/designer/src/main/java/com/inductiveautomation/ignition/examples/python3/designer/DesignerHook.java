package com.inductiveautomation.ignition.examples.python3.designer;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Designer hook for the Python 3 Integration module (v1.7.0+).
 *
 * <p>This hook integrates the Python 3 IDE into the Ignition Designer by adding
 * a menu item to the Tools menu. The IDE communicates with the Gateway via REST API.</p>
 *
 * <p>Lifecycle:</p>
 * <ul>
 *   <li>startup() - Adds "Python 3 IDE" menu item to Tools menu</li>
 *   <li>shutdown() - Closes IDE window if open</li>
 * </ul>
 *
 * <p><strong>Architecture Change (v1.7.0):</strong> This version uses REST API instead of RPC
 * for Designer-Gateway communication, providing better performance and reliability.</p>
 */
public class DesignerHook extends AbstractDesignerModuleHook {
    private static final Logger LOGGER = LoggerFactory.getLogger(DesignerHook.class);

    private DesignerContext context;
    private JFrame ideFrame;

    /**
     * Called when the Designer module is starting up.
     *
     * @param context the Designer context
     * @param activationState the license state
     */
    @Override
    public void startup(DesignerContext context, LicenseState activationState) throws Exception {
        super.startup(context, activationState);

        // CRITICAL: Store context immediately at startup
        this.context = context;

        LOGGER.info("Python 3 Integration Designer module starting up");

        // Add menu item to Tools menu (call directly, no deferral needed)
        addToolsMenuItem();

        LOGGER.info("Python 3 Integration Designer module startup complete");
        LOGGER.info("Python 3 IDE available from Tools menu (communicates with Gateway via REST API)");
    }

    /**
     * Called when the Designer module is shutting down.
     */
    @Override
    public void shutdown() {
        super.shutdown();

        LOGGER.info("Python 3 Integration Designer module shutting down");

        // Close IDE window if open
        if (ideFrame != null && ideFrame.isVisible()) {
            ideFrame.dispose();
            ideFrame = null;
        }

        LOGGER.info("Python 3 Integration Designer module shutdown complete");
    }

    /**
     * Adds the "Python 3 IDE" menu item to the Tools menu.
     */
    private void addToolsMenuItem() {
        try {
            LOGGER.info("Attempting to add Python 3 IDE menu item...");

            // Get the main Designer frame
            Frame designerFrame = context.getFrame();
            LOGGER.info("Designer frame type: {}", designerFrame != null ? designerFrame.getClass().getName() : "null");

            if (!(designerFrame instanceof JFrame)) {
                LOGGER.warn("Designer frame is not a JFrame, cannot add menu item");
                return;
            }

            JFrame jFrame = (JFrame) designerFrame;
            JMenuBar menuBar = jFrame.getJMenuBar();

            if (menuBar == null) {
                LOGGER.warn("Could not get menu bar from Designer frame");
                return;
            }

            LOGGER.info("Got menu bar successfully");

            // Log all available menus
            LOGGER.info("Available menus (count={}): ", menuBar.getMenuCount());
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null) {
                    LOGGER.info("  Menu {}: '{}'", i, menu.getText());
                }
            }

            // Find or create Tools menu
            JMenu toolsMenu = findMenu(menuBar, "Tools");
            if (toolsMenu == null) {
                LOGGER.info("Tools menu not found, creating it");
                toolsMenu = new JMenu("Tools");
                menuBar.add(toolsMenu);
                LOGGER.info("Created new Tools menu");
            } else {
                LOGGER.info("Found existing Tools menu");
            }

            // Add separator if menu is not empty
            if (toolsMenu.getItemCount() > 0) {
                toolsMenu.addSeparator();
            }

            // Create menu item
            JMenuItem python3IDEItem = new JMenuItem("Python 3 IDE");
            python3IDEItem.setToolTipText("Open the Python 3 IDE for testing Python code on the Gateway");
            python3IDEItem.addActionListener(e -> openPython3IDE());

            toolsMenu.add(python3IDEItem);

            LOGGER.info("Successfully added 'Python 3 IDE' menu item to Tools menu");

        } catch (Exception e) {
            LOGGER.error("Failed to add Tools menu item", e);
        }
    }

    /**
     * Finds a menu by name in the menu bar.
     *
     * @param menuBar the menu bar to search
     * @param menuName the menu name to find
     * @return the menu, or null if not found
     */
    private JMenu findMenu(JMenuBar menuBar, String menuName) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && menuName.equals(menu.getText())) {
                return menu;
            }
        }
        return null;
    }

    /**
     * Opens the Python 3 IDE window.
     */
    private void openPython3IDE() {
        LOGGER.info("Opening Python 3 IDE");

        // If window already exists, just bring it to front
        if (ideFrame != null && ideFrame.isVisible()) {
            ideFrame.toFront();
            ideFrame.requestFocus();
            return;
        }

        // Create new IDE window
        SwingUtilities.invokeLater(() -> {
            try {
                // Create IDE panel
                Python3IDE idePanel = new Python3IDE(context);

                // Create frame
                ideFrame = new JFrame("Python 3 IDE - Ignition Designer");
                ideFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                ideFrame.setContentPane(idePanel);

                // Set size and location
                ideFrame.setSize(1000, 700);
                ideFrame.setLocationRelativeTo(context.getFrame());

                // Show window
                ideFrame.setVisible(true);

                LOGGER.info("Python 3 IDE window opened");

            } catch (Exception e) {
                LOGGER.error("Failed to open Python 3 IDE", e);

                JOptionPane.showMessageDialog(
                        context.getFrame(),
                        "Failed to open Python 3 IDE: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
