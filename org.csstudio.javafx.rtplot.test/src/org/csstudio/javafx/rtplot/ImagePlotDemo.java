/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.diirt.util.array.ArrayDouble;
import org.diirt.util.array.ListDouble;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;

/** @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImagePlotDemo extends Application
{
    private static final int WIDTH = 600, HEIGHT = 400;

    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    private static final long start = System.currentTimeMillis();

    private ListDouble computeData()
    {
        final long now = System.currentTimeMillis();

        final double phase = (now - start)/1000.0;

        final double[] data = new double[WIDTH * HEIGHT];
        int i = 0;
        for (int y=0; y<HEIGHT; ++y)
        {
            final double dy = y - HEIGHT/2;
            final double dy2 = dy*dy;
            for (int x=0; x<WIDTH; ++x)
            {
                final double dx = x - WIDTH/2;
                final double r = Math.sqrt(dx*dx + dy2);
                data[i++] = Math.exp(-r/(WIDTH/2)) * (1.0 + Math.cos(2*Math.PI*(r/(WIDTH/6) - phase)));
            }
        }
        return new ArrayDouble(data);
    }

    volatile boolean show_colorbar = true;

    @Override
    public void start(final Stage stage) throws Exception
    {
        final Level level = Level.CONFIG;
        Logger.getLogger("").setLevel(level);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(level);

        final ImagePlot plot = new ImagePlot();

        plot.setColorMapping(ImagePlot.RAINBOW);

        plot.setAutoscale(true);
        plot.setValueRange(0.0, 2.0);

        plot.getXAxis().setGridVisible(true);
        plot.getYAxis().setGridVisible(true);
        plot.getYAxis().setScaleFont(Font.font("Liberation Sans", FontPosture.ITALIC, 25));

        timer.scheduleAtFixedRate(() -> plot.setValue(WIDTH, HEIGHT, computeData()),
                                  200, 100, TimeUnit.MILLISECONDS);

        timer.scheduleAtFixedRate(() ->
        {
            show_colorbar = ! show_colorbar;
            plot.showColorBar(show_colorbar);
        }, 5000, 5000, TimeUnit.MILLISECONDS);


		final Pane root = new Pane(plot);
		plot.widthProperty().bind(root.widthProperty());
		plot.heightProperty().bind(root.heightProperty());

        final Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Image Plot Demo");
        stage.show();

        stage.setOnCloseRequest((event) ->
        {
            timer.shutdown();
            try
            {
                timer.awaitTermination(2, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            plot.dispose();
        });
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}