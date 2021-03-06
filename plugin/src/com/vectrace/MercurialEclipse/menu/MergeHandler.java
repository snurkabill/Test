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
 *     Ilya Ivanov (Intland) - modification
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.merge.MergeContext;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgMergeClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.utils.BranchUtils;
import com.vectrace.MercurialEclipse.views.MergeView;

public class MergeHandler extends RootHandler {

	@Override
	protected void run(HgRoot hgRoot) throws CoreException {
		determineMergeHeadAndMerge(hgRoot, getShell(), new NullProgressMonitor(), false, true);
	}

	public static void determineMergeHeadAndMerge(HgRoot hgRoot, Shell shell, IProgressMonitor monitor,
			boolean autoPickOtherHead, boolean showCommitDialog) throws CoreException {

		// can we do the equivalent of plain "hg merge"?
		ChangeSet cs = HgLogClient.getChangeSet(hgRoot, getHeadForEasyMerge(hgRoot));
		boolean forced = false;

		String forceMessage = "Forced merge (this will discard all uncommitted changes!)";
		boolean hasDirtyFiles = HgStatusClient.isDirty(hgRoot);
		if (cs != null) {
			if (!autoPickOtherHead) {

				String csSummary = "    Changeset: " + cs.getName()
						+ "\n    User: " + cs.getAuthor() + "\n    Date: "
						+ cs.getDateString() + "\n    Summary: " + cs.getSummary();

				String branch = cs.getBranch();
				if (BranchUtils.isDefault(branch)) {
					branch = BranchUtils.DEFAULT;
				}
				String message = MessageFormat.format(Messages
						.getString("MergeHandler.mergeWithOtherHead"), branch, csSummary);

				if(hasDirtyFiles){
					MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
							shell, "Merge", message, forceMessage, false, null, null);
					if(dialog.getReturnCode() != IDialogConstants.YES_ID) {
						cs = null;
					}
					forced = dialog.getToggleState();
				} else {
					MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					mb.setText("Merge");
					mb.setMessage(message);
					if (mb.open() == SWT.NO) {
						cs = null;
					}
				}
			}
		}

		// have to open the dialog until we get a valid changeset
		while (cs == null) {
			RevisionChooserDialog dialog = new RevisionChooserDialog(shell,
					Messages.getString("MergeHandler.mergeWith"), hgRoot); //$NON-NLS-1$
			dialog.setDefaultShowingHeads(true);
			dialog.setDisallowSelectingParents(true);
			dialog.showForceButton(hasDirtyFiles);
			dialog.setForceChecked(forced);
			dialog.setForceButtonText(forceMessage);
			dialog.setRequireChangeset(true);
			if (dialog.open() != IDialogConstants.OK_ID) {
				return;
			}

			cs = dialog.getChangeSet();
			forced = dialog.isForceChecked();
		}

		mergeAndCommit(hgRoot, shell, monitor, showCommitDialog, cs, forced);
	}

	/**
	 * Call from UI thread
	 *
	 * TODO: Run only necessary parts in the UI thread
	 */
	public static void mergeAndCommit(HgRoot hgRoot, Shell shell, IProgressMonitor monitor,
			boolean showCommitDialog, ChangeSet cs, boolean forced) throws HgException,
			CoreException {
		MercurialUtilities.setOfferAutoCommitMerge(true);

		try {
			MergeContext ctx = createMergeContext(hgRoot, cs.getNode(), forced);

			if (mergeAndAutoResolve(hgRoot, ctx, cs.getNode())) {
				if (showCommitDialog) {
					MercurialStatusCache.getInstance().refreshStatus(hgRoot, monitor);
				}
				commitMerge(monitor, hgRoot, cs.getNode(), shell, showCommitDialog);
			} else {
				MergeView.showMergeConflict(hgRoot, ctx, shell);
			}
		} finally {
			new RefreshWorkspaceStatusJob(hgRoot).schedule();
		}
	}

	public static MergeContext createMergeContext(HgRoot hgRoot, String mergeNode, boolean forced) throws HgException {
		return HgMergeClient.merge(hgRoot, mergeNode, forced);
	}
	/**
	 * @return True if the merge can be immediately committed, false if the merge view should be shown
	 */
	public static boolean mergeAndAutoResolve(HgRoot hgRoot, MergeContext ctx, String mergeNode) throws HgException {
		// Is this necessary? Refresh should be done anyway later
		MercurialStatusCache.getInstance().setMergeStatus(hgRoot, mergeNode);

		return HgResolveClient.autoResolve(hgRoot, ctx);
	}

	public static void commitMerge(IProgressMonitor monitor, final HgRoot hgRoot,
			final String mergeChangesetId, final Shell shell, boolean showCommitDialog) {

		monitor.subTask(com.vectrace.MercurialEclipse.wizards.Messages.getString("PullRepoWizard.pullOperation.commit")); //$NON-NLS-1$
		if (!showCommitDialog) {
			CommitMergeHandler.commitMerge(hgRoot, HgCommitMessageManager
					.getDefaultCommitName(hgRoot), "Merge with " + mergeChangesetId);
		} else {
			CommitMergeHandler.commitMergeWithCommitDialog(hgRoot, shell);
		}
		monitor.worked(1);
	}

	private static Changeset getHeadForEasyMerge(HgRoot hgRoot) throws HgException {
		ArrayList<Changeset> otherHeads = getOtherHeadsInCurrentBranch(hgRoot);

		if (otherHeads != null && otherHeads.size() == 1) {
			return otherHeads.get(0);
		}

		// can not perform easy merge - need to run wizard
		return null;
	}

	/**
	 * Returns list of Heads in the same branch as current head. Current head itself is not included.
	 * @param hgRoot
	 * @return
	 * @throws HgException
	 */
	private static ArrayList<Changeset> getOtherHeadsInCurrentBranch(HgRoot hgRoot) throws HgException {
		ArrayList<Changeset> result = getHeadsInCurrentBranch(hgRoot);
		ChangeSet currentRevision = LocalChangesetCache.getInstance().getCurrentChangeSet(hgRoot);

		Changeset csToRemove = null;
		for (Changeset cs : result) {
			// can't be the current
			if (cs.getNode().equals(currentRevision.getNode())) {
				csToRemove = cs;
				break;
			}
		}

		if (csToRemove != null) {
			result.remove(csToRemove);
		}

		return result;
	}

	/**
	 * Returns list of all Heads in the same branch as current head
	 * @param hgRoot
	 * @return
	 * @throws HgException
	 */
	public static ArrayList<Changeset> getHeadsInCurrentBranch(HgRoot hgRoot) throws HgException {
		ArrayList<Changeset> otherHeads = new ArrayList<Changeset>();
		Changeset[] heads = HgLogClient.getHeads(hgRoot);

		ChangeSet currentRevision = LocalChangesetCache.getInstance().getCurrentChangeSet(hgRoot);
		if (currentRevision == null) {
			return otherHeads;
		}
		String branch = currentRevision.getBranch();

		for (Changeset cs : heads) {
			// must match branch
			if (!BranchUtils.same(branch, cs.getBranch())) {
				continue;
			}

			otherHeads.add(cs);
		}

		return otherHeads;
	}

}
