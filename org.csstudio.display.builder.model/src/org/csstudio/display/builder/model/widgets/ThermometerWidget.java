package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorMinimum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayFillColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimeValue;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.diirt.vtype.VType;

public class ThermometerWidget extends VisibleWidget
{
    /** Widget descriptor */
    @SuppressWarnings("nls")
    public static final WidgetDescriptor WIDGET_DESCRIPTOR = new WidgetDescriptor("thermometer",
            WidgetCategory.MONITOR,
            "Thermometer",
            "platform:/plugin/org.csstudio.display.builder.model/icons/Thermo.gif",
            "A thermometer",
            Arrays.asList("org.csstudio.opibuilder.widgets.thermometer"))
    {
        @Override
        public Widget createWidget()
        {
            return new ThermometerWidget();
        }
    };

    //TODO: configurator that ignores if show_bulb property is false (vertical progress bar instead)

    public ThermometerWidget()
    {
        super(WIDGET_DESCRIPTOR.getType());
    }

    private volatile WidgetProperty<String> pv_name;
    private volatile WidgetProperty<Boolean> limits_from_pv;
    private volatile WidgetProperty<Double> minimum;
    private volatile WidgetProperty<Double> maximum;
    private volatile WidgetProperty<WidgetColor> fill_color;
    private volatile WidgetProperty<VType> value;

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(fill_color = displayFillColor.createProperty(this, new WidgetColor(60, 255, 60)));
        properties.add(pv_name = behaviorPVName.createProperty(this, ""));
        properties.add(limits_from_pv = behaviorLimitsFromPV.createProperty(this, true));
        properties.add(minimum = behaviorMinimum.createProperty(this, 0.0));
        properties.add(maximum = behaviorMaximum.createProperty(this, 100.0));
        properties.add(value = runtimeValue.createProperty(this, null));
    }

    /** @return Display 'fill_color' */
    public WidgetProperty<WidgetColor> displayFillColor()
    {
        return fill_color;
    }

    /** @return Behavior 'pv_name' */
    public WidgetProperty<String> behaviorPVName()
    {
        return pv_name;
    }

    /** @return Behavior 'limits_from_pv' */
    public WidgetProperty<Boolean> behaviorLimitsFromPV()
    {
        return limits_from_pv;
    }

    /** @return Behavior 'minimum' */
    public WidgetProperty<Double> behaviorMinimum()
    {
        return minimum;
    }

    /** @return Behavior 'maximum' */
    public WidgetProperty<Double> behaviorMaximum()
    {
        return maximum;
    }

    /** @return Runtime 'value' */
    public WidgetProperty<VType> runtimeValue()
    {
        return value;
    }
}
