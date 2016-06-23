/*******************************************************************************
 * Copyright (c) 2005-2016 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Amenel VOGLOZIN		Implementation (2016-06-18)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Dialog in which the user can enter a name and a comment for a to-be-created changeset.
 * <p>
 * After the user clicks OK, a new changeset is created and the resources that were selected prior
 * to the opening of the context menu are all reassigned to the new changeset.
 */
public class ReassignToNewChangesetDialog extends BaseCommitDialog {

	private final List<IFile> selectedFiles;

	private Text changesetNameText;
	private Button useNameAsCommentButton;
	private Button useUserCommentButton;
	private Composite commitMessageGroup;

	private final UncommittedChangesetGroup group;

	public ReassignToNewChangesetDialog(Shell shell, List<IFile> selectedFiles,
			UncommittedChangesetGroup group) {
		super(shell);
		//
		this.selectedFiles = selectedFiles;
		this.group = group;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.TITLE);
		setBlockOnOpen(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = SWTWidgetHelper.createComposite(parent, 1);
		GridData gd = SWTWidgetHelper.getFillGD(300);
		container.setLayoutData(gd);
		super.createDialogArea(parent);

		createChangesetNameContainer(container);
		createCommentSelectionButtons(container);
		createCommitMessageGroup(container);

		changesetNameText.setText(Messages.getString("ReassignToNewChangesetDialog.newChangeset")); //$NON-NLS-1$
		commitTextDocument.set(Messages.getString("ReassignToNewChangesetDialog.newChangeset")); //$NON-NLS-1$

		useNameAsCommentButton.setSelection(true);
		useUserCommentButton.setSelection(false);
		commitMessageGroup.setEnabled(false);
		changesetNameText.setFocus();
		changesetNameText.selectAll();

		getShell().setText(Messages.getString("ReassignToNewChangesetDialog.windowTitle")); //$NON-NLS-1$
		setTitle(Messages.getString("ReassignToNewChangesetDialog.title")); //$NON-NLS-1$
		setMessage(Messages.getString("ReassignToNewChangesetDialog.message")); //$NON-NLS-1$

		return container;
	}

	/**
	 * Creates the commit message group, which contains both the commit text box and the Previous
	 * commit messages combobox.
	 *
	 * @param container
	 *            The parent container.
	 */
	private void createCommitMessageGroup(Composite container) {
		commitMessageGroup = SWTWidgetHelper.createGroup(container,
				Messages.getString("ReassignToNewChangesetDialog.commentGroupLabel"), 1, //$NON-NLS-1$
				GridData.FILL_HORIZONTAL);
		GridData gd = SWTWidgetHelper.getFillGD(150);
		commitMessageGroup.setLayoutData(gd);
		//
		createCommitTextBox(commitMessageGroup);
//		commitTextBox.getTextWidget().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createOldCommitCombo(commitMessageGroup);
	}

	/**
	 * "Changeset name" label and textbox.
	 *
	 * @param container
	 *            The parent container.
	 */
	private void createChangesetNameContainer(Composite container) {
		Composite comp = SWTWidgetHelper.createComposite(container, 2);
		comp.moveAbove(container.getChildren()[0]);

		SWTWidgetHelper.createLabel(comp,
				Messages.getString("ReassignToNewChangesetDialog.changesetName")); //$NON-NLS-1$
		changesetNameText = SWTWidgetHelper.createTextField(comp);
		changesetNameText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				validateControls();
			}
		});
	}

	/**
	 * Creates the two radiobuttons and adds the appropriate actions when each button is selected.
	 *
	 * @param container
	 *            The parent container.
	 */
	private void createCommentSelectionButtons(Composite container) {
		Composite comp = SWTWidgetHelper.createComposite(container, 1);
		useNameAsCommentButton = SWTWidgetHelper.createRadioButton(comp,
				Messages.getString("ReassignToNewChangesetDialog.useChangesetName"), 1); //$NON-NLS-1$

		// Selecting this button causes the changeset name to be selected as a whole and the commit
		// message to appear deselected.
		useNameAsCommentButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				changesetNameText.setFocus();
				changesetNameText.selectAll();
				commitTextBox.setSelectedRange(getCommitMessage().length(), 0);
				commitMessageGroup.setEnabled(false);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// Selecting this button causes the comment group to be activated and the text to be
		// selected.
		useUserCommentButton = SWTWidgetHelper.createRadioButton(comp,
				Messages.getString("ReassignToNewChangesetDialog.useComment"), 1); //$NON-NLS-1$
		useUserCommentButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				commitMessageGroup.setEnabled(true);
				commitTextBox.getTextWidget().setFocus();
				commitTextBox.getTextWidget().selectAll();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	/**
	 *
	 * @see com.vectrace.MercurialEclipse.dialogs.BaseCommitDialog#validateControls()
	 */
	@Override
	protected void validateControls() {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton == null) {
			// This method may be called before the dialog actually opens. The purpose of this check
			// is to avoid a silent NPE which the user won't know anything about anyway.
			return;
		}
		if (useNameAsCommentButton.getSelection() && changesetNameText.getText().isEmpty()) {
			setErrorMessage(
					Messages.getString("ReassignToNewChangesetDialog.changesetNameRequired")); //$NON-NLS-1$
			okButton.setEnabled(false);
		} else if (!useNameAsCommentButton.getSelection() && getCommitMessage().isEmpty()) {
			setErrorMessage(
					Messages.getString("ReassignToNewChangesetDialog.commitMessageRequired")); //$NON-NLS-1$
			okButton.setEnabled(false);
		} else {
			okButton.setEnabled(true);
			setErrorMessage(null);
		}
	}

	/**
	 * Creates the new changeset with the files that were selected before the context menu opened,
	 * then closes the dialog.
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		WorkingChangeSet newCs = group.create(selectedFiles.toArray(new IFile[0]));
		newCs.setName(changesetNameText.getText());
		newCs.setComment(useNameAsCommentButton.getSelection() ? changesetNameText.getText()
				: getCommitMessage());

		super.okPressed();
	}

}
