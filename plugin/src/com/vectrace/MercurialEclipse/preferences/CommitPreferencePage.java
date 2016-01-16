/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author Amenel VOGLOZIN
 *
 */
public class CommitPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public CommitPreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.getString("CommitPreferencePage.description")); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PREF_COMMIT_UPDATE_QUICKDIFF,
				Messages.getString("CommitPreferencePage.updateQuickDiffAfterCommit"), //$NON-NLS-1$
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(PREF_PRESELECT_UNTRACKED_IN_COMMIT_DIALOG,
				Messages.getString("CommitPreferencePage.preselectUntrackedInCommitDialog"), //$NON-NLS-1$
				getFieldEditorParent()));

		BooleanFieldEditor refresh_editor = new BooleanFieldEditor(PREF_REFRESH_BEFORE_COMMIT,
				Messages.getString("CommitPreferencePage.refreshRepositoryBeforeCommit"), //$NON-NLS-1$
				getFieldEditorParent());
		addField(refresh_editor);

		IntegerFieldEditor commitSizeEditor = new IntegerFieldEditor(COMMIT_MESSAGE_BATCH_SIZE,
				Messages.getString("CommitPreferencePage.field.commitMessageBatchSize"), //$NON-NLS-1$
				getFieldEditorParent());
		commitSizeEditor.setValidRange(1, Integer.MAX_VALUE);
		addField(commitSizeEditor);

	}

}
