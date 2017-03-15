/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *     Amenel Voglozin - reimplementation after deprecating HgAtticClient
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.IConsole;
import com.vectrace.MercurialEclipse.commands.extensions.HgShelveClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

/**
 * Check {@link #isConflict()} after running and display appropriate message to user.
 *
 * @author bastian
 */
public class UnShelveOperation extends HgOperation {
	private final HgRoot hgRoot;
	private boolean conflict;
	private final boolean abort;
	private final boolean cont;
	private final boolean keep;

	public UnShelveOperation(IWorkbenchPart part, HgRoot hgRoot, boolean abort, boolean cont, boolean keep) {
		super(part);
		this.hgRoot = hgRoot;
		this.abort = abort;
		this.cont = cont;
		this.keep = keep;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("UnShelveOperation.UnshelvingChanges"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			// get modified files
			monitor.beginTask(Messages.getString("UnShelveOperation.Unshelving"), 4); //$NON-NLS-1$

			monitor.subTask(Messages.getString("UnShelveOperation.GettingChanges")); //$NON-NLS-1$
			File shelveDir = new File(hgRoot, HgShelveClient.DEFAULT_FOLDER);

			if (shelveDir.exists()) {
				File shelveFile = new File(shelveDir, hgRoot.getName() + HgShelveClient.EXTENSION);
				IConsole console = HgClients.getConsole();
				if (shelveFile.exists()) {
					monitor.worked(1);
					monitor.subTask(Messages.getString("UnShelveOperation.applyingChanges")); //$NON-NLS-1$
					try {
						String output = HgShelveClient.unshelve(hgRoot, abort, cont, keep);
						monitor.worked(2);
						console.printMessage(output, null);
						monitor.subTask(Messages.getString("UnShelveOperation.refreshingProject")); //$NON-NLS-1$
					} catch (HgException e) {
						if (HgShelveClient.isUnshelveConflict(e)) {
							conflict = true;
							result = e.getLocalizedMessage();
						} else {
							throw e;
						}
					} finally {
						Job job = new RefreshWorkspaceStatusJob(hgRoot);
						job.schedule();
						if (conflict) {
							job.join();
						}
					}
				} else {
					console.printMessage(Messages.getString("UnShelveOperation.error.ShelfEmpty"),
							null);
					throw new HgException(Messages.getString("UnShelveOperation.error.ShelfEmpty")); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.done();
		}
	}

	/**
	 * @return Whether a conflict occurred while unshelving
	 */
	public boolean isConflict() {
		return conflict;
	}
}
