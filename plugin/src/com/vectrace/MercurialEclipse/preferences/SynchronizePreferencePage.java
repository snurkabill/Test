/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov		 - implementation
 *     Soren Mathiasen		 - synchronize view options
 *     Amenel VOGLOZIN		 - Changeset context menu option
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class SynchronizePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private BooleanFieldEditor enableChangesetsContextMenu;

	public SynchronizePreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.getString("SynchronizePreferencePage.description")); //$NON-NLS-1$
	}

	public void init(IWorkbench workbench) {
		// noop
	}

	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PREF_SYNC_ONLY_CURRENT_BRANCH,
				Messages.getString("SynchronizePreferencePage.syncOnlyCurrentBranch"), //$NON-NLS-1$
				getFieldEditorParent()));

		// This field, upon being unchecked, will disable the context menu field.
		final BooleanFieldEditor enableLocalChangesets = new BooleanFieldEditor(
				PREF_SYNC_ENABLE_LOCAL_CHANGESETS,
				Messages.getString("SynchronizePreferencePage.syncEnableLocalChangeSets"), //$NON-NLS-1$
				getFieldEditorParent()) {
			@Override
			protected void fireStateChanged(String property, boolean oldValue, boolean newValue) {
				super.fireStateChanged(property, oldValue, newValue);
				if (oldValue != newValue) {
					enableChangesetsContextMenu.setEnabled(getBooleanValue(),
							getFieldEditorParent());
				}
			}
		};
		addField(enableLocalChangesets);

		enableChangesetsContextMenu = new BooleanFieldEditor(
				PREF_SYNC_ENABLE_LOCAL_CHANGESETS_CONTEXT_MENU,
				Messages.getString(
						"SynchronizePreferencePage.syncEnableLocalChangeSetsContextMenu"), //$NON-NLS-1$
				getFieldEditorParent());
		addField(enableChangesetsContextMenu);

		addField(new BooleanFieldEditor(PREF_SYNC_ALL_PROJECTS_IN_REPO,
				Messages.getString("SynchronizePreferencePage.syncAllProjectsInRepo"), //$NON-NLS-1$
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(PREF_SYNC_SHOW_EMPTY_GROUPS,
				Messages.getString("SynchronizePreferencePage.showEmptyGroups"), //$NON-NLS-1$
				getFieldEditorParent()));
	}

}