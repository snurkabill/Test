/*******************************************************************************
 * Copyright (c) 2005-2017 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation (2017-02-15)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Simple informational non-modal dialog for letting the user know about files that have been
 * "auto-resolved", that is files upon which Mercurial's pre-merge algorithm has acted and which
 * ended in a "resolved" state.
 * <p>
 * This dialog simply presents a list of those files, an informational text and a close button.
 *
 * @author Amenel Voglozin
 *
 */
public class AutoresolveRecapDialog extends TitleAreaDialog {

	private final List<IResource> resources;
	final private boolean allConflictsResolved;

	/**
	 * @param parentShell
	 * @param resources
	 *            the files to display in the file list
	 */
	public AutoresolveRecapDialog(Shell parentShell, List<IResource> resources,
			boolean conflictsResolved) {
		super(parentShell);
		this.resources = resources;
		this.allConflictsResolved = conflictsResolved;
		setHelpAvailable(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		super.createDialogArea(parent);
		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		GridData gd = SWTWidgetHelper.getFillGD(400);
		gd.minimumWidth = 500;
		container.setLayoutData(gd);

		CommitFilesChooser fileList = new CommitFilesChooser(container, false, resources, true,
				true, true);
		fileList.setLayoutData(new GridData(GridData.FILL_BOTH));

		if (!allConflictsResolved) {
			SWTWidgetHelper.createLabel(container,
					Messages.getString("AutoresolveSummaryDialog.conflictsExist")); //$NON-NLS-1$
		}

		getShell().setText(Messages.getString("AutoresolveSummaryDialog.windowTitle")); //$NON-NLS-1$
		setTitle(Messages.getString("AutoresolveSummaryDialog.title")); //$NON-NLS-1$

		if (resources.size() == 0) {
			setMessage(Messages.getString("AutoresolveSummaryDialog.noResourcesResolved"), //$NON-NLS-1$
					IMessageProvider.WARNING);
		} else {
			setMessage(Messages.getString("AutoresolveSummaryDialog.message"), //$NON-NLS-1$
					IMessageProvider.INFORMATION);
		}

		return container;
	}

	/**
	 * We make the window non-modal.
	 *
	 * @see org.eclipse.jface.window.Window#setShellStyle(int)
	 */
	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
	}

	/**
	 * Override this method because this is an informational dialog. We only need one button.
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

}
