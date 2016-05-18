/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.macros.MacroValueProvider;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionButtonRepresentation extends RegionBaseRepresentation<Pane, ActionButtonWidget>
{
    // Uses a Button if there is only one action,
    // otherwise a MenuButton so that user can select the specific action.
    //
    // These two types were chosen because they share the same ButtonBase base class.
    // ChoiceBox is not derived from ButtonBase, plus it has currently selected 'value',
    // and with action buttons it wouldn't make sense to select one of the actions.
    //
    // Current implementation does not allow changing the actions at runtime.
    // Specifically, if the action count changed between 1 and >1,
    // it won't update between Button and MenuButton.

    private final DirtyFlag dirty_representation = new DirtyFlag();
    private final DirtyFlag dirty_actionls = new DirtyFlag();

    private volatile ButtonBase base;
    private volatile String background, text_fill, fx_base;
    private volatile Color foreground;
    private volatile String button_text;

    /** Optional modifier of the open display 'target */
    private Optional<OpenDisplayActionInfo.Target> target_modifier = Optional.empty();
    private Pane pane;



    @Override
    public Pane createJFXNode() throws Exception
    {
        updateColors();
        makeBaseButton();

        // Model has width/height, but JFX widget has min, pref, max size.
        // updateChanges() will set the 'pref' size, so make min use that as well.
        base.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);

        // Monitor keys that modify the OpenDisplayActionInfo.Target.
        // Use filter to capture event that's otherwise already handled.
        base.addEventFilter(MouseEvent.MOUSE_PRESSED, this::checkModifiers);

        pane = new Pane();
        pane.getChildren().add(base);

        return pane;
    }

    /** @param event Mouse event to check for target modifier keys */
    private void checkModifiers(final MouseEvent event)
    {
        if (event.isControlDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.TAB);
        else if (event.isShiftDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.WINDOW);
        else
            target_modifier = Optional.empty();

        // At least on Linux, a Control-click or Shift-click
        // will not 'arm' the button, so the click is basically ignored.
        // Force the 'arm', so user can Control-click or Shift-click to
        // invoke the button
        if (target_modifier.isPresent())
        {
            logger.log(Level.FINE, "{0} modifier: {1}", new Object[] { model_widget, target_modifier.get() });
            base.arm();
        }
    }

    /** @param action Action that the user invoked */
    private void handleAction(ActionInfo action)
    {
        logger.log(Level.FINE, "{0} pressed", model_widget);
        if (action instanceof OpenDisplayActionInfo  &&  target_modifier.isPresent())
        {
            final OpenDisplayActionInfo orig = (OpenDisplayActionInfo) action;
            action = new OpenDisplayActionInfo(orig.getDescription(), orig.getFile(), orig.getMacros(), target_modifier.get());
        }
        toolkit.fireAction(model_widget, action);
    }

    @Override
    protected void registerListeners()
    {
        updateColors();
        super.registerListeners();
        model_widget.positionWidth().addUntypedPropertyListener(this::representationChanged);
        model_widget.positionHeight().addUntypedPropertyListener(this::representationChanged);
        model_widget.displayText().addUntypedPropertyListener(this::representationChanged);
        model_widget.displayFont().addUntypedPropertyListener(this::representationChanged);
        model_widget.displayBackgroundColor().addUntypedPropertyListener(this::buttonChanged);
        model_widget.displayForegroundColor().addUntypedPropertyListener(this::buttonChanged);
        model_widget.behaviorActions().addUntypedPropertyListener(this::buttonChanged);
    }

    private void representationChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        updateColors();
        dirty_representation.mark();
        toolkit.scheduleUpdate(this);
    }

    private void buttonChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        updateColors();
        dirty_actionls.mark();
        dirty_representation.mark();
        toolkit.scheduleUpdate(this);
    }

    private void updateColors()
    {
        text_fill = "-fx-text-fill: " + JFXUtil.webRGB(model_widget.displayForegroundColor().getValue()) + ";";

        foreground = JFXUtil.convert(model_widget.displayForegroundColor().getValue());

        final String bg = JFXUtil.webRGB(model_widget.displayBackgroundColor().getValue());

        fx_base = "-fx-base: " + bg + ";";

        background = "-fx-color: derive(" + bg + ", 50%);" +
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
    }

    private String makeActionText(ActionInfo action)
    {
        String action_str = action.getDescription();
        String expanded;
        if (action_str.length() == 0)
        {
            action_str = action.toString();
        }
        final MacroValueProvider macros = model_widget.getMacrosOrProperties();

        try
        {
            expanded = MacroHandler.replace(macros, action_str);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, model_widget + " action " + action + " cannot expand macros for " + action_str, ex);
            expanded = action_str;
        }
        return expanded;
    }

    private void makeButtonText()
    {
        button_text = model_widget.displayText().getValue();
        if (button_text.length() > 0)
            return;

        final List<ActionInfo> actions = model_widget.behaviorActions().getValue();
        if (actions.size() < 1)
        {
            button_text = "EMPTY";
            return;
        }
        if (actions.size() > 1)
        {
            button_text = "Choose 1 of " + String.valueOf(actions.size());
            return;
        }
        button_text = makeActionText(actions.get(0));
    }

    private void makeBaseButton()
    {
        final List<ActionInfo> actions = model_widget.behaviorActions().getValue();
        // Create either single-action button or menu for selecting one out of N actions
        if (actions.size() < 2)
        {
            final Button button = new Button();
            if (actions.size() > 0)
            {
                final ActionInfo the_action = actions.get(0);
                button.setOnAction(event -> handleAction(the_action));
            }
            base = button;
        }
        else
        {
            final MenuButton button = new MenuButton();
            for (final ActionInfo action : actions)
            {
                final MenuItem item = new MenuItem(makeActionText(action));
                //item.setStyle("-fx-background-color: slateblue; -fx-text-fill: white;");
                item.setStyle(background + " " + text_fill);
                item.setOnAction(event -> handleAction(action));
                button.getItems().add(item);
            }
            base = button;
        }
        //button1.setStyle("-fx-font: 22 arial; -fx-base: #b6e7c9;");
        base.setStyle(background + " " + fx_base);

        if (toolkit.isEditMode())
        {
            //base.setOnMouseReleased(event -> {});
            base.setOnMousePressed((event) ->
            {
                event.consume();
                toolkit.fireClick(model_widget, event.isControlDown());
            });
        }


    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_actionls.checkAndClear())
        {
            makeBaseButton();
            jfx_node.getChildren().clear();
            jfx_node.getChildren().add(base);
        }
        if (dirty_representation.checkAndClear())
        {
            makeButtonText();
            base.setText(button_text);
            base.setTextFill(foreground);
            base.setPrefSize(model_widget.positionWidth().getValue(),
                    model_widget.positionHeight().getValue());
            base.setFont(JFXUtil.convert(model_widget.displayFont().getValue()));
            jfx_node.getChildren().clear();
            jfx_node.getChildren().add(base);
        }
    }
}
