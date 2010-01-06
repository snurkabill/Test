/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.GChangeSet.RowCount;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgGLogClient extends HgCommand {
	private final List<GChangeSet> sets = new ArrayList<GChangeSet>();

	public HgGLogClient(IResource resource, int batchSize, int startRev) throws HgException {
		super("glog", false);
		File fileHandle = ResourceUtils.getFileHandle(resource);
		workingDir = ResourceUtils.getFirstExistingDirectory(fileHandle);
		addOptions("--config", "extensions.graphlog="); //$NON-NLS-1$ //$NON-NLS-2$
		addOptions("--template", "*{rev}\\n"); // Removes everything //$NON-NLS-1$ //$NON-NLS-2$

		if(batchSize > 0) {
			addOptions("--limit", String.valueOf(batchSize)); //$NON-NLS-1$
		} else {
			// set very high limit for log/glog when no limit is wanted to override limits set in user's .hgrc
			addOptions("--limit", HgLogClient.NOLIMIT); //$NON-NLS-1$
		}

		if (startRev >= 0 && startRev != Integer.MAX_VALUE) {
			// always advise to follow until 0 revision: the reason is that log limit
			// might be bigger then the difference of two consequent revisions on a specific resource
			addOptions("-r");
			addOptions(startRev + ":" + 0);
		}

		if (resource.getType() != IResource.PROJECT) {
			addOptions(fileHandle.getAbsolutePath());
		} else {
			HgRoot hgRoot = getHgRoot();
			if(!hgRoot.equals(fileHandle)){
				// for multiple projects under same hg root we should return only current project history
				addOptions(fileHandle.getAbsolutePath());
			}
		}
		setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
		load(executeToString());
	}

	private void load(String s) {
		String[] split = s.split("\n"); //$NON-NLS-1$
		// real changeset count as glog inserts a line between two changesets
		int length = split.length / 2;
		int lengthp1 = length;
		RowCount rowCount = new RowCount();
		GChangeSet last = null;
		for (int i = 0; i < lengthp1; i++) {
			// adjust index for spacing
			int changeset = i * 2;
			String afterS = i != length ? split[changeset + 1] : "";
			// add current changeset and next line
			GChangeSet newOne = new GChangeSet(rowCount, i, split[changeset], afterS);
			newOne.clean(last);
			last = newOne;
			sets.add(last);
		}
	}

	public List<GChangeSet> getChangeSets() {
		return sets;
	}

}
