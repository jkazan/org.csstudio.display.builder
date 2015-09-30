/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.BaseWidget;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty.Descriptor;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.epics.vtype.VType;
import org.osgi.framework.Version;
import org.w3c.dom.Element;

/** Widget that displays X/Y waveforms
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotWidget extends BaseWidget
{
    // TODO Need 'x_axis' as well as 'y_axis' (later more than one)
    // X: <axis_0_auto_scale> <axis_0_minimum> <axis_0_maximum> <axis_0_axis_title>
    // Y: <axis_1_auto_scale> <axis_1_minimum> <axis_1_maximum> <axis_1_axis_title>

    // Elements of the 'axis' structure
    private static final WidgetPropertyDescriptor<String> title =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "title", "Title");

    private static final WidgetPropertyDescriptor<Boolean> autoscale =
        CommonWidgetProperties.newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "autoscale", "Auto-scale");

    private final static StructuredWidgetProperty.Descriptor behaviorAxis =
            new Descriptor(WidgetPropertyCategory.BEHAVIOR, "axis", "Axis");

    /** 'axis' structure */
    public static class AxisWidgetProperty extends StructuredWidgetProperty
    {
        public AxisWidgetProperty(final Widget widget)
        {
            super(behaviorAxis, widget,
                  Arrays.asList(title.createProperty(widget, "x"),
                                autoscale.createProperty(widget, false),
                                CommonWidgetProperties.behaviorMinimum.createProperty(widget, 0.0),
                                CommonWidgetProperties.behaviorMaximum.createProperty(widget, 100.0)));
        }
        public WidgetProperty<String> title()        { return getElement(0); }
        public WidgetProperty<Boolean> autoscale()   { return getElement(1); }
        public WidgetProperty<Double> minimum()      { return getElement(2); }
        public WidgetProperty<Double> maximum()      { return getElement(3); }
    };

    // Elements of the 'trace' structure
    private static final WidgetPropertyDescriptor<String> traceX =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "x_pv", "X PV");
    private static final WidgetPropertyDescriptor<String> traceY =
        CommonWidgetProperties.newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "y_pv", "Y PV");
    private static final WidgetPropertyDescriptor<WidgetColor> traceColor =
        CommonWidgetProperties.newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "color", "Color");
    private static final WidgetPropertyDescriptor<VType> traceXValue = CommonWidgetProperties.newRuntimeValue("x_value", "X Value");
    private static final WidgetPropertyDescriptor<VType> traceYValue = CommonWidgetProperties.newRuntimeValue("y_value", "Y Value");

    private final static StructuredWidgetProperty.Descriptor behaviorTrace =
            new Descriptor(WidgetPropertyCategory.BEHAVIOR, "trace", "Trace");

    /** 'trace' structure */
    public static class TraceWidgetProperty extends StructuredWidgetProperty
    {
        public TraceWidgetProperty(final Widget widget)
        {
            super(behaviorTrace, widget,
                  Arrays.asList(traceX.createProperty(widget, ""),
                                traceY.createProperty(widget, ""),
                                traceColor.createProperty(widget, new WidgetColor(0, 0, 255)),
                                traceXValue.createProperty(widget, null),
                                traceYValue.createProperty(widget, null)  ));
        }
        public WidgetProperty<String> traceX()          { return getElement(0); }
        public WidgetProperty<String> traceY()          { return getElement(1); }
        public WidgetProperty<WidgetColor> traceColor() { return getElement(2); }
        public WidgetProperty<VType> xValue()           { return getElement(3); }
        public WidgetProperty<VType> yValue()           { return getElement(4); }
    };



    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR
        = new WidgetDescriptor("xyplot", WidgetCategory.MONITOR,
                Messages.XYPlotWidget_Name,
                "platform:/plugin/org.csstudio.display.builder.model/icons/xyplot.png",
                Messages.XYPlotWidget_Description,
                Arrays.asList("org.csstudio.opibuilder.widgets.xyGraph"))
        {
            @Override
            public Widget createWidget()
            {
                return new XYPlotWidget();
            }
        };

    /** Configurator that handles legacy properties */
    private static class Configurator extends WidgetConfigurator
    {
        public Configurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public void configureFromXML(final Widget widget, final Element xml)
                throws Exception
        {
            final XYPlotWidget plot = (XYPlotWidget) widget;
            configureAllPropertiesFromMatchingXML(widget, xml);

            // Legacy widget had a "pv_name" property that was basically used as a macro
            final String pv_macro = XMLUtil.getChildString(xml, "pv_name").orElse("");
            XMLUtil.getChildString(xml, "axis_0_axis_title").ifPresent(title ->
            {
                final WidgetProperty<String> property = plot.x_axis.title();
                ((StringWidgetProperty)property).setSpecification(title.replace("$(pv_name)", pv_macro));
            });

            XMLUtil.getChildString(xml, "axis_0_minimum").ifPresent(txt ->
                plot.x_axis.minimum().setValue(Double.parseDouble(txt))
            );
            XMLUtil.getChildString(xml, "axis_0_maximum").ifPresent(txt ->
                plot.x_axis.maximum().setValue(Double.parseDouble(txt))
            );

            XMLUtil.getChildString(xml, "trace_0_x_pv").ifPresent(pv ->
            {
                final WidgetProperty<String> property = plot.trace.traceX();
                ((StringWidgetProperty)property).setSpecification(pv.replace("$(pv_name)", pv_macro));
            });
            XMLUtil.getChildString(xml, "trace_0_y_pv").ifPresent(pv ->
            {
                final WidgetProperty<String> property = plot.trace.traceY();
                ((StringWidgetProperty)property).setSpecification(pv.replace("$(pv_name)", pv_macro));
            });

            Element element = XMLUtil.getChildElement(xml, "trace_0_trace_color");
            if (element != null)
                plot.trace.traceColor().readFromXML(element);
        }
    };

    // TODO: Support minimum of legacy properties:
    // <axis_0_axis_title>
    // <trace_0_x_pv>
    // <trace_0_y_pv>
    // <trace_0_trace_color>

    // TODO: ArrayWidgetProperty of StructureWidgetProperty

    private AxisWidgetProperty x_axis;
    private TraceWidgetProperty trace;

    public XYPlotWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new Configurator(persisted_version);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(x_axis = new AxisWidgetProperty(this));
        properties.add(trace = new TraceWidgetProperty(this));
    }

    /** @return Behavior 'x_axis' */
    public AxisWidgetProperty behaviorXAxis()
    {
        return x_axis;
    }

    /** @return Behavior 'trace' */
    public TraceWidgetProperty behaviorTrace()
    {
        return trace;
    }
}