/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.editor.actions.ActionGUIHelper;
import org.csstudio.display.builder.editor.actions.EnableGridAction;
import org.csstudio.display.builder.editor.actions.EnableSnapAction;
import org.csstudio.display.builder.editor.actions.LoadModelAction;
import org.csstudio.display.builder.editor.actions.RedoAction;
import org.csstudio.display.builder.editor.actions.SaveModelAction;
import org.csstudio.display.builder.editor.actions.UndoAction;
import org.csstudio.display.builder.editor.properties.PropertyPanel;
import org.csstudio.display.builder.editor.tracker.SelectedWidgetUITracker;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.util.undo.UndoableActionManager;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** All the editor components for standalone test
 *
 *  <pre>
 *  Toolbar
 *  ------------------------------------------------
 *  WidgetTree | Editor (w/ palette) | PropertyPanel
 *  ------------------------------------------------
 *  Status
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditorDemoGUI
{
    private final Logger logger = Logger.getLogger(getClass().getName());

    private volatile File file = null;

    private final JFXRepresentation toolkit;

    private DisplayEditor editor;

    private WidgetTree tree;

    private PropertyPanel property_panel;

    public EditorDemoGUI(final Stage stage)
    {
        toolkit = new JFXRepresentation();
        createElements(stage);
    }

    private void createElements(final Stage stage)
    {
        editor = new DisplayEditor(toolkit);

        final ToolBar toolbar = createToolbar(editor.getSelectedWidgetUITracker(),
                                              editor.getUndoableActionManager());

        tree = new WidgetTree(editor.getWidgetSelectionHandler());

        property_panel = new PropertyPanel(editor.getWidgetSelectionHandler(),
                                           editor.getUndoableActionManager());

        final Label status = new Label("Status");

        final SplitPane center = new SplitPane();
        center.getItems().addAll(tree.create(), editor.create(), property_panel);
        center.setDividerPositions(0.2, 0.8);

        final BorderPane toolbar_center_status = new BorderPane();
        toolbar_center_status.setTop(toolbar);
        toolbar_center_status.setCenter(center);
        toolbar_center_status.setBottom(status);
        BorderPane.setAlignment(center, Pos.TOP_LEFT);

        stage.setTitle("Editor");
        stage.setWidth(1200);
        stage.setHeight(600);
        final Scene scene = new Scene(toolbar_center_status, 1200, 600);
        stage.setScene(scene);
        EditorUtil.setSceneStyle(scene);

        stage.show();
    }

    private ToolBar createToolbar(final SelectedWidgetUITracker selection_tracker, final UndoableActionManager undo)
    {
        final Button debug = new Button("Debug");
        debug.setOnAction(event -> editor.debug());

        final Button undo_button = ActionGUIHelper.createButton(new UndoAction(undo));
        final Button redo_button = ActionGUIHelper.createButton(new RedoAction(undo));
        undo_button.setDisable(true);
        redo_button.setDisable(true);
        undo.addListener((to_undo, to_redo) ->
        {
            undo_button.setDisable(to_undo == null);
            redo_button.setDisable(to_redo == null);
        });

        return new ToolBar(
                ActionGUIHelper.createButton(new LoadModelAction(this)),
                ActionGUIHelper.createButton(new SaveModelAction(this)),
                new Separator(),
                ActionGUIHelper.createToggleButton(new EnableGridAction(selection_tracker)),
                ActionGUIHelper.createToggleButton(new EnableSnapAction(selection_tracker)),
                new Separator(),
                undo_button,
                redo_button,
                new Separator(),
                debug);
    }

    /** @return Currently edited file */
    public File getFile()
    {
        return file;
    }

    /** Load model from file
     *  @param file File that contains the model
     */
    public void loadModel(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            try
            {
                doLoad(new FileInputStream(file));
                this.file = file;
            }
            catch (final Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot start", ex);
            }
        });
    }

    /** Load model from stream
     *  @param stream Stream that contains the model
     */
    public void loadModel(final InputStream stream)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            try
            {
                doLoad(stream);
                this.file = null;
            }
            catch (final Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot start", ex);
            }
        });
    }

    private void doLoad(final InputStream stream) throws Exception
    {
        final ModelReader reader = new ModelReader(stream);
        final DisplayModel model = reader.readModel();
        setModel(model);
    }

    /** Save model to file
     *  @param file File into which to save the model
     */
    public void saveModelAs(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            logger.log(Level.FINE, "Save as {0}", file);
            try
            (
                final ModelWriter writer = new ModelWriter(new FileOutputStream(file));
            )
            {
                writer.writeModel(editor.getModel());
                this.file = file;
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot save as " + file, ex);
            }
        });
    }

    private void setModel(final DisplayModel model)
    {
        // Representation needs to be created in UI thread
        toolkit.execute(() ->
        {
            editor.setModel(model);
            tree.setModel(model);
        });
    }

    public void dispose()
    {
        editor.dispose();
    }
}