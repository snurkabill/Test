/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation (2016-08-13)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.GroupedUncommittedChangeSet;

/**
 * Base class for handlers of Synchronize perspective operations that operate upon local changesets.
 *
 * @author Amenel Voglozin
 *
 */
public abstract class SynchronizeChangesetHandler extends AbstractHandler {
	private GroupedUncommittedChangeSet changeset;
	private Shell shell;

	protected Shell getShell() {
		return shell != null ? shell : MercurialEclipsePlugin.getActiveShell();
	}

	protected GroupedUncommittedChangeSet getSelectedChangeset() {
		return changeset;
	}

	/**
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		changeset = null;

		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		try {
			if (selection != null && selection.size() == 1) {
				Object selectionObject = selection.toArray()[0];
				if (selectionObject instanceof GroupedUncommittedChangeSet) {
					changeset = (GroupedUncommittedChangeSet) selectionObject;
				}
			}
			if (changeset == null) {
				// NOP
				return null;
			}
			run(getSelectedChangeset());
		} catch (Exception e) {
			MessageDialog.openError(getShell(), Messages.getString("SingleResourceHandler.hgSays"), //$NON-NLS-1$
					e.getMessage() + Messages.getString("SingleResourceHandler.seeErrorLog")); //$NON-NLS-1$
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	protected abstract void run(GroupedUncommittedChangeSet cs) throws Exception;
}
