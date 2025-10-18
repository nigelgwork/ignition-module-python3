package com.inductiveautomation.ignition.examples.python3.designer.ui;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Custom TitledBorder implementation that eliminates ALL internal spacing/padding.
 *
 * Standard TitledBorder has a hardcoded EDGE_SPACING constant (2+ pixels) that creates
 * white gaps between the border and the component content. This custom implementation
 * overrides the insets calculation to return ZERO spacing on all edges.
 *
 * This solves the persistent "white lines" issue where lighter panel backgrounds showed
 * through the TitledBorder's internal padding as visible white gaps.
 *
 * Usage:
 * <pre>
 * ZeroInsetTitledBorder border = new ZeroInsetTitledBorder(
 *     BorderFactory.createLineBorder(Color.GRAY),
 *     "Title Text",
 *     TitledBorder.DEFAULT_JUSTIFICATION,
 *     TitledBorder.DEFAULT_POSITION,
 *     new Font("Dialog", Font.PLAIN, 12),
 *     Color.WHITE
 * );
 * panel.setBorder(border);
 * </pre>
 *
 * @version 2.5.12
 * @since 2.5.12
 */
public class ZeroInsetTitledBorder extends TitledBorder {

    /**
     * Creates a ZeroInsetTitledBorder with the specified border and title.
     *
     * @param border the Border to use
     * @param title the title the border should display
     */
    public ZeroInsetTitledBorder(Border border, String title) {
        super(border, title);
    }

    /**
     * Creates a ZeroInsetTitledBorder with full customization options.
     *
     * @param border the Border to use
     * @param title the title the border should display
     * @param titleJustification the justification for the title
     * @param titlePosition the position for the title
     * @param titleFont the Font to use for the title
     * @param titleColor the Color to use for the title text
     */
    public ZeroInsetTitledBorder(Border border, String title, int titleJustification,
                                  int titlePosition, Font titleFont, Color titleColor) {
        super(border, title, titleJustification, titlePosition, titleFont, titleColor);
    }

    /**
     * Returns the insets of the border - OVERRIDDEN TO RETURN ZERO SPACING.
     *
     * Standard TitledBorder adds EDGE_SPACING (2px) + TEXT_SPACING (2px) + border insets.
     * This implementation returns ZERO insets to eliminate all internal padding.
     *
     * The title text will still render, but there will be NO white gaps between
     * the border line and the component content.
     *
     * @param c the component for which this border insets value applies
     * @return an Insets object with all values set to 0
     */
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Reinitialize the insets parameter with this Border's current Insets.
     * OVERRIDDEN TO RETURN ZERO SPACING.
     *
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     * @return the insets object (same as parameter, reinitialized to 0,0,0,0)
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = 0;
        insets.left = 0;
        insets.bottom = 0;
        insets.right = 0;
        return insets;
    }

    /**
     * Returns whether or not the border is opaque.
     * We return false to allow the parent component's background to show through.
     *
     * @return false
     */
    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
