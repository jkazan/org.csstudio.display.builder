/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.diirt.vtype.VType;
import org.eclipse.osgi.util.NLS;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Dialog for displaying widget information
 *  @author Kay Kasemir
 */
public class WidgetInfoDialog extends Dialog<Boolean>
{
    /** Create dialog
     *  @param widget {@link Widget}
     *  @param pvs {@link RuntimePV}s, may be empty
     */
    public WidgetInfoDialog(final Widget widget, final Collection<RuntimePV> pvs)
    {
        setTitle(Messages.WidgetInfoDialog_Title);
        setHeaderText(NLS.bind(Messages.WidgetInfoDialog_Info_Fmt, new Object[] { widget.getName(), widget.getType() }));

        if (! (widget instanceof DisplayModel))
        {   // Widgets (but not the DisplayModel!) have a descriptor for their icon
            try
            {
                final WidgetDescriptor descriptor = WidgetFactory.getInstance().getWidgetDescriptor(widget.getType());
                final InputStream icon = descriptor.getIconStream();
                setGraphic(new ImageView(new Image(icon)));
            }
            catch (Exception ex)
            {
                // No icon, no problem
            }
        }
        final TabPane tabs = new TabPane(createProperties(widget), createPVs(pvs), createMacros(widget.getEffectiveMacros()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));

        getDialogPane().setContent(tabs);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        setResizable(true);
        tabs.setMinWidth(800);

        setResultConverter(button -> true);
    }

    private Tab createMacros(Macros orig_macros)
    {
        final Macros macros = (orig_macros == null) ? new Macros() : orig_macros;
        // Use text field to allow copying the name and value
        // Table uses list of macro names as input
        // Name column just displays the macro name,..
        final TableColumn<String, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()));

        // .. value column fetches the macro value
        final TableColumn<String, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(TextFieldTableCell.forTableColumn());
        value.setCellValueFactory(param -> new ReadOnlyStringWrapper(macros.getValue(param.getValue())));

        final TableView<String> table =
            new TableView<>(FXCollections.observableArrayList(macros.getNames()));
        table.getColumns().add(name);
        table.getColumns().add(value);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new Tab(Messages.WidgetInfoDialog_TabMacros, table);
    }

    private Tab createPVs(final Collection<RuntimePV> pvs)
    {
        // Use text field to allow users to copy the name, value to clipboard
        final TableColumn<RuntimePV, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));

        final TableColumn<RuntimePV, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(TextFieldTableCell.forTableColumn());
        value.setCellValueFactory(param ->
        {
            final VType v = param.getValue().read();
            final String text;
            if (v == null)
                text = Messages.WidgetInfoDialog_Disconnected;
            else
                text = VTypeUtil.getValueString(v, true);
            return new ReadOnlyStringWrapper(text);
        });

        final ObservableList<RuntimePV> pv_data = FXCollections.observableArrayList(pvs);
        pv_data.sort((a, b) -> a.getName().compareTo(b.getName()));
        final TableView<RuntimePV> table = new TableView<>(pv_data);
        table.getColumns().add(name);
        table.getColumns().add(value);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new Tab(Messages.WidgetInfoDialog_TabPVs, table);
    }

    private Tab createProperties(final Widget widget)
    {
        // Use text field to allow copying the name (for use in scripts)
        // and value, but not the localized description and category
        // which are just for information
        final TableColumn<WidgetProperty<?>, String> cat = new TableColumn<>(Messages.WidgetInfoDialog_Category);
        cat.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getCategory().getDescription()));

        final TableColumn<WidgetProperty<?>, String> descr = new TableColumn<>(Messages.WidgetInfoDialog_Property);
        descr.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getDescription()));

        final TableColumn<WidgetProperty<?>, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));

        final TableColumn<WidgetProperty<?>, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(TextFieldTableCell.forTableColumn());
        value.setCellValueFactory(param -> new ReadOnlyStringWrapper(Objects.toString(param.getValue().getValue())));

        final TableView<WidgetProperty<?>> table =
            new TableView<>(FXCollections.observableArrayList(widget.getProperties()));
        table.getColumns().add(cat);
        table.getColumns().add(descr);
        table.getColumns().add(name);
        table.getColumns().add(value);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new Tab(Messages.WidgetInfoDialog_TabProperties, table);
   }
}
