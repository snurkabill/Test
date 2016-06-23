/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Watson 		- implementation
 *     Amenel VOGLOZIN 		- (2016-06-22) Only selected files (or the current editor file) will be listed in the dialog
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitHandler;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class ActionCommit extends ActionDelegate {

	/**
	 * The action has been activated. The argument of the method represents the 'real' action
	 * sitting in the workbench UI.
	 *
	 * @throws HgException
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		try {
			List<IResource> resources = new ArrayList<IResource>();
			boolean merging = collectResourcesToCommit(resources);
			if (!merging) {
				if (resources.size() == 0) {
					resources = getSelectedHgProjects();
				}
				doRun(resources);
			} else {
				Shell shell = MercurialEclipsePlugin.getActiveShell();
				boolean doCommit = MessageDialog.openConfirm(shell,
						Messages.getString("ActionCommit.HgCommit"), //$NON-NLS-1$
						Messages.getString("ActionCommit.mergeIsRunning")); //$NON-NLS-1$
				if (doCommit) {
					doRun(resources);
				}
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	private static void doRun(List<IResource> resources) throws HgException {
		new CommitHandler().run(resources);
	}

	// @Amenel: Inspired from ActionRevert. Given that both Revert and Commit operate on modified
	// files, I've chosen to maintain the merge flag.
	private boolean collectResourcesToCommit(List<IResource> resources) {
		boolean mergeIsRunning = false;
		for (Object obj : selection.toList()) {
			if (obj instanceof IResource) {
				IResource resource = (IResource) obj;
				boolean merging = MercurialStatusCache.getInstance().isMergeInProgress(resource);
				if (merging) {
					mergeIsRunning = true;
				}
				boolean supervised = MercurialTeamProvider.isHgTeamProviderFor(resource);
				if (supervised) {
					resources.add(resource);
				}
			}
		}
		return mergeIsRunning;
	}

}
