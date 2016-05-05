/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets.plots;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.XYPlotWidget;
import org.csstudio.display.builder.model.widgets.XYPlotWidget.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.XYPlotWidget.TraceWidgetProperty;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.RTValuePlot;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VType;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotRepresentation extends RegionBaseRepresentation<Pane, XYPlotWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_config = new DirtyFlag();

    private final UntypedWidgetPropertyListener config_listener = (WidgetProperty<?> property, Object old_value, Object new_value) ->
    {
        dirty_config.mark();
        toolkit.scheduleUpdate(this);
    };

    /** Plot */
    private RTValuePlot plot;

    /** Handler for one trace of the plot
     *
     *  <p>Updates the plot when the configuration of a trace
     *  or the associated X or Y value in the model changes.
     */
    private class TraceHandler
    {
        private final TraceWidgetProperty model_trace;
        private final XYVTypeDataProvider data = new XYVTypeDataProvider();
        private final UntypedWidgetPropertyListener trace_listener = this::traceChanged, value_listener = this::valueChanged;
        private final Trace<Double> trace;

        TraceHandler(final TraceWidgetProperty model_trace)
        {
            this.model_trace = model_trace;

            trace = plot.addTrace(getDisplayName(), "", data,
                                  JFXUtil.convert(model_trace.traceColor().getValue()),
                                  TraceType.SINGLE_LINE_DIRECT, 1, PointType.NONE, 5,
                                  model_trace.traceYAxis().getValue());

            model_trace.traceName().addUntypedPropertyListener(trace_listener);
            model_trace.traceYPV().addUntypedPropertyListener(trace_listener);
            model_trace.traceColor().addUntypedPropertyListener(trace_listener);
            model_trace.traceYAxis().addUntypedPropertyListener(trace_listener);
            model_trace.traceXValue().addUntypedPropertyListener(value_listener);
            model_trace.traceYValue().addUntypedPropertyListener(value_listener);
        }

        private String getDisplayName()
        {
            String name = model_trace.traceName().getValue();
            if (name.isEmpty())
                return model_trace.traceYPV().getValue();
            return name;
        }

        private void traceChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            trace.setName(getDisplayName());
            trace.setColor(JFXUtil.convert(model_trace.traceColor().getValue()));
            final int desired = model_trace.traceYAxis().getValue();
            if (desired != trace.getYAxis())
                plot.moveTrace(trace, desired);
            plot.requestLayout();
        };

        private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
        {
            try
            {
                final VType x_value = model_trace.traceXValue().getValue();
                final VType y_value = model_trace.traceYValue().getValue();
                if (! (x_value instanceof VNumberArray  &&  y_value instanceof VNumberArray))
                    return;

                trace.setUnits(((VNumberArray)y_value).getUnits());
                data.setData( ((VNumberArray)x_value).getData(), ((VNumberArray)y_value).getData());
                plot.requestUpdate();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "XYGraph data error", ex);
            }
        }

        void dispose()
        {
            model_trace.traceName().removePropertyListener(trace_listener);
            model_trace.traceYPV().removePropertyListener(trace_listener);
            model_trace.traceYAxis().removePropertyListener(trace_listener);
            model_trace.traceColor().removePropertyListener(trace_listener);
            model_trace.traceXValue().removePropertyListener(value_listener);
            model_trace.traceYValue().removePropertyListener(value_listener);
            plot.removeTrace(trace);
        }
    };

    private final List<TraceHandler> trace_handlers = new CopyOnWriteArrayList<>();


    @Override
    public Pane createJFXNode() throws Exception
    {
        // Plot is only active in runtime mode, not edit mode
        plot = new RTValuePlot(! toolkit.isEditMode());
        plot.showToolbar(false);
        plot.showCrosshair(false);
        return plot;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        model_widget.displayTitle().addUntypedPropertyListener(config_listener);
        model_widget.displayLegend().addUntypedPropertyListener(config_listener);

        trackAxisChanges(model_widget.behaviorXAxis());

        // Track initial Y axis
        final List<AxisWidgetProperty> y_axes = model_widget.behaviorYAxes().getValue();
        trackAxisChanges(y_axes.get(0));
        // Create additional Y axes from model
        if (y_axes.size() > 1)
            yAxesChanged(model_widget.behaviorYAxes(), null, y_axes.subList(1, y_axes.size()));
        // Track added/remove Y axes
        model_widget.behaviorYAxes().addPropertyListener(this::yAxesChanged);

        final UntypedWidgetPropertyListener position_listener = this::positionChanged;
        model_widget.positionWidth().addUntypedPropertyListener(position_listener);
        model_widget.positionHeight().addUntypedPropertyListener(position_listener);

        tracesChanged(model_widget.behaviorTraces(), null, model_widget.behaviorTraces().getValue());
        model_widget.behaviorTraces().addPropertyListener(this::tracesChanged);
    }

    /** Listen to changed axis properties
     *  @param axis X or Y axis
     */
    private void trackAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().addUntypedPropertyListener(config_listener);
        axis.minimum().addUntypedPropertyListener(config_listener);
        axis.maximum().addUntypedPropertyListener(config_listener);
        axis.autoscale().addUntypedPropertyListener(config_listener);
        axis.titleFont().addUntypedPropertyListener(config_listener);
        axis.scaleFont().addUntypedPropertyListener(config_listener);
    }

    /** Ignore changed axis properties
     *  @param axis X or Y axis
     */
    private void ignoreAxisChanges(final AxisWidgetProperty axis)
    {
        axis.title().removePropertyListener(config_listener);
        axis.minimum().removePropertyListener(config_listener);
        axis.maximum().removePropertyListener(config_listener);
        axis.autoscale().removePropertyListener(config_listener);
        axis.titleFont().removePropertyListener(config_listener);
        axis.scaleFont().removePropertyListener(config_listener);
    }

    private void yAxesChanged(final WidgetProperty<List<AxisWidgetProperty>> property,
                              final List<AxisWidgetProperty> removed, final List<AxisWidgetProperty> added)
    {
        // Remove axis
        if (removed != null)
        {   // Notification holds the one removed axis, which was the last one
            final AxisWidgetProperty axis = removed.get(0);
            final int index = plot.getYAxes().size()-1;
            ignoreAxisChanges(axis);
            plot.removeYAxis(index);
        }

        // Add missing axes
        // Notification will hold the one added axis,
        // but initial call from registerListeners() will hold all axes to add
        if (added != null)
            for (AxisWidgetProperty axis : added)
            {
                plot.addYAxis(axis.title().getValue());
                trackAxisChanges(axis);
            }
        // Update axis detail: range, ..
        config_listener.propertyChanged(property, removed, added);
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    private void tracesChanged(final WidgetProperty<List<TraceWidgetProperty>> property,
                               final List<TraceWidgetProperty> removed, final List<TraceWidgetProperty> added)
    {
        final List<TraceWidgetProperty> model_traces = property.getValue();
        int count = trace_handlers.size();
        // Remove extra traces
        while (count > model_traces.size())
            trace_handlers.remove(--count).dispose();
        // Add missing traces
        while (count < model_traces.size())
            trace_handlers.add(new TraceHandler(model_traces.get(count++)));
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_config.checkAndClear())
            updateConfig();
        if (dirty_position.checkAndClear())
        {
            final int w = model_widget.positionWidth().getValue();
            final int h = model_widget.positionHeight().getValue();
            plot.setPrefWidth(w);
            plot.setPrefHeight(h);
        }
        plot.requestUpdate();
    }

    private void updateConfig()
    {
        plot.setTitle(model_widget.displayTitle().getValue());

        plot.showLegend(model_widget.displayLegend().getValue());

        // Update X Axis
        updateAxisConfig(plot.getXAxis(), model_widget.behaviorXAxis());
        // Use X axis font for legend
        plot.setLegendFont(JFXUtil.convert(model_widget.behaviorXAxis().titleFont().getValue()));

        // Update Y Axes
        final List<AxisWidgetProperty> model_y = model_widget.behaviorYAxes().getValue();
        if (plot.getYAxes().size() != model_y.size())
        {
            logger.log(Level.WARNING, "Plot has " + plot.getYAxes().size() + " while model has " + model_y.size() + " Y axes");
            return;
        }
        for (int i=0;  i<model_y.size();  ++i)
            updateAxisConfig( plot.getYAxes().get(i), model_y.get(i));
    }

    private void updateAxisConfig(final Axis<Double> plot_axis, final AxisWidgetProperty model_axis)
    {
        plot_axis.setName(model_axis.title().getValue());
        plot_axis.setValueRange(model_axis.minimum().getValue(), model_axis.maximum().getValue());
        plot_axis.setAutoscale(model_axis.autoscale().getValue());
        plot_axis.setLabelFont(JFXUtil.convert(model_axis.titleFont().getValue()));
        plot_axis.setScaleFont(JFXUtil.convert(model_axis.scaleFont().getValue()));
    }

    @Override
    public void dispose()
    {
        plot.dispose();
        super.dispose();
    }
}
