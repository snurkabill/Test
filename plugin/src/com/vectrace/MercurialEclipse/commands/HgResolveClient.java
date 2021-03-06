/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Amenel Voglozin           - Added Autoresolve recap dialog
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.aragost.javahg.commands.ResolveCommand;
import com.aragost.javahg.commands.ResolveStatusLine;
import com.aragost.javahg.commands.flags.ResolveCommandFlags;
import com.aragost.javahg.merge.ConflictResolvingContext;
import com.aragost.javahg.merge.FlagConflict;
import com.aragost.javahg.merge.KeepDeleteConflict;
import com.google.common.collect.Lists;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.dialogs.AutoresolveRecapDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ResolveStatus;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgResolveClient extends AbstractClient {

	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

	/**
	 * List merge state of files after merge
	 */
	public static List<ResolveStatus> list(IResource res) throws HgException {
		HgRoot hgRoot = getHgRoot(res);
		IProject project = res.getProject();

		List<ResolveStatusLine> list = ResolveCommandFlags.on(hgRoot.getRepository()).list();
		List<ResolveStatus> result = Lists.newArrayList();

		for (ResolveStatusLine line : list) {
			// Status line is always hg root relative. For those projects
			// which has different project root hg root relative path must
			// be converted to project relative
			IResource iFile = ResourceUtils.convertRepoRelPath(hgRoot, project, line.getFileName());
			if (iFile != null) {
				result.add(new ResolveStatus(iFile, line.getType()));
			}
		}
		return result;
	}

	/**
	 * List merge state of files after merge
	 */
	public static List<ResolveStatus> list(HgRoot hgRoot) {
		List<ResolveStatusLine> list = ResolveCommandFlags.on(hgRoot.getRepository()).list();
		List<ResolveStatus> result = Lists.newArrayList();

		for (ResolveStatusLine line : list) {
			// Status line is always hg root relative. For those projects
			// which has different project root (always deeper than hg root)
			// hg root relative path must be converted
			IResource iFile = ResourceUtils.getFileHandle(hgRoot.toAbsolute(new Path(line
					.getFileName())));
			if (iFile != null) {
				result.add(new ResolveStatus(iFile, line.getType()));
			}
		}

		return result;
	}

	/**
	 * Mark a resource as resolved ("R")
	 */
	public static void markResolved(HgRoot hgRoot, IFile ifile) throws HgException {
		File file = ResourceUtils.getFileHandle(ifile);
		try {
			ResolveCommandFlags.on(hgRoot.getRepository()).mark(file.getCanonicalPath());
			// cleanup .orig files left after merge
			deleteOrigFile(file);
			refreshStatus(ifile);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Deletes the .orig file that is left after a merge, <b>provided that the file is not being
	 * tracked by Mercurial</b>. This can safely be called even when not sure that the said file
	 * exists; no errors will occur and no exceptions will be thrown.
	 *
	 * @param file
	 * @throws HgException
	 */
	private static void deleteOrigFile(File file) throws HgException {
		File origFile = new File(file.getAbsolutePath() + ".orig");
		if (origFile.isFile()) {
			IResource fileToDelete = ResourceUtils.convert(origFile);

			if (STATUS_CACHE.isSupervised(fileToDelete)) {
				return; // We won't delete tracked .orig files.
			}

			boolean deleted = origFile.delete();
			if (!deleted) {
				MercurialEclipsePlugin.logInfo("Failed to cleanup " + origFile
						+ " file after merge", null);
			} else {
				try {
					fileToDelete.refreshLocal(IResource.DEPTH_ZERO, null);
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
	}

	/**
	 * Mark a resource as unresolved ("U")
	 */
	public static void markUnresolved(HgRoot hgRoot, IFile ifile) throws HgException {
		File file = ResourceUtils.getFileHandle(ifile);

		try {
			ResolveCommandFlags.on(hgRoot.getRepository()).unmark(file.getCanonicalPath());
			refreshStatus(ifile);
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	public static void deleteKeepDeleteConflict(HgRoot hgRoot, ConflictResolvingContext ctx, KeepDeleteConflict c, IFile iFile) throws HgException {
		try {
			c.delete();
			ctx.getKeepDeleteConflicts().remove(c);
			refreshStatus(iFile);
		} catch (Exception e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	private static void refreshStatus(IResource res) throws HgException {
		MercurialStatusCache.getInstance().refreshStatus(res, null);
		ResourceUtils.touch(res);
	}

	/**
	 * Executes resolve command to find change sets necessary for merging
	 * <p>
	 * WARNING: This method potentially reverts changes!
	 * <p>
	 * Future: We should write some python to interface with the Mercurial API directly to get this
	 * info so we don't have to do operations with side effects or rely on --debug output.
	 *
	 * @param file
	 *            The file to consider
	 * @return An array of length 3 of changeset ids: result[0] - 'my' result[1] - 'other' result[2]
	 *         - 'base'
	 * @throws HgException
	 * @deprecated TODO: find a better way to do this
	 */
	@Deprecated
	public static String[] restartMergeAndGetChangeSetsForCompare(IFile file) throws HgException {
		String[] results = new String[3];
		HgCommand command = new HgCommand("resolve", "Invoking resolve to find parent information",
				file, false);

		command.addOptions("--config", "ui.merge=internal:mustfail", "--debug");
		command.addFiles(file);

		String stringResult = "";
		try {
			command.executeToString();
		} catch (HgException e) {
			// exception is expected here
			stringResult = e.getMessage();
		}

		String filename = file.getName();
		String patternString = "my .*" + filename + "@?([0-9a-fA-F]*)\\+?[\\s]" + "other .*"
				+ filename + "@?([0-9a-fA-F]*)\\+?[\\s]" + "ancestor .*" + filename
				+ "@?([0-9a-fA-F]*)\\+?[\\s]";

		Matcher matcher = Pattern.compile(patternString).matcher(stringResult);

		if (matcher.find() && matcher.groupCount() == 3) {
			results[0] = matcher.group(1); // my
			results[1] = matcher.group(2); // other
			results[2] = matcher.group(3); // ancestor
		}

		return results;
	}

	/**
	 * Resolve all conflicts using pre-merge, or external merge tool if configured
	 * @param hgRoot TODO
	 * @param hgRoot The root to resolve for
	 *
	 * @return True if all conflicts are resolved
	 */
	public static boolean autoResolve(HgRoot hgRoot, ConflictResolvingContext ctx) {
		// Never auto-resolve keep-delete conflicts

		// TODO: should this instead be done in the merge view asyncronously?
		for (FlagConflict conflict : ctx.getFlagConflicts()) {

		}

		boolean r = autoResolve(hgRoot);

		// keep-delete conflicts should not be auto-resolved
		if ( r && ! ctx.getKeepDeleteConflicts().isEmpty() ) {
			return false;
		}

		return r;
	}

	/**
	 * Attempt to externally resolve all conflicts: If external merge tool preference is set, simply
	 * invokes the resolve operation. Otherwise, <code>resolve</code> is invoked with an invalid
	 * merge tool so Mercurial's premerge algorithm is invoked and the file is left unresolved if
	 * pre-merge fails.
	 * <p>
	 * NOTE</u>: the .orig files created by Mercurial when not using an external tool will be
	 * deleted.
	 *
	 * @param hgRoot
	 *            The root to resolve for
	 * @return <code>true</code> if all conflicts are resolved.
	 */
	public static boolean autoResolve(HgRoot hgRoot) {

		//
		// First, a tool is used to try to resolve any conflicts.
		//
		if (isUseExternalMergeTool()) {
			ResolveCommand command = ResolveCommandFlags.on(hgRoot.getRepository()).all();

			command.cmdAppend("--config", "ui.merge=");
			command.execute();
		} else {
			// Do resolve one by one because we're using an invalid merge tool so only pre-merge is done.
			for(ResolveStatusLine line : ResolveCommandFlags.on(hgRoot.getRepository()).list()) {
				if (line.getType() == ResolveStatusLine.Type.UNRESOLVED) {
					ResolveCommandFlags.on(hgRoot.getRepository()).tool("simplemerge").execute(line.getFileName());
				}
			}
		}

		//
		// Second, we check that **all** conflicts were indeed resolved; if so, we'll return true,
		// otherwise we return false.
		//
		boolean allConflictsResolved = true; // This is the variable whose value we will return.

		List<ResolveStatusLine> postAutoresolveList = ResolveCommandFlags.on(hgRoot.getRepository()).list();

		// Files that undergo a merge: they were modified both locally and remotely.
		List<IResource> resources = new ArrayList<IResource>();

		for (ResolveStatusLine line : postAutoresolveList) {
			IResource resource = ResourceUtils
					.getFileHandle(hgRoot.toAbsolute(new Path(line.getFileName())));
			resources.add(resource);

			if (line.getType() == ResolveStatusLine.Type.UNRESOLVED) {
				allConflictsResolved = false;
			} else {
				//
				// The **untracked** .orig file is deleted when the pre-merge was successful (using
				// an external tool is not covered; we leave the deletion to the user).
				if (!isUseExternalMergeTool()) {
					File file = ResourceUtils.getFileHandle(resource);
					try {
						deleteOrigFile(file);
						refreshStatus(resource);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
		}

		//
		// Third, we show the dialog, only if the user wants to see it and there's sth to show (there'll be
		// sth to show only if files were modified locally AND remotely).
		//
		boolean showRecapDialog = MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_RESOLVE_SHOW_RECAP_DIALOG);
		if (showRecapDialog && resources.size() > 0) {
			final AutoresolveRecapDialog dialog = new AutoresolveRecapDialog(
					MercurialEclipsePlugin.getActiveShell(), resources, allConflictsResolved);
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					dialog.open();
				}
			});
		}

		return allConflictsResolved;
	}


	@SuppressWarnings("unused")
	private static void writeToConsole( String name, String line ) {
		findConsole( name ).activate();
		getConsoleWriter(name).println(line);
	}

	private static MessageConsoleStream getConsoleWriter( String name ) {
		MessageConsole c = findConsole( name );
		c.activate();
		return c.newMessageStream();
	}

	private static MessageConsole findConsole( String name ) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for ( int i = 0; i < existing.length; i++ ) {
			if ( name.equals( existing[i].getName() ) ) {
				return (MessageConsole)existing[i];
			}
		}
		//no console found, so create a new one
		MessageConsole myConsole = new MessageConsole( name, null );
		conMan.addConsoles( new IConsole[] { myConsole } );
		return myConsole;
	}
}
