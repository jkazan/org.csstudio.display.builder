/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.rcp.run;

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;

/** Part that hosts display builder runtime
 *
 *  <p>Hosts FXCanvas in SWT
 *
 *  @author Kay Kasemir
 */
public class RuntimeViewPart extends ViewPart
{
	// FXViewPart could save a tiny bit code, but this may allow more control.
	// e4view would allow E4-like POJO, but unclear how representation
	// would then best find the newly created RuntimeViewPart to set its input etc.
	// --> Using E3 ViewPart
    public final static String ID = "org.csstudio.display.builder.rcp.run.RuntimeViewPart";

    public static RuntimeViewPart open() throws Exception
    {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    	return (RuntimeViewPart) page.showView(ID, UUID.randomUUID().toString(), IWorkbenchPage.VIEW_ACTIVATE);
    }
    
    @Override
    public void createPartControl(final Composite parent)
    {
        final FXCanvas fx_canvas = new FXCanvas(parent, SWT.NONE);
        final Group root = new Group();

        new JFXDisplayRuntime().representModel(root, null);

        final Scene scene = new Scene(root);
        fx_canvas.setScene(scene);
        
        createContextMenu(parent);
    }

    private void createContextMenu(final Control parent)
    {
    	final MenuManager mm = new MenuManager();
    	mm.add(new Action("SWT Item")
    	{
			@Override
			public void run()
			{
				System.out.println("RCP Context menu");
			}
		});
    	final Menu menu = mm.createContextMenu(parent);
    	parent.setMenu(menu);
    }

	@Override
    public void setFocus()
    {
    }
}
