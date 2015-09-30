/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Widget property that contains array of widget properties.
 *
 *  <p>Individual elements are properties which can be modified as usual,
 *  unless they are themselves read-only.
 *  The overall list as return by <code>getValue</code> is read-only,
 *  because direct modification of the underlying list would not allow
 *  this property to send change events.
 *  To add/remove elements, this property has dedicated <code>addElement</code>,
 *  <code>removeElement</code> API.
 *
 *  @author Kay Kasemir
 *  @param <E> Type of each item's property
 */
@SuppressWarnings("nls")
public class ArrayWidgetProperty<E> extends WidgetProperty<List<WidgetProperty<E>>>
{
    /** Factory that creates a new array element.
     *
     *  <p>When reading persisted data or when adding a new array element
     *  with default value, this factory is used to create that element.
     *
     *  <p>Implementation may always create an element with the same value,
     *  or based that value on the array index.
     */
    @FunctionalInterface
    public static interface ElementFactory<E>
    {
        /** Create a new array element
         *  @param widget Widget that contains the property
         *  @param index Index of the new array element
         *  @return Array element
         */
        WidgetProperty<E> newElement(Widget widget, int index);
    }

    /** Descriptor of an array property */
    public static class Descriptor<E> extends WidgetPropertyDescriptor<List<WidgetProperty<E>>>
    {
        final private ElementFactory<E> factory;

        public Descriptor(final WidgetPropertyCategory category,
                          final String name, final String description,
                          final ElementFactory<E> factory)
        {
            super(category, name, description);
            this.factory = factory;
        }

        @Override
        public ArrayWidgetProperty<E> createProperty(
                final Widget widget, final List<WidgetProperty<E>> elements)
        {
            return new ArrayWidgetProperty<E>(this, widget, elements);
        }
    };

    protected ArrayWidgetProperty(final Descriptor<E> descriptor,
            final Widget widget, final List<WidgetProperty<E>> elements)
    {
        // Default's elements can be changed, they each track their own 'default',
        // but overall default_value List<> is not modifiable
        super(descriptor, widget, Collections.unmodifiableList(elements));
        value = new ArrayList<>(elements);
    }

    @Override
    public boolean isDefaultValue()
    {
        for (WidgetProperty<E> element : value)
            if (! element.isDefaultValue())
                return false;
        return true;
    }

    /** @return Current List<>, not modifiable.
     *  @see #addElement(WidgetProperty)
     *  @see #removeElement()
     */
    @Override
    public List<WidgetProperty<E>> getValue()
    {
        return Collections.unmodifiableList(value);
    }

    /** @return Element count */
    public int size()
    {
        return value.size();
    }

    /** Access element
     *  @param index Element index, 0 .. (<code>getValue().size()</code>-1)
     *  @return Element of array
     */
    public  WidgetProperty<E> getElement(final int index)
    {
        return value.get(index);
    }

    /** Remove the last element
     *  @return Removed element
     *  @throws IndexOutOfBoundsException if list is empty
     */
    public WidgetProperty<E> removeElement()
    {
        final WidgetProperty<E> removed = value.remove(value.size()-1);
        firePropertyChange(Arrays.asList(removed), null);
        return removed;
    }

    /** @param element Element to add to end of list */
    public void addElement(final WidgetProperty<E> element)
    {
        value.add(element);
        firePropertyChange(null, Arrays.asList(element));
    }

    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeToXML(final XMLStreamWriter writer) throws Exception
    {
        for (WidgetProperty<E> element : value)
        {
            writer.writeStartElement(element.getName());
            element.writeToXML(writer);
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final Element property_xml) throws Exception
    {
        // Loop over XML child elements.
        // The element names are unknown at this time, only once we create an element
        // could we get its name...
        final List<WidgetProperty<E>> elements = new ArrayList<>();
        Node child = property_xml.getFirstChild();
        while (child != null)
        {
            if (child.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element child_xml = (Element) child;
                final WidgetProperty<E> element =
                    ((Descriptor<E>)descriptor).factory.newElement(widget, elements.size());
                try
                {
                    element.readFromXML(child_xml);
                }
                catch (Exception ex)
                {
                    Logger.getLogger(getClass().getName())
                          .log(Level.WARNING, "Error reading " + getName() + " element " + element.getName(), ex);
                }
                elements.add(element);
            }
            child = child.getNextSibling();
        }
        value = elements;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder("'" + getName() + "' = [ ");
        boolean first = true;
        for (WidgetProperty<E> element : value)
        {
            if (first)
                first = false;
            else
                buf.append(", ");
            buf.append(element);
        }
        buf.append(" ]");
        return buf.toString();
    }
}