/*******************************************************************************
 * Copyright (c) 2005-2016 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Amenel Voglozin		Implementation (2016-06-18)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.dialogs.ReassignToNewChangesetDialog;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * This action of the Synchronize perspective deals with "New Change Set..." in the "Reassign
 * Changes To" submenu of the context menu.
 * <p>
 * This action is the entry point for the following behavior:
 * <ul>
 * <li>A dialog opens and the user can specify the changeset name and comment
 * <li>Upon the user clicking OK, a new changeset is created
 * <li>All resources below the selected items are reassigned to the new changeset.
 * </ul>
 *
 * @author Amenel VOGLOZIN
 */
public class ReassignToNewChangeSetSynchronizeAction
		extends AbstractReassignToChangesetSynchronizeAction {

	private static final String ID = "hg.reassignToNewChangeset"; //$NON-NLS-1$

	private final UncommittedChangesetGroup group;

	//
	// The operation.
	//
	private class ReassignToNewChangeSetSynchronizeOperation extends SynchronizeModelOperation {

		public ReassignToNewChangeSetSynchronizeOperation(
				ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
			super(configuration, elements);
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			getPart();
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					ReassignToNewChangesetDialog dialog = new ReassignToNewChangesetDialog(
							getShell(), selectedFiles, group);
					dialog.open();
				}
			});
			monitor.done();
		}
	}

	//
	//
	//
	public ReassignToNewChangeSetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider,
			List<IFile> selectedFiles, UncommittedChangesetGroup ucg) {
		super(text, configuration, selectionProvider, selectedFiles);
		//
		this.group = ucg;
		setId(ID);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("elcl16/uncommitted_cs.gif", //$NON-NLS-1$
				"ovr/add_ovr.gif", //$NON-NLS-1$
				IDecoration.TOP_RIGHT));
	}

	//
	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new ReassignToNewChangeSetSynchronizeOperation(configuration, elements);
	}

}
