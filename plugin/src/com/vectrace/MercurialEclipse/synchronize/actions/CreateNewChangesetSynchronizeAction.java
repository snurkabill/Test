/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 * 		Amenel Voglozin - changes: Create New Change Set is now possible on any child of Uncommitted
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.navigator.CommonViewer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.PathFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * Creates new empty uncommitted changeset with default name
 */
public class CreateNewChangesetSynchronizeAction extends SynchronizeModelAction {

	public static final String ID = "hg.createNewChangeset";

	public CreateNewChangesetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
		setId(ID);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("elcl16/uncommitted_cs.gif", "ovr/add_ovr.gif",
				IDecoration.TOP_RIGHT));
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		IStructuredSelection sel = getStructuredSelection();
		if (isSupported(sel.toArray())) {
			return new SynchronizeModelOperation(configuration, elements) {

				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					Viewer viewer = configuration.getPage().getViewer();
					if(!(viewer instanceof ContentViewer)){
						return;
					}
					CommonViewer commonViewer = (CommonViewer) viewer;
					final HgChangeSetContentProvider csProvider = OpenAction.getProvider(commonViewer.getNavigatorContentService());
					IUncommitted uc = csProvider.getUncommittedEntry();

					if (uc instanceof UncommittedChangesetGroup) {
						((UncommittedChangesetGroup)uc).create(new IFile[0]);
					} else {
						MercurialEclipsePlugin.logError(
								"Unexped invocation of CreateNewChangesetSynchronizeAction",
								new IllegalStateException());
					}
				}
			};
		}
		return null;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		boolean updateSelection = super.updateSelection(selection);
		if(!updateSelection){
			Object[] array = selection.toArray();
			return isSupported(array);
		}
		return updateSelection;
	}

	/**
	 * Tells whether this action can be performed (and enabled) for the given selection. This will
	 * be positive if and only if all objects have the "Uncommitted" top-level changeset group as
	 * their parent.
	 * <p>
	 * A selected object can be ("OK" means that the object's ancestry goes back to Uncommitted as
	 * its top-level parent):
	 * <ul>
	 * <li>A top-level changeset group (ie Uncommitted –OK–, Incoming –NOK– or Outgoing –NOK–)
	 * <li>A workspace changeset (aka WorkingChangeSet): OK
	 * <li>A repository changeset (under Incoming or Outgoing): NOK
	 * <li>A folder (aka PathFromChangeSet – OK if the parent is a workspace change set)
	 * <li>A file (aka FileFromChangeSet – OK if its parent changeset is a workspace changeset)
	 * </ul>
	 *
	 * @param selectedObjects
	 *            The objects selected in the Synchronize view ("Enable local changesets" is
	 *            necessarily activated)
	 * @return <code>true</code> if all objects have the uncommitted changeset group as their
	 *         parent.
	 */
	private static boolean isSupported(Object[] selectedObjects) {
		if (selectedObjects.length == 0) {
			return false;
		}

		for (Object object : selectedObjects) {
			if (object instanceof JHgChangeSet) {
				return false;
			}
			if (object instanceof UncommittedChangesetGroup || object instanceof WorkingChangeSet) {
				continue;
			} else if (object instanceof FileFromChangeSet) {
				FileFromChangeSet file = (FileFromChangeSet) object;
				if (!(file.getChangeset() instanceof WorkingChangeSet)) {
					return false;
				}
			} else if (object instanceof PathFromChangeSet) {
				Object parent = ((PathFromChangeSet) object).getParent();
				if (parent instanceof UncommittedChangesetGroup || parent instanceof WorkingChangeSet) {
					continue;
				}
			} else {
				return false;
			}
		}

		return true;
	}


}
