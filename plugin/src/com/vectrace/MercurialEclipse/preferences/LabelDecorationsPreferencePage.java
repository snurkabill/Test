/*******************************************************************************
 * Copyright (c) 2005-2017 VecTrace (Zingo Andersen) and others.
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
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.ResourceDecorator;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Amenel VOGLOZIN
 *
 */
public class LabelDecorationsPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	private StringFieldEditor syntaxField;
	private Label previewStaticField;

	private static final class LabelDecoratorRadioGroupFieldEditor extends RadioGroupFieldEditor {
		private LabelDecoratorRadioGroupFieldEditor(String name, String labelText, int numColumns,
				String[][] labelAndValues, Composite parent, boolean useGroup) {
			super(name, labelText, numColumns, labelAndValues, parent, useGroup);
		}

		@Override
		protected void doStore() {
			super.doStore();
			MercurialEclipsePlugin.getDefault().checkHgInstallation();
			// ResourceDecorator.onConfigurationChanged();
		}
	}

	public LabelDecorationsPreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.getString("LabelDecorationsPreferencePage.description")); //$NON-NLS-1$
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

		Composite parent = getFieldEditorParent();

		//
		createFontAndColorGroup(parent);

		//
		LabelDecoratorRadioGroupFieldEditor editor = new LabelDecoratorRadioGroupFieldEditor(
				LABELDECORATOR_LOGIC,
				Messages.getString("LabelDecorationsPreferencePage.decorationGroup.description"), //$NON-NLS-1$
				1,
				new String[][] {
						{ Messages.getString(
								"LabelDecorationsPreferencePage.decorationGroup.asModified"), //$NON-NLS-1$
								LABELDECORATOR_LOGIC_2MM },
						{ Messages.getString(
								"LabelDecorationsPreferencePage.decorationGroup.mostImportant"), //$NON-NLS-1$
								LABELDECORATOR_LOGIC_HB } },
				parent, true);
		addField(editor);

		//
		createProjectLabelsGroup(parent);

	}

	private static Group createGroup(Composite parent, int numColumns, String text) {
		Group group = new Group(parent, SWT.NULL);
		group.setText(text);
		//
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		group.setLayout(layout);
		//
		// Horizontal and vertical fill, grabbing excess horiz space.
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalIndent = 0;
		// NOTE: I can't figure out why, but a +1 is necessary for correct right alignment with the
		// radio group.
		data.horizontalSpan = numColumns + 1;
		group.setLayoutData(data);
		//
		return group;
	}

	/**
	 * @param parent
	 */
	@SuppressFBWarnings(value="DLS", justification="Voluntary use of variables for holding PreferenceLinkArea objects.")
	private void createFontAndColorGroup(Composite parent) {
		Group fontAndColorGroup = createGroup(parent, 1,
				Messages.getString("LabelDecorationsPreferencePage.fontAndColorGroup.description")); // $NON-NLS-1$

		Composite box = new Composite(fontAndColorGroup, SWT.NONE);
		GridLayout boxLayout = new GridLayout();
		boxLayout.numColumns = 1;
		box.setLayout(boxLayout);

		addField(new BooleanFieldEditor(PREF_DECORATE_WITH_COLORS,
				Messages.getString("LabelDecorationsPreferencePage.enableFontAndColorDecorations"), //$NON-NLS-1$
				box));

		@SuppressWarnings("unused")
		PreferenceLinkArea linkToDecorations = new PreferenceLinkArea(box, SWT.NONE,
				"org.eclipse.ui.preferencePages.Decorators", //$NON-NLS-1$
				Messages.getString("LabelDecorationsPreferencePage.linkToDecorationsPrefPage"), //$NON-NLS-1$
				(IWorkbenchPreferenceContainer) getContainer(), null);

		@SuppressWarnings("unused")
		PreferenceLinkArea linkToFontAndColor = new PreferenceLinkArea(box, SWT.NONE,
				"org.eclipse.ui.preferencePages.ColorsAndFonts", //$NON-NLS-1$
				Messages.getString("LabelDecorationsPreferencePage.linkToFontAndColorPrefPage"), //$NON-NLS-1$
				(IWorkbenchPreferenceContainer) getContainer(), null);
	}

	/**
	 * @param parent
	 */
	private void createProjectLabelsGroup(Composite parent) {
		Group syntaxGroup = createGroup(parent, 1,
				Messages.getString("LabelDecorationsPreferencePage.syntaxGroup.description")); //$NON-NLS-1$

		/*
		 * We use this box to contain all widgets so that the horizontal indentation yields a left
		 * alignment with controls in the radiobuttons group.
		 */
		Composite box = new Composite(syntaxGroup, SWT.NONE);
		GridData boxGd = new GridData(GridData.FILL_HORIZONTAL);
		box.setLayoutData(boxGd);

		addField(new BooleanFieldEditor(RESOURCE_DECORATOR_SHOW_CHANGESET_IN_PROJECT_LABEL,
				Messages.getString("LabelDecorationsPreferencePage.showChangesetInfoOnContainer"), //$NON-NLS-1$
				box));

		addField(new BooleanFieldEditor(PREF_SHOW_LOGICAL_NAME_OF_REPOSITORIES,
				Messages.getString("LabelDecorationsPreferencePage.showLogicalNameOfRepositories"), //$NON-NLS-1$
				box));

		/*
		 * Contains the label and text editor on the first row, then the preview label and text on
		 * the second row, then the Reset button on the third one.
		 */
		Composite syntaxEditComposite = SWTWidgetHelper.createComposite(box, 2);
		GridData secGd = (GridData) syntaxEditComposite.getLayoutData();
		secGd.grabExcessHorizontalSpace = true;

		syntaxField = new StringFieldEditor(PREF_DECORATE_PROJECT_LABEL_SYNTAX,
				Messages.getString("LabelDecorationsPreferencePage.syntaxGroup.syntax"), //$NON-NLS-1$
				syntaxEditComposite) {
			/**
			 * @see org.eclipse.jface.preference.FieldEditor#fireValueChanged(java.lang.String,
			 *      java.lang.Object, java.lang.Object)
			 */
			@Override
			protected void fireValueChanged(String property, Object oldVal, Object newVal) {
				super.fireValueChanged(property, oldVal, newVal);
				updatePreview();
			}

			@Override
			protected void doLoadDefault() {
				super.doLoadDefault();
				updatePreview();
			}
		};
		addField(syntaxField);

		@SuppressWarnings("unused")
		Label previewTitle = SWTWidgetHelper.createLabel(syntaxEditComposite, "Preview");
		previewStaticField = SWTWidgetHelper.createLabel(syntaxEditComposite,
				"Example preview text");

		Button resetBtn = SWTWidgetHelper.createPushButton(box,
				Messages.getString("LabelDecorationsPreferencePage.syntaxGroup.reset"), 2); //$NON-NLS-1$
		resetBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				syntaxField.setStringValue(HgDecoratorConstants.DEFAULT_PROJECT_SYNTAX);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				syntaxField.setStringValue(HgDecoratorConstants.DEFAULT_PROJECT_SYNTAX);
			}
		});
		// Align the button to the right.
		GridData gd = (GridData) resetBtn.getLayoutData();
		gd.horizontalAlignment = SWT.RIGHT;
	}

	private void updatePreview() {
		String previewUserSyntax = syntaxField.getStringValue();
		previewStaticField.setText(ResourceDecorator.previewProjectLabel(previewUserSyntax));
	}

	/**
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#initialize()
	 */
	@Override
	protected void initialize() {
		super.initialize();
		updatePreview();
	}

}
