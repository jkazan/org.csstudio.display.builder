/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.propsheet;

import org.csstudio.display.builder.util.undo.UndoableAction;
import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ModelItem;

/** Undo-able command to change item's point size
 *  @author Kay Kasemir
 */
public class ChangePointSizeCommand extends UndoableAction
{
    final private ModelItem item;
    final private int old_size, new_size;

    /** Register and perform the command
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param item Model item to configure
     *  @param new_size New value
     */
    public ChangePointSizeCommand(final UndoableActionManager operations_manager,
            final ModelItem item, final int new_size)
    {
        super(Messages.PointSize);
        this.item = item;
        this.old_size = item.getPointSize();
        this.new_size = new_size;
        operations_manager.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        item.setPointSize(new_size);
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        item.setPointSize(old_size);
    }
}
