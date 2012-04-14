/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 * TODO: use JavaHg
 */
public class HgBackoutClient extends AbstractClient {

	/**
	 * Backout of a changeset
	 *
	 * @param hgRoot
	 *            the project
	 * @param backoutRevision
	 *            revision to backout
	 * @param merge
	 *            flag if merge with a parent is wanted
	 * @param msg
	 *            commit message
	 */
	public static String backout(final HgRoot hgRoot, ChangeSet backoutRevision,
			boolean merge, String msg, String user) throws HgException {

		HgCommand command = new HgCommand("backout", //$NON-NLS-1$
				"Backing out changeset " + backoutRevision.getNode(), hgRoot, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));

		addMergeToolPreference(command);

		command.addOptions("-r", backoutRevision.getNode(), "-m", msg); //$NON-NLS-1$ //$NON-NLS-2$
		command.addUserName(user);
		if (merge) {
			command.addOptions("--merge"); //$NON-NLS-1$
		}

		String result = command.executeToString();
		command.rememberUserName();
		return result;
	}

	public static boolean isMergeError(HgException e) {
		// eg: (without --merge)
		// reverting src/file1.text
		// adding src/file2.txt
		// removing src/file22.txt
		// 0 files updated, 0 files merged, 0 files removed, 1 files unresolved
		// use 'hg resolve' to retry unresolved file merges.
		return HgUpdateClient.isWorkspaceUpdateConflict(e);
	}

}
