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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.model.GroupedUncommittedChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * This action in the Reassign Changes submenu of the Synchronize perspective deals with reassigning
 * files of the selected items (changesets, paths and files) to given changesets.
 * <p>
 * This action does not lead to a UI interaction.
 *
 * @author Amenel VOGLOZIN
 */
public class ReassignToChangeSetSynchronizeAction
		extends AbstractReassignToChangesetSynchronizeAction {

	private static final String ID = "hg.reassignToChangeset"; //$NON-NLS-1$

	private final GroupedUncommittedChangeSet changeset;

	/**
	 * We need this so that the moving of files to a changeset can be reflected in the UI.
	 */
	private final UncommittedChangesetGroup group;

	//
	// The operation.
	//
	private class ReassignToChangeSetSynchronizeOperation extends SynchronizeModelOperation {

		public ReassignToChangeSetSynchronizeOperation(ISynchronizePageConfiguration configuration,
				IDiffElement[] elements) {
			super(configuration, elements);
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			group.move(selectedFiles.toArray(new IFile[0]), changeset);
			monitor.done();
		}
	}

	public ReassignToChangeSetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider,
			UncommittedChangesetGroup ucg, List<IFile> selectedFiles,
			GroupedUncommittedChangeSet cs, Integer order) {
		super(text, configuration, selectionProvider, selectedFiles);
		//
		this.changeset = cs;
		this.group = ucg;
		setId(ID + "." + order.toString());
	}

	//
	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new ReassignToChangeSetSynchronizeOperation(configuration, elements);
	}

}
