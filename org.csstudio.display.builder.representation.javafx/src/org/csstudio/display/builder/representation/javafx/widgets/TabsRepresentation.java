/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import com.sun.javafx.tk.Toolkit;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TabsRepresentation extends JFXBaseRepresentation<TabPane, TabsWidget>
{
    private final DirtyFlag dirty_layout = new DirtyFlag();

    private final AtomicBoolean changing_active_tab = new AtomicBoolean();

    private volatile Font tab_font;

    private final WidgetPropertyListener<String> tab_title_listener = (property, old_value, new_value) ->
    {
        final List<TabItemProperty> model_tabs = model_widget.displayTabs().getValue();
        for (int i=0; i<model_tabs.size(); ++i)
            if (model_tabs.get(i).name() == property)
            {
                final Tab tab = jfx_node.getTabs().get(i);
                final Label label = (Label) tab.getGraphic();
                label.setText(property.getValue());
                break;
            }
    };

    private final WidgetPropertyListener<List<Widget>> tab_children_listener = (property, removed, added) ->
    {
        final List<TabItemProperty> model_tabs = model_widget.displayTabs().getValue();
        int index;
        for (index = model_tabs.size() - 1;  index >= 0; --index)
            if (model_tabs.get(index).children() == property)
                break;
        if (index < 0)
            throw new IllegalStateException("Cannot locate tab children " + property + " in " + model_widget);

        if (removed != null)
            for (Widget removed_widget : removed)
            {
                toolkit.execute(() -> toolkit.disposeWidget(removed_widget));
            }

        if (added != null)
            addChildren(index, added);
    };

    @Override
    public TabPane createJFXNode() throws Exception
    {
        final TabPane tabs = new TabPane();

        // See 'twiddle' below
        tabs.setStyle("-fx-background-color: lightgray;");
        tabs.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

        tabs.setMinSize(TabPane.USE_PREF_SIZE, TabPane.USE_PREF_SIZE);
        tabs.setTabMinHeight(model_widget.displayTabHeight().getValue());

        return tabs;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        // Create initial tabs and their children
        addTabs(model_widget.displayTabs().getValue());

        final UntypedWidgetPropertyListener listener = this::layoutChanged;
        model_widget.positionWidth().addUntypedPropertyListener(listener);
        model_widget.positionHeight().addUntypedPropertyListener(listener);
        model_widget.displayBackgroundColor().addUntypedPropertyListener(listener);
        model_widget.displayFont().addUntypedPropertyListener(listener);
        model_widget.displayTabs().addPropertyListener(this::tabsChanged);
        model_widget.displayDirection().addUntypedPropertyListener(listener);
        model_widget.displayTabHeight().addUntypedPropertyListener(listener);

        // Update UI when model selects a tab
        final WidgetPropertyListener<Integer> track_active_model_tab = (p, old, value) ->
        {
            if (! changing_active_tab.compareAndSet(false, true))
                return;
            if (value == null)
                value = p.getValue();
            if (value >= jfx_node.getTabs().size())
                value = jfx_node.getTabs().size() - 1;
            jfx_node.getSelectionModel().select(value);
            changing_active_tab.set(false);
        };
        // Select initial tab
        track_active_model_tab.propertyChanged(model_widget.displayActiveTab(), null, null);
        model_widget.displayActiveTab().addPropertyListener(track_active_model_tab);

        // Update model when UI selects a tab
        jfx_node.getSelectionModel().selectedIndexProperty().addListener((t, o, selected) ->
        {
            if (! changing_active_tab.compareAndSet(false, true))
                return;
            model_widget.displayActiveTab().setValue(selected.intValue());
            changing_active_tab.set(false);
        });

        // Initial update of font, size
        layoutChanged(null, null, null);
    }

    private void tabsChanged(final WidgetProperty<List<TabItemProperty>> property,
                             final List<TabItemProperty> removed,
                             final List<TabItemProperty> added)
    {
        Toolkit.getToolkit().checkFxUserThread();
        if (removed != null)
            removeTabs(removed);
        if (added != null)
            addTabs(added);
    }

    private void addTabs(final List<TabItemProperty> added)
    {
        for (TabItemProperty item : added)
        {
            final String name = item.name().getValue();
            final Pane content = new Pane();

            // 'Tab's are added with a Label as 'graphic'
            // because that label allows setting the font.

            // Quirk: Tabs will not show the label unless there's also a non-empty text
            final Tab tab = new Tab(" ", content);
            final Label label = new Label(name);
            tab.setGraphic(label);
            tab.setClosable(false); // !!
            tab.setUserData(item);

            final int index = jfx_node.getTabs().size();
            jfx_node.getTabs().add(tab);

            addChildren(index, item.children().getValue());

            item.name().addPropertyListener(tab_title_listener);
            item.children().addPropertyListener(tab_children_listener);
        }
    }

    private void removeTabs(final List<TabItemProperty> removed)
    {
        for (TabItemProperty item : removed)
        {
            item.children().removePropertyListener(tab_children_listener);
            item.name().removePropertyListener(tab_title_listener);
            for (Tab tab : jfx_node.getTabs())
                if (tab.getUserData() == item)
                {
                    jfx_node.getTabs().remove(tab);
                    break;
                }
        }
    }

    private void addChildren(final int index, final List<Widget> added)
    {
        final Pane parent_item = (Pane) jfx_node.getTabs().get(index).getContent();
        for (Widget added_widget : added)
        {
            final Optional<Widget> parent = added_widget.getParent();
            if (! parent.isPresent())
                throw new IllegalStateException("Cannot locate parent widget for " + added_widget);
            toolkit.execute(() -> toolkit.representWidget(parent_item, added_widget));
        }
    }

    private void layoutChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        tab_font = JFXUtil.convert(model_widget.displayFont().getValue());
        dirty_layout.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Compute 'insets'
     *
     *  <p>Determines where the tabs' content Pane
     *  is located relative to the bounds of the TabPane
     */
    private void computeInsets()
    {   // There is always at least one tab. All tabs have the same size.
        final Pane pane = (Pane)jfx_node.getTabs().get(0).getContent();
        final Point2D tabs_bounds = jfx_node.localToScene(0.0, 0.0);
        final Point2D pane_bounds = pane.localToScene(0.0, 0.0);
        final int[] insets = new int[] { (int)(pane_bounds.getX() - tabs_bounds.getX()),
                                         (int)(pane_bounds.getY() - tabs_bounds.getY()) };
        logger.log(Level.INFO, "Insets: " + Arrays.toString(insets));
        if (insets[0] < 0  ||  insets[1] < 0)
        {
            logger.log(Level.WARNING, "Inset computation failed: TabPane at " + tabs_bounds + ", content pane at " + pane_bounds);
            insets[0] = insets[1] = 0;
        }
        model_widget.runtimeInsets().setValue(insets);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_layout.checkAndClear())
        {
            // How to best set colors?
            // Content Pane can be set in API, but Tab has no usable 'set color' API.
            // TabPane has setBackground(), but in "floating" style that would be
            // the background behind the tabs, which is usually transparent.
            // modena.css of JDK8 reveals a structure of sub-items which are shaded with gradients based
            // on  -fx-color for the inactive tabs,
            //     -fx-outer-border and -fx-inner-border for the, well, border,
            // and -fx-background for the selected tab,
            // so re-define those.
            final String bg = JFXUtil.webRGB(model_widget.displayBackgroundColor().getValue());
            final String style = "-fx-color: derive(" + bg + ", 50%);" +
                                 "-fx-outer-border: derive(" + bg + ", -23%);" +
                                 "-fx-inner-border: linear-gradient(to bottom," +
                                 "ladder(" + bg + "," +
                                 "       derive(" + bg + ",30%) 0%," +
                                 "       derive(" + bg + ",20%) 40%," +
                                 "       derive(" + bg + ",25%) 60%," +
                                 "       derive(" + bg + ",55%) 80%," +
                                 "       derive(" + bg + ",55%) 90%," +
                                 "       derive(" + bg + ",75%) 100%" +
                                 ")," +
                                 "ladder(" + bg + "," +
                                 "       derive(" + bg + ",20%) 0%," +
                                 "       derive(" + bg + ",10%) 20%," +
                                 "       derive(" + bg + ",5%) 40%," +
                                 "       derive(" + bg + ",-2%) 60%," +
                                 "       derive(" + bg + ",-5%) 100%" +
                                 "));" +
                                 "-fx-background: " + bg + ";";

            final Background background = new Background(new BackgroundFill(JFXUtil.convert(model_widget.displayBackgroundColor().getValue()), null, null));

            for (Tab tab : jfx_node.getTabs())
            {   // Set the font of the 'graphic' that's used to represent the tab
                final Label label = (Label) tab.getGraphic();
                label.setFont(tab_font);

                // Set colors
                tab.setStyle(style);

                final Pane content  = (Pane) tab.getContent();
                content.setBackground(background);
            }

            final Integer width = model_widget.positionWidth().getValue();
            final Integer height = model_widget.positionHeight().getValue();
            jfx_node.setPrefSize(width, height);
            jfx_node.setTabMinHeight(model_widget.displayTabHeight().getValue());

            refreshHack();
        }
    }

    /** Force TabPane refresh */
    private void refreshHack()
    {
        // Imperfect; works most of the time.
        // See org.csstudio.display.builder.representation.javafx.sandbox.TabDemo
        // for the setSide hack.
        // In addition, if TabPane is the _only_ widget, it will not show
        // unless the background style is initially set to something.
        // OK to then clear the style later, i.e. in here.
        final Callable<Object> twiddle = () ->
        {
            Thread.sleep(500);
            Platform.runLater(() ->
            {
                jfx_node.setStyle("");
                jfx_node.setSide(Side.BOTTOM);
                if (model_widget.displayDirection().getValue() == Direction.HORIZONTAL)
                    jfx_node.setSide(Side.TOP);
                else
                    jfx_node.setSide(Side.LEFT);
            });
            Thread.sleep(500);
            Platform.runLater(() ->
            {   // Insets computation only possible once TabPane is properly displayed.
                // Until then, content Pane will report position as 0,0.
                computeInsets();
            });
            return null;
        };
        ModelThreadPool.getExecutor().submit(twiddle);
    }
}