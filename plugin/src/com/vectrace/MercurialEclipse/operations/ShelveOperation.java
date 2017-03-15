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
 *     Josh Tam        - large files support
 *     Amenel Voglozin - reimplementation after deprecating HgAtticClient
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.commands.IConsole;
import com.vectrace.MercurialEclipse.commands.extensions.HgShelveClient;
import com.vectrace.MercurialEclipse.exception.HgCoreException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class ShelveOperation extends HgOperation {
	private final HgRoot hgRoot;
	private File shelveFileConflict;

	/**
	 * Tells whether the menu entry selected by the user implicitly warrants prompting the user
	 * for information, irrespective of the value of the relevant preference setting.
	 * <p>
	 * This is meant to allow two entries in the Shelve submenu: "Shelve" and "Shelve...", the
	 * latter of which will force the display of a dialog box.
	 */
	private final boolean forceInteraction;

	public ShelveOperation(IWorkbenchPart part, HgRoot hgRoot, boolean forceInteraction) {
		super(part);
		this.hgRoot = hgRoot;
		this.forceInteraction = forceInteraction;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("ShelveOperation.shelvingChanges"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		//
		// Directory where the Shelve extension stores its artifacts
		File shelveDir = new File(hgRoot, HgShelveClient.DEFAULT_FOLDER);
		try {
			monitor.beginTask(Messages.getString("ShelveOperation.shelving"), 5); //$NON-NLS-1$
			monitor.subTask(Messages.getString("ShelveOperation.determiningChanges")); //$NON-NLS-1$
			//
			IConsole console = HgClients.getConsole();
			if (!HgStatusClient.isDirty(hgRoot)) {
				String key = "ShelveOperation.error.nothingToShelve"; //$NON-NLS-1$
				// Let the user know why nothing happens by printing to the console.
				console.printMessage(Messages.getString(key), null);
				throw new HgCoreException(Messages.getString(key));
			}
			monitor.worked(1);
			monitor.subTask(Messages.getString("ShelveOperation.shelvingChanges")); //$NON-NLS-1$

			//
			// Configuration
			String shelveName = hgRoot.getName();
			String shelveCommitMessage = "MercurialEclipse shelve operation";
			boolean useInteraction = MercurialEclipsePlugin.getDefault().getPreferenceStore()
					.getBoolean(MercurialPreferenceConstants.PREF_SHELVE_USE_INTERACTION);
			if (useInteraction || forceInteraction) {
				// TODO get information from the user
			}
			File shelveFile = new File(shelveDir, shelveName + HgShelveClient.EXTENSION);
			if (shelveFile.exists()) {
				shelveFileConflict = shelveFile;
				console.printMessage(Messages.getString("ShelveOperation.error.shelfNotEmpty"), //$NON-NLS-1$
						null);
				throw new HgCoreException(
						Messages.getString("ShelveOperation.error.shelfNotEmpty")); //$NON-NLS-1$
			}

			//
			// Execute the operation
			String res = HgShelveClient.shelve(hgRoot, shelveCommitMessage, shelveName);
			console.printMessage(res, null); // Output the result of the operation to the console.
			monitor.worked(2);

			//
			// Update the project so as to reflect the changes from the shelving.
			monitor.subTask(Messages.getString("ShelveOperation.cleaningDirtyFiles")); //$NON-NLS-1$
			HgUpdateClient.cleanUpdate(hgRoot, ".", null);
		} catch (HgCoreException e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} catch (CoreException e) {
			// cleanup directory which otherwise may contain empty or invalid files and
			// block next shelve operation to execute
			if(shelveDir.isDirectory()){
				ResourceUtils.delete(shelveDir, true);
			}
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		} finally {
			monitor.done();
		}
	}

	public File getShelveFileConflict() {
		return shelveFileConflict;
	}
}
