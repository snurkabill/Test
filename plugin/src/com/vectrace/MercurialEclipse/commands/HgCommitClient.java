/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     StefanC
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland)
 *     Andrei Loskutov           - bug fixes
 *     Amenel Voglozin           - added listeners and notification (bug #337-Quick Diff refresh)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.ExecutionException;
import com.aragost.javahg.commands.flags.CommitCommandFlags;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.extensionpoint.definition.handlers.ActionListenerContributionDispatcher;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 */
public class HgCommitClient extends AbstractClient {

	private static List<IPostCommitListener> postCommitListeners = null;
	private static Job prevNotification = null;

	/**
	 * Commit given resources and refresh the caches for the associated projects
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commitResources(List<IResource> resources, String user, String message,
			boolean closeBranch, boolean amend, IProgressMonitor monitor) throws HgException {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot root = mapEntry.getKey();
			if (monitor != null) {
				if (monitor.isCanceled()) {
					break;
				}
				monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
			}
			List<IResource> files = mapEntry.getValue();

			commit(root, AbstractClient.toFiles(files), user, message, closeBranch, amend);
		}
		for (HgRoot root : resourcesByRoot.keySet()) {
			new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
	}

	/**
	 * Commit given hg root with all checked out/added/deleted changes and refresh the caches for
	 * the associated projects
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commitResources(HgRoot root, String user, String message, boolean closeBranch,
			boolean amend, IProgressMonitor monitor) throws HgException {
		monitor.subTask(Messages.getString("HgCommitClient.commitJob.committing") + root.getName()); //$NON-NLS-1$
		List<File> emptyList = Collections.emptyList();
		try {
			commit(root, emptyList, user, message, closeBranch, amend);
		} finally {
			new RefreshRootJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
		}
	}

	/**
	 * Performs commit. No refresh of any caches is done afterwards.
	 *
	 * <b>Note</b> clients should not use this method directly, it is NOT private for tests only
	 */
	protected static void commit(HgRoot hgRoot, List<File> files, String user, String message,
			boolean closeBranch, boolean amend) throws HgException {
		CommitCommand command = CommitCommandFlags.on(hgRoot.getRepository());

		user = MercurialUtilities.getDefaultUserName(user);
		command.user(user);

		if (closeBranch) {
			command.closeBranch();
		}

		if (amend) {
			command.amend();
		}

		command.message(message);

		try {
			Changeset tipChangeSet = amend ? command.getRepository().tip() : null;
			Changeset changeSet = command.execute(files.toArray(new File[files.size()]));

			if (amend && changeSet != null) {
				ActionListenerContributionDispatcher.onAmend(tipChangeSet == null ? Changeset.NULL_ID
						: tipChangeSet.getNode(), changeSet.getNode());
			}
		} catch (ExecutionException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}

		HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
		firePostCommitEvent();
	}

	/**
	 * Commit given project after the merge and refresh the caches. Implementation note: after
	 * merge, no files should be specified.
	 *
	 * Note: refreshes local and outgoing status
	 */
	public static void commit(HgRoot hgRoot, String user, String message) {
		CommitCommand command = CommitCommandFlags.on(hgRoot.getRepository());
		user = MercurialUtilities.getDefaultUserName(user);
		command.user(user);
		command.message(message);

		try {
			command.execute();
			HgCommitMessageManager.updateDefaultCommitName(hgRoot, user);
		} finally {
			new RefreshRootJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
			firePostCommitEvent();
		}
	}

	// borrowed and adapted from Subclipse's SVNRemoteStorage.fireResourceStatesChangedEvent()
	protected static void firePostCommitEvent() {
		final Job prevNotify = HgCommitClient.prevNotification;
		Job job = new Job("HgCommitClient notification of listeners that a commit has completed.") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (prevNotify != null) {
					try {
						prevNotify.join();
					} catch (InterruptedException e) {
						return Status.CANCEL_STATUS;
					}
				}

				if (postCommitListeners != null) {
					for (IPostCommitListener listener : postCommitListeners) {
						if (!monitor.isCanceled()) {
							listener.resourceCommitted();
						}
					}
				}
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
		HgCommitClient.prevNotification = job;
	}

	/**
	 * Adds a listener to be notified after the commit operation has completed. Since the
	 * registration for notification does not specify any resource, all listeners will be notified
	 * for each commit, including that of resources that they don't care about.
	 *
	 */
	public static void addPostCommitListener(IPostCommitListener listener) {
		if (postCommitListeners == null) {
			postCommitListeners = new ArrayList<IPostCommitListener>();
		}
		if (!postCommitListeners.contains(listener)) {
			postCommitListeners.add(listener);
		}
	}

	public static void removePostCommitListener(IPostCommitListener listener) {
		if (postCommitListeners == null) {
			return;
		}
		postCommitListeners.remove(listener);
	}
}
