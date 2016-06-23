/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel VOGLOZIN	Implementation + JavaDoc (2016-06-21)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * This class implements basic functionality from bits and pieces copied from the now-child class
 * CommitDialog. This is essentially a product of refactoring so as to avoid code duplication.
 *
 * @author Amenel VOGLOZIN
 *
 */
public abstract class BaseCommitDialog extends TitleAreaDialog {

	protected ISourceViewer commitTextBox;
	protected SourceViewerDecorationSupport decorationSupport;
	protected final IDocument commitTextDocument;
	protected Options options;

	public static final String DEFAULT_COMMIT_MESSAGE = Messages
			.getString("CommitDialog.defaultCommitMessage"); //$NON-NLS-1$

	public static class Options {
		public boolean showDiff = true;
		public boolean showAmend = true;
		public boolean showCloseBranch = true;
		public boolean showRevert = true;
		public boolean showUser = true;
		public boolean filesSelectable = true;
		public String defaultCommitMessage = DEFAULT_COMMIT_MESSAGE;
		public String readyMessageSelector = null;
		public boolean showCommitMessage = true;
		public boolean allowEmptyCommit = false;
		/** optional to use if no files are specified and allowEmptyCommit is true */
		public HgRoot hgRoot = null;
	}

	public BaseCommitDialog(Shell parentShell) {
		super(parentShell);
		//
		options = new Options();
		commitTextDocument = new Document();
	}

	protected void createCommitTextBox(Composite container) {
		if (!options.showCommitMessage) {
			return;
		}

		setMessage(Messages.getString("CommitDialog.commitTextLabel.text"));

		commitTextBox = new SourceViewer(container, null,
				SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
		commitTextBox.setEditable(true);
		commitTextBox.getTextWidget().setLayoutData(SWTWidgetHelper.getFillGD(100));

		// set up spell-check annotations
		decorationSupport = new SourceViewerDecorationSupport(commitTextBox, null,
				new DefaultMarkerAnnotationAccess(), EditorsUI.getSharedTextColors());

		AnnotationPreference pref = EditorsUI.getAnnotationPreferenceLookup()
				.getAnnotationPreference(SpellingAnnotation.TYPE);

		decorationSupport.setAnnotationPreference(pref);
		decorationSupport.install(EditorsUI.getPreferenceStore());

		commitTextBox.configure(new TextSourceViewerConfiguration(EditorsUI.getPreferenceStore()));
		AnnotationModel annotationModel = new AnnotationModel();
		commitTextBox.setDocument(commitTextDocument, annotationModel);
		commitTextBox.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				decorationSupport.uninstall();
			}
		});

		commitTextBox.addTextListener(new ITextListener() {
			public void textChanged(TextEvent event) {
				validateControls();
			}
		});
	}

	/**
	 * Provides the text that the user has entered in the commit message input box.
	 *
	 * @return The text of the user-entered commit message.
	 */
	public String getCommitMessage() {
		return commitTextDocument.get();
	}

	/**
	 * Performs various checks about the user input, which may trigger an error message in the title
	 * area. The implementing class has to handle the enabling and disabling of the OK button.
	 */
	protected abstract void validateControls();

	protected void createOldCommitCombo(Composite container) {
		if (!options.showCommitMessage) {
			return;
		}
		createOldCommitCombo(container, commitTextDocument, commitTextBox);
	}

	/**
	 * Creates a combobox containing the previous commit messages.
	 * <p>
	 * NOTE: The combobox <b>will not</b> be created if there are no previous commit messages.
	 *
	 * @param container
	 *            The parent container.
	 * @param commitTextDocument
	 *            The document of which the contents is displayed in the input box. Used to set the
	 *            comment to an old commit selected from the combobox.
	 * @param commitTextBox
	 *            The input box. Used to have the entire contents selected, so that the user can
	 *            start typing if they don't want to keep that contents.
	 */
	public static void createOldCommitCombo(Composite container, final IDocument commitTextDocument,
			final ISourceViewer commitTextBox) {
		final String[] oldCommits = MercurialEclipsePlugin.getCommitMessageManager()
				.getCommitMessages();
		if (oldCommits.length > 0) {
			final Combo oldCommitComboBox = SWTWidgetHelper.createCombo(container);
			String[] oddCommitsDisplay = new String[oldCommits.length + 1];
			oddCommitsDisplay[0] = Messages.getString("CommitDialog.oldCommitMessages");
			for (int i = 0; i < oldCommits.length; i++) {
				// Add text to the combo but replace \n with <br> to get a one-liner
				String commitText = oldCommits[i].replaceAll("\\n", "<br>");
				// XXX This is a workaround of Bug 209157 of SWT Combo
				if (oddCommitsDisplay.length > 3 || commitText.length() <= 100) {
					oddCommitsDisplay[i + 1] = commitText;
				} else {
					oddCommitsDisplay[i + 1] = commitText.substring(0, 100) + " ...";
				}
			}
			oldCommitComboBox.setItems(oddCommitsDisplay);
			oldCommitComboBox.setText(Messages.getString("CommitDialog.oldCommitMessages"));
			oldCommitComboBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (oldCommitComboBox.getSelectionIndex() != 0) {
						commitTextDocument
								.set(oldCommits[oldCommitComboBox.getSelectionIndex() - 1]);
						commitTextBox.setSelectedRange(0,
								oldCommits[oldCommitComboBox.getSelectionIndex() - 1].length());
					}

				}
			});
		}
	}

}
