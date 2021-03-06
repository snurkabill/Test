/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.aragost.javahg.commands.ExecutionException;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIgnoreClient;
import com.vectrace.MercurialEclipse.dialogs.IgnoreDialog;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.ResourceDecorator;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class HgIgnoreHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		IgnoreDialog dialog;
		switch(resource.getType()) {
			case IResource.FILE:
				dialog = new IgnoreDialog(getShell(), (IFile) resource);
				break;
			case IResource.FOLDER:
				dialog = new IgnoreDialog(getShell(), (IFolder) resource);
				break;
			default:
				dialog = new IgnoreDialog(getShell());
		}

		if(dialog.open() == IDialogConstants.OK_ID) {
			HgRoot root = MercurialRootCache.getInstance().getHgRoot(resource);
			boolean bRefreshHgIgnore = !HgIgnoreClient.hasHgIgnore(root);

			switch(dialog.getResultType()) {
				case FILE:
					HgIgnoreClient.addFile(dialog.getFile());
					break;
				case EXTENSION:
					HgIgnoreClient.addExtension(dialog.getFile());
					break;
				case FOLDER:
					HgIgnoreClient.addFolder(dialog.getFolder());
					break;
				case GLOB:
					HgIgnoreClient.addGlob(resource.getProject(), dialog.getPattern());
					break;
				case REGEXP:
					HgIgnoreClient.addRegexp(resource.getProject(), dialog.getPattern());
					break;
			}
			refreshStatus(root, bRefreshHgIgnore);
		}
	}

	private static void refreshStatus(final HgRoot root, boolean bRefreshHgIgnore) {
		// TODO: Evaluate using only RefreshStatusJob
		final Job job = new Job("Refreshing status for ignored resources: " + root.getName()){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// update the HgRoot of the resource. This will update all projects that contain this HgRoot
					MercurialStatusCache.getInstance().refreshStatus(root, monitor);

				} catch (ExecutionException e) {
					MercurialEclipsePlugin.logError(Messages.getString("HgIgnoreHandler.unableToRefreshProject"), //$NON-NLS-1$
							e);
					return Status.CANCEL_STATUS;
				}

				// fix for issue #10152:
				// trigger decorator update for resources being ignored
				// For some reasons, resource.touch() and refreshLocal() isn't enough
				// to get updated status into the Navigator/Explorer views
				ResourceDecorator.updateClientDecorations();
				return Status.OK_STATUS;
			}
		};

		if (bRefreshHgIgnore) {
			Job job2 = new RefreshWorkspaceStatusJob(root);

			job2.addJobChangeListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					job.schedule();
				}
			});

			job2.schedule();
		} else {
			job.schedule();
		}
	}

}
