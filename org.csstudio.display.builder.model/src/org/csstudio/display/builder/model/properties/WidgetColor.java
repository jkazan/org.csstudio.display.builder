/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

/** Description of a color
 *  @author Kay Kasemir
 */
// Implementation avoids AWT, SWT, JavaFX color
@SuppressWarnings("nls")
public class WidgetColor
{
    protected final int red, green, blue;

    /** Construct RGB color
     *  @param red Red component, range {@code 0-255}
     *  @param green Green component, range {@code 0-255}
     *  @param blue Blue component, range {@code 0-255}
     */
    public WidgetColor(final int red, final int green, final int blue)
    {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /** @return Red component, range {@code 0-255} */
    public int getRed()
    {
        return red;
    }

    /** @return Green component, range {@code 0-255} */
    public int getGreen()
    {
        return green;
    }

    /** @return Blue component, range {@code 0-255} */
    public int getBlue()
    {
        return blue;
    }

        @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = blue;
        result = prime * result + green;
        result = prime * result + red;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (! (obj instanceof WidgetColor))
            return false;
        final WidgetColor other = (WidgetColor) obj;
        return blue == other.blue   &&
               green == other.green &&
               red == other.red;
    }

    @Override
    public String toString()
    {
        return "RGB(" + red + "," + green + "," + blue + ")";
    }
}
