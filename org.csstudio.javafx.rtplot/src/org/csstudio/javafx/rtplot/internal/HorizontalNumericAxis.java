/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** 'X' or 'horizontal' axis for numbers.
 *  @see TimeAxis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HorizontalNumericAxis extends NumericAxis
{
    /** Create axis with label and listener. */
    public HorizontalNumericAxis(final String name, final PlotPartListener listener)
    {
        super(name, listener,
              true,       // Horizontal
              0.0, 10.0); // Initial range
    }

    /** {@inheritDoc} */
    @Override
    public final int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        logger.log(Level.FINE,  "XAxis layout");

        if (! isVisible())
            return 0;

        gc.setFont(label_font);
        final int label_size = gc.getFontMetrics().getHeight();
        gc.setFont(scale_font);
        final int scale_size = gc.getFontMetrics().getHeight();
        // Need room for ticks, tick labels, and axis label
        return TICK_LENGTH + label_size + scale_size;
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D gc, final Rectangle plot_bounds)
    {
        if (! isVisible())
            return;

        final Rectangle region = getBounds();

        final Stroke old_width = gc.getStroke();
        final Color old_fg = gc.getColor();
        gc.setColor(GraphicsUtils.convert(getColor()));
        gc.setFont(scale_font);

        super.paint(gc);

        // Axis and Tick marks
        gc.drawLine(region.x, region.y, region.x + region.width-1, region.y);

        computeTicks(gc);

        // Major tick marks
        for (MajorTick<Double> tick : ticks.getMajorTicks())
        {
            final int x = getScreenCoord(tick.getValue());
            gc.setStroke(TICK_STROKE);
            gc.drawLine(x, region.y, x, region.y + TICK_LENGTH - 1);

            // Grid line
            if (show_grid)
            {   // Dashed line
                gc.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[] { 5 }, 0));
                gc.drawLine(x, plot_bounds.y, x, plot_bounds.y + plot_bounds.height-1);
            }
            gc.setStroke(old_width);

            // Tick Label
            drawTickLabel(gc, x, tick.getLabel());
        }

        // Minor tick marks
        for (MinorTick<Double> tick : ticks.getMinorTicks())
        {
            final int x = getScreenCoord(tick.getValue());
            gc.drawLine(x, region.y, x, region.y + TICK_LENGTH - 1);
        }


        // Label: centered at bottom of region
        gc.setFont(label_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, getName());
        gc.drawString(getName(),
                      region.x + (region.width - metrics.width)/2,
                      region.y + metrics.y + region.height - metrics.height);
        gc.setColor(old_fg);
    }

    private void drawTickLabel(final Graphics2D gc, final int x, final String mark)
    {
        final Rectangle region = getBounds();
        gc.setFont(scale_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, mark);
        int tx = x - metrics.width/2;
        // Correct location of rightmost label to remain within region
        if (tx + metrics.width > region.x + region.width)
            tx = region.x + region.width - metrics.width;

        // Debug: Outline of text
        // gc.drawRect(tx, region.y + TICK_LENGTH, metrics.width, metrics.height);
        gc.drawString(mark, tx, region.y + metrics.y + TICK_LENGTH);
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Double tick, final boolean floating)
    {
        final Rectangle region = getBounds();
        final int x = getScreenCoord(tick);
        final String mark = floating ? ticks.formatDetailed(tick) : ticks.format(tick);
        gc.setFont(scale_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, mark);
        int tx = x - metrics.width/2;
        // Correct location of rightmost label to remain within region
        if (tx + metrics.width > region.x + region.width)
            tx = region.x + region.width - metrics.width;

        if (floating)
        {
            gc.drawLine(x, region.y, x, region.y + TICK_LENGTH);
            final Color orig_fill = gc.getColor();
            gc.setColor(java.awt.Color.WHITE);
            gc.fillRect(tx-BORDER, region.y + TICK_LENGTH-BORDER, metrics.width+2*BORDER, metrics.height+2*BORDER);
            gc.setColor(orig_fill);
            gc.drawRect(tx-BORDER, region.y + TICK_LENGTH-BORDER, metrics.width+2*BORDER, metrics.height+2*BORDER);
        }

        // Debug: Outline of text
        // gc.drawRect(tx, region.y + TICK_LENGTH, metrics.width, metrics.height);
        gc.drawString(mark, tx, region.y + metrics.y + TICK_LENGTH);
    }
}
