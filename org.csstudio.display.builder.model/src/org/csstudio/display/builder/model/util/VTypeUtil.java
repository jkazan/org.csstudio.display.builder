/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.util;

import java.text.NumberFormat;
import java.util.List;

import org.diirt.util.array.ListInt;
import org.diirt.util.array.ListNumber;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VType;

/** Utility for displaying VType data.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeUtil
{
    /** Format a value as text.
     *
     *  TODO Add equivalent of org.csstudio.simplepv.FormatEnum
     *
     *  @param value VType
     *  @param with_units Add units?
     *  @return Text for value (without timestamp, alarm, ..)
     */
    public static String getValueString(final VType value, final boolean with_units)
    {
        if (value instanceof VNumber)
        {
            final VNumber cast = (VNumber) value;
            final NumberFormat format = cast.getFormat();
            final String text;
            if (format != null)
                text = format.format(cast.getValue());
            else
                text = cast.getValue().toString();
            if (with_units  &&  !cast.getUnits().isEmpty())
                return text + " " + cast.getUnits();
            return text;
        }
        if (value instanceof VString)
            return ((VString)value).getValue();
        if (value instanceof VEnum)
            return ((VEnum)value).getValue();
        // For arrays, return first element
        if (value instanceof VNumberArray)
        {
            final VNumberArray cast = (VNumberArray)value;
            final ListNumber numbers = cast.getData();
            final NumberFormat format = cast.getFormat();
            if (numbers.size() <= 0)
                return "[]";
            final String text;
            if (format != null)
                text = format.format(numbers.getDouble(0));
            else
                text = Double.toString(numbers.getDouble(0));
            if (with_units  &&  !cast.getUnits().isEmpty())
                return text + " " + cast.getUnits();
        }
        if (value instanceof VEnumArray)
        {
            final List<String> labels = ((VEnumArray)value).getLabels();
            if (labels.size() > 0)
                return labels.get(0);
            else
                return "[]";
        }
        if (value == null)
            return "<null>";
        return "<" + value.getClass().getName() + ">";
    }

    /** Obtain numeric value
     *  @param value VType
     *  @return Number for value.
     *          <code>Double.NaN</code> in case the value type
     *          does not decode into a number.
     */
    public static Number getValueNumber(final VType value)
    {
        if (value instanceof VNumber)
        {
            final VNumber cast = (VNumber) value;
            return cast.getValue();
        }
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        // For arrays, return first element
        if (value instanceof VNumberArray)
        {
            final ListNumber array = ((VNumberArray)value).getData();
            if (array.size() > 0)
                return array.getDouble(0);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt array = ((VEnumArray)value).getIndexes();
            if (array.size() > 0)
                return array.getInt(0);
        }
        return Double.valueOf(Double.NaN);
    }
}
