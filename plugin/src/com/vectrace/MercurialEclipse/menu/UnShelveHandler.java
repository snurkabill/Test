/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bastian         - implementation
 *     Andrei Loskutov - bug fixes
 *     Amenel Voglozin - update following the deprecation of HgAtticClient
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.dialogs.RejectsDialog;
import com.vectrace.MercurialEclipse.dialogs.UnshelveDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.UnShelveOperation;

/**
 * @author bastian
 */
public class UnShelveHandler extends RootHandler {

	@Override
	protected void run(final HgRoot hgRoot) {

		final UnshelveDialog dlg = new UnshelveDialog(MercurialEclipsePlugin.getActiveShell(), HgStatusClient.isDirty(hgRoot));
		if (dlg.open() != Window.OK) {
			return;
		}
		final UnshelveJob job = new UnshelveJob(Messages.getString("UnShelveHandler.Unshelving"),
				hgRoot, dlg.getAbort(), dlg.getContinue(), dlg.getKeep());

		job.schedule();
	}

	private final class UnshelveJob extends SafeWorkspaceJob {

		private final HgRoot hgRoot;
		private final boolean abort;
		private final boolean cont;
		private final boolean keep;

		private UnshelveJob(String name, HgRoot hgRoot, boolean abort, boolean cont, boolean keep) {
			super(name);
			this.hgRoot = hgRoot;
			this.abort = abort;
			this.cont = cont;
			this.keep = keep;
		}

		@Override
		protected IStatus runSafe(IProgressMonitor monitor) {
			try {
				final UnShelveOperation op = new UnShelveOperation((IWorkbenchPart) null, hgRoot, abort, cont, keep);
				op.run(monitor);

				if (op.isConflict()) {
					getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							try {
								new RejectsDialog(getShell(), hgRoot, op.getResult(),
										"UnshelveRejectsDialog.title",
										"UnshelveRejectsDialog.conflict").open();
							} catch (HgException e) {
								// Fallback if couldn't parse rejects
								MessageDialog.openInformation(getShell(), Messages
										.getString("UnShelveHandler.Unshelving"), Messages
										.getString("UnShelveHandler.conflict")
										+ "\n" + op.getResult());
								MercurialEclipsePlugin.logError(e);
							}
						}
					});
				}

				return super.runSafe(monitor);
			} catch (InvocationTargetException e) {
				return new Status(IStatus.WARNING, MercurialEclipsePlugin.ID, 0, e
						.getLocalizedMessage(), e);
			} catch (InterruptedException e) {
				return new Status(IStatus.INFO, MercurialEclipsePlugin.ID, 0, e
						.getLocalizedMessage(), e);
			}
		}
	}
}
