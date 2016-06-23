/*******************************************************************************
 * Copyright (c) 2005-2016 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel VOGLOZIN	Implementation (2016-06-22)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.PathFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * Base class for all "Reassign To" actions. Purpose: code deduplication.
 *
 * @author Amenel VOGLOZIN
 *
 */
public abstract class AbstractReassignToChangesetSynchronizeAction extends PathAwareAction {

	protected final List<IFile> selectedFiles;

	public AbstractReassignToChangesetSynchronizeAction(String text,
			ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider,
			List<IFile> selectedFiles) {
		super(text, configuration, selectionProvider);
		this.selectedFiles = selectedFiles;
	}

	@Override
	protected boolean isSupported(Object object) {
		if (object instanceof UncommittedChangesetGroup) {
			// The contents of the entire Uncommited group cannot be reassigned.
			return false;
		}
		// The contents of a non-empty workspace changeset can be reassigned.
		if (object instanceof WorkingChangeSet) {
			return (((WorkingChangeSet) object).getFiles().size() > 0);
		} else if (object instanceof FileFromChangeSet) {
			// A file can be reassigned if its parent is a workspace changeset.
			FileFromChangeSet file = (FileFromChangeSet) object;
			if (file.getChangeset() instanceof WorkingChangeSet) {
				return true;
			}
		} else if (object instanceof PathFromChangeSet) {
			// A path can be reassigned if its parent is a workspace changeset.
			Object parent = ((PathFromChangeSet) object).getParent();
			if (parent instanceof UncommittedChangesetGroup || parent instanceof WorkingChangeSet) {
				return true;
			}
		}

		// Any other object cannot be reassigned to a changeset.
		return false;
	}

}
