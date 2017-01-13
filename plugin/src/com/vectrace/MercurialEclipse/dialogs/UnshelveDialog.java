/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Amenel Voglozin - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

public class UnshelveDialog extends TitleAreaDialog {

	/**
	 * Whether the workspace contains uncommitted changes. In such a case, the Unshelve will not be
	 * allowed (i.e. the OK button will not be enabled) unless the user ticks the right checkbox.
	 */
	private final boolean isWorkspaceDirty;

	private boolean abort = false;
	private boolean cont = false;
	private boolean keep = false;

	private Button abortCheckbox;
	private Button continueCheckbox;
	private Button keepCheckbox;

	/**
	 * Checkbox to compel the user to validate a "Force unshelve" when the workspace is known to be
	 * dirty. The "Force unshelve" checkbox is <u>ONLY</u> created when the workspace is dirty. Any
	 * access to this field must be guarded by a check of the workspace dirtiness status.
	 */
	private Button forceCheckbox = null;

	/**
	 * @param parentShell
	 */
	public UnshelveDialog(Shell parentShell, boolean dirty) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.isWorkspaceDirty = dirty;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings dialogSettings = MercurialEclipsePlugin.getDefault().getDialogSettings();
		String sectionName = getClass().getSimpleName();
		IDialogSettings section = dialogSettings.getSection(sectionName);
		if (section == null) {
			dialogSettings.addNewSection(sectionName);
		}
		return section;
	}

	@Override
	public void create() {
		super.create();
		getShell().setText(Messages.getString("UnshelveDialog.window.title")); //$NON-NLS-1$
		setTitle(Messages.getString("UnshelveDialog.title")); //$NON-NLS-1$
		setMessage(Messages.getString("UnshelveDialog.message")); //$NON-NLS-1$
	}

	/**
	 * Create the contents of the dialog.
	 *
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		GridData gd = SWTWidgetHelper.getFillGD(200);
		container.setLayoutData(gd);
		super.createDialogArea(parent);

		createControls(container);

		return container;
	}

	/**
	 * Overriding in order to disable the OK button if relevant.
	 *
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control res = super.createContents(parent);
		//
		// This is solely so that the OK button can be disabled when the dialog opens while there
		// are outstanding changes.
		validateControls();

		return res;
	}

	/**
	 * @param container
	 */
	private void createControls(Composite container) {
		abortCheckbox = SWTWidgetHelper.createCheckBox(container,
				Messages.getString("UnshelveDialog.checkbox.abort")); //$NON-NLS-1$
		continueCheckbox = SWTWidgetHelper.createCheckBox(container,
				Messages.getString("UnshelveDialog.checkbox.continue")); //$NON-NLS-1$
		keepCheckbox = SWTWidgetHelper.createCheckBox(container,
				Messages.getString("UnshelveDialog.checkbox.keep")); //$NON-NLS-1$
		if (this.isWorkspaceDirty) {
			createForceSection(container);
		}
	}

	/**
	 * Creates the part of the dialog that contains the Force Unshelve checkbox.
	 *
	 * @param container
	 */
	private void createForceSection(Composite container) {
		Group group = SWTWidgetHelper.createGroup(container,
				Messages.getString("UnshelveDialog.force.group")); //$NON-NLS-1$
		//
		// Checkbox
		forceCheckbox = SWTWidgetHelper.createCheckBox(group,
				Messages.getString("UnshelveDialog.force.checkbox")); //$NON-NLS-1$
		// The listener is for setting/unsetting the error message each time the box is checked or
		// unchecked.
		forceCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				validateControls();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	@Override
	protected void okPressed() {
		abort = abortCheckbox.getSelection();
		cont = continueCheckbox.getSelection();
		keep = keepCheckbox.getSelection();

		super.okPressed();
	}

	/**
	 * Checks the consistency of the user input, which may trigger an error message in the title
	 * area. In this dialog, a dirty workspace will require the user to manually tick the "Force
	 * unshelve" checkbox.
	 */
	protected void validateControls() {
		if (this.isWorkspaceDirty) {
			if (!forceCheckbox.getSelection()) {
				setErrorMessage(Messages.getString("UnshelveDialog.force.errorMessage")); //$NON-NLS-1$
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				setErrorMessage(null);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		}
	}

	public boolean getAbort() {
		return abort;
	}

	public boolean getContinue() {
		return cont;
	}

	public boolean getKeep() {
		return keep;
	}
}
