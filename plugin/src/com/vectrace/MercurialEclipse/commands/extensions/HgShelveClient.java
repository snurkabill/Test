/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation, based on HgAtticClient.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * As of Hg 4.0, {@code shelve} and [{@code unshelve} are part of the Shelve extension, which is
 * part of the distribution but disabled. Therefore, the extension is explicitly enabled in all
 * commands of this implementation.
 * <p>
 * TODO:
 * <ul>
 * <li>use javahg
 * <li>define UI settings
 * <ul>
 * <li>Use interaction?
 * <li>hgshelve.maxbackups
 * </ul>
 * <p>
 *
 * @author Amenel Voglozin
 *
 */
public class HgShelveClient extends AbstractClient {
	/**
	 * Directory where that the Shelve extension saves shelved changes.
	 */
	public final static String DEFAULT_FOLDER = ".hg" + File.separator //$NON-NLS-1$
			+ "shelved"; //$NON-NLS-1$

	/**
	 * File extension that the Shelve extension uses.
	 */
	public final static String EXTENSION = ".patch"; //$NON-NLS-1$

	/**
	 * Runs the {@code unshelve} command from the Shelve extension.
	 * <p>
	 * From the hg 4.0 documentation:
	 *
	 * <pre>
	 Options:
	-A, --addremove	mark new/missing files as added/removed before shelving
	-u, --unknown	store unknown files in the shelve
	--cleanup	delete all shelved changes
	--date <DATE>		shelve with the specified commit date
	-d, --delete	delete the named shelved change(s)
	-e, --edit	invoke editor on commit messages
	-l, --list	list current shelves
	-m, --message &lt;TEXT&gt;	use text as shelve message
	-n, --name &lt;NAME&gt; 	use the given name for the shelved commit
	-p, --patch	show patch
	-i, --interactive	interactive mode, only works while creating a shelve
	--stat	output diffstat-style summary of changes
	-I, --include &lt;PATTERN[+]&gt;	include names matching the given patterns
	-X, --exclude &lt;PATTERN[+]&gt;	exclude names matching the given patterns
	 * </pre>
	 *
	 * In this implementation, this command adds the {@code --addremove} and {@code --unknown}
	 * options are specified, with no possibilities for the user to override this behavior.
	 *
	 * @param hgRoot
	 * @param commitMessage
	 * @param name
	 *            name for the shelved commit
	 * @return
	 * @throws HgException
	 */
	public static String shelve(HgRoot hgRoot, String commitMessage, String name)
			throws HgException {
		HgCommand cmd = new HgCommand("shelve", "Invoking shelve on the Shelve extension", hgRoot,
				false);

		// Activate the Shelve extension.
		cmd.addOptions("--config", "extensions.shelve=");

		// Default options
		cmd.addOptions("--addremove");
		cmd.addOptions("--unknown");

		if (commitMessage != null && commitMessage.length() > 0) {
			cmd.addOptions("--message", commitMessage); // $NON-NLS-1$
		}

		cmd.addOptions("--name", name); // $NON-NLS-1$

		return cmd.executeToString();
	}

	/**
	 * Runs the {@code unshelve} command from the Shelve extension.
	 * <p>
	 * From the hg 4.0 documentation:
	 *
	 * <pre>
	 * Options:-a, --abort	abort an incomplete unshelve operation
	-c, --continue	continue an incomplete unshelve operation
	-k, --keep	keep shelve after unshelving
	-t, --tool <VALUE>
	specify merge tool
	--date <DATE>	set date for temporary commits (DEPRECATED)
	 *
	 * </pre>
	 *
	 *
	 * @param hgRoot
	 * @param abort
	 * @param cont
	 * @param keep
	 * @return
	 * @throws HgException
	 */
	public static String unshelve(HgRoot hgRoot, boolean abort, boolean cont, boolean keep)
			throws HgException {
		AbstractShellCommand cmd = new HgCommand("unshelve",
				"Invoking unshelve on the Shelve extension", hgRoot, false);

		// Activate the Shelve extension.
		cmd.addOptions("--config", "extensions.shelve=");

		// TODO allow the user to specify a merge tool

		if (abort) {
			cmd.addOptions("--abort");
		}
		if (cont) {
			cmd.addOptions("--continue");
		}
		if (keep) {
			cmd.addOptions("--keep");
		}
		return cmd.executeToString();
	}

	/**
	 * Determine if the given exception indicates a conflict occurred at unshelving a shelved change
	 *
	 * @param e
	 *            The exception to check
	 * @return <code>true</code> if the exception indicates a conflict occurred
	 */
	public static boolean isUnshelveConflict(HgException e) {
		String lastLine = e.getMessage().substring(e.getMessage().lastIndexOf("\n") + 1);
		return lastLine.startsWith("unresolved conflicts");
	}

}
