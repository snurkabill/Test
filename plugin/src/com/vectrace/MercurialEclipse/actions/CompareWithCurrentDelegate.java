/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgWorkspaceFile;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * This delegate is used for object contribution to context menus.
 * Object class must be FileFromChangeSet or be adaptable to it.
 */
public class CompareWithCurrentDelegate implements IObjectActionDelegate {

	private FileFromChangeSet fileFromChangeSet;

	public void run(IAction action) {
		if (fileFromChangeSet == null) {
			return;
		}
		ChangeSet cs = fileFromChangeSet.getChangeset();
		HgWorkspaceFile left = HgWorkspaceFile.make(fileFromChangeSet.getFile());

		Assert.isTrue(cs instanceof JHgChangeSet);

		HgFile right = HgFile.make((JHgChangeSet) cs, fileFromChangeSet.getFile());

		CompareUtils.openEditor(left, right, false, null);

	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection newSelection) {
		action.setEnabled(false);
		if (newSelection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) newSelection;
			if (sel.getFirstElement() instanceof FileFromChangeSet) {
				this.fileFromChangeSet = (FileFromChangeSet) sel.getFirstElement();

				int diffKind = fileFromChangeSet.getDiffKind();
				if ((diffKind & Differencer.CHANGE_TYPE_MASK) != Differencer.DELETION
						&& fileFromChangeSet.getFile().exists()) {
					action.setEnabled(true);
				}
			}
		} else {
			this.fileFromChangeSet = null;
		}
	}

	/**
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}
}
