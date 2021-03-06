/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     StefanC                   - some updates, code cleanup
 *     Stefan Groschupf          - logError
 *     Subclipse project committers - reference
 *     Charles O'Farrell         - comparison diff
 *     Andrei Loskutov           - bug fixes
 *     Ilya Ivanov (Intland)     - modifications
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.history.RevisionAnnotationController;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.osgi.framework.Version;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.ExportAsBundleAction;
import com.vectrace.MercurialEclipse.actions.MergeWithCurrentChangesetAction;
import com.vectrace.MercurialEclipse.actions.OpenMercurialRevisionAction;
import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.HistoryContentProposalProvider.RevisionContentProposal;
import com.vectrace.MercurialEclipse.menu.StripHandler;
import com.vectrace.MercurialEclipse.menu.UpdateJob;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.team.ActionRevert;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.BackoutWizard;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class MercurialHistoryPage extends HistoryPage {

	private static final boolean ECLISPE_BEFORE_38;

	private static final boolean ECLISPE_BEFORE_431;

	static {
		Version ver = Platform.getBundle("org.eclipse.ui.ide").getVersion();
		ECLISPE_BEFORE_431 = HgFeatures.doCompare(ver, new Version(4, 3, 1)) < 0;
		ECLISPE_BEFORE_38 = HgFeatures.doCompare(ver, new Version(3, 8, 0)) < 0;
	}

	// attributes

	private boolean showTags;
	/**
	 * Whether or not the 'Show Graph' option is currently enabled
	 */
	private boolean showGraph;
	protected boolean showGoTo;

	IResource resource;
	private HgRoot hgRoot;

	private ChangeSet currentWorkdirChangeset;
	private MercurialHistory mercurialHistory;
	private HistoryContentProposalProvider proposalProvider;
	private RevisionAnnotationController rulerSelectionListener;
	private Job refreshFileHistoryJob;
	private Job fetchAllJob;

	// .. actions

	private OpenMercurialRevisionAction openAction;
	private BaseSelectionListenerAction openEditorAction;
	private BaseSelectionListenerAction focusOnSelectedFileAction;
	private CompareRevisionAction compareWithCurrAction;
	private CompareRevisionAction compareWithPrevAction;
	private CompareRevisionAction compareWithOtherAction;
	private CompareRevisionAction compareTwo;
	private BaseSelectionListenerAction revertAction;
	private Action actionShowParentHistory;
	private final IAction bisectMarkGoodAction = new BisectMarkGoodAction(this);
	private final IAction bisectMarkBadAction = new BisectMarkBadAction(this);
	private final IAction bisectResetAction = new BisectResetAction(this);
	private final IAction exportAsBundleAction = new ExportAsBundleAction(this);
	private final IAction mergeWithCurrentChangesetAction = new MergeWithCurrentChangesetAction(this);
	private Action stripAction;
	private Action backoutAction;

	// .. controls

	private ChangedPathsPage changedPaths;
	private GraphLogTableViewer viewer;
	private Text gotoText;
	private Composite rootControl;
	private Composite gotoPanel;


	//
	private TableLayout layout = null;
	/**
	 * Whether or not the graph column was hidden the last time the table data was updated
	 */
	private boolean graphColumnHidden = false;

	/** Number of columns in the History view.*/
	private static final int NUMBER_OF_COLUMNS = 7;

	/**
	 * Action which is related to the selected file
	 */
	private abstract class BaseFileHistoryAction extends BaseSelectionListenerAction {
		protected IFile file;

		private BaseFileHistoryAction(String text) {
			super(text);
		}

		@Override
		protected boolean updateSelection(IStructuredSelection selection) {
			Object element = selection.getFirstElement();
			if(element instanceof MercurialHistory){
				MercurialHistory history = (MercurialHistory) element;
				IFileRevision[] revisions = history.getFileRevisions();
				if(revisions.length != 1 || !(revisions[0] instanceof MercurialRevision)){
					file = null;
					return false;
				}
				MercurialRevision rev = (MercurialRevision) revisions[0];
				if(rev.getResource() instanceof IFile){
					file = (IFile) rev.getResource();
					return file.exists();
				}
			} else if (element instanceof MercurialRevision){
				MercurialRevision rev = (MercurialRevision) element;
				if(rev.getResource() instanceof IFile){
					file = (IFile) rev.getResource();
					return file.exists();
				}
			}
			if(resource instanceof IFile){
				file = (IFile) resource;
				return file.exists();
			}
			file = null;
			return false;
		}
	}

	private final class FetchEntireHistoryJob extends Job {

		private FetchEntireHistoryJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			int from = mercurialHistory.getLastVersion() - 1;
			boolean gotEverything = historyFetched(from);
			while(!gotEverything && !monitor.isCanceled()) {
				try {
					mercurialHistory.load(monitor, from);
				} catch (CoreException ex) {
					MercurialEclipsePlugin.logError(ex);
				}
				from = mercurialHistory.getLastVersion() - 1;
				gotEverything = historyFetched(from);
				updateUI(gotEverything);
			}
			return Status.OK_STATUS;
		}

		private boolean historyFetched(int from) {
			return from == mercurialHistory.getLastRequestedVersion() || from < 0;
		}

		private void updateUI(final boolean gotEverything) {
			final Control ctrl = viewer.getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				ctrl.getDisplay().syncExec(new Runnable() {
					public void run() {
						if (!ctrl.isDisposed()) {
							viewer.setInput(mercurialHistory);
							viewer.refresh();
							// refresh the proposal list with new data.
							// code below works only if the gotoText is not empty
							Listener[] listeners2 = gotoText.getListeners(SWT.KeyDown);
							for (Listener listener : listeners2) {
								Event event = new Event();
								event.type = SWT.KeyDown;
								event.keyCode = SWT.ARROW_RIGHT;
								event.widget = gotoText;
								listener.handleEvent(event);
							}
							// remove the workaround after we've sent the event
							if(gotEverything && gotoText.getText().equals(" ")) {
								gotoText.setText("");
							}
						}
					}
				});
			}
		}
	}

	class LoadMercurialHistoryJob extends Job {
		private final int from;

		public LoadMercurialHistoryJob(int from) {
			super("Retrieving Mercurial revisions..."); //$NON-NLS-1$
			this.from = from;
			setRule(new ExclusiveHistoryRule());
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			if (mercurialHistory == null) {
				return Status.OK_STATUS;
			}

			mercurialHistory.setEnableExtraTags(showTags);
			mercurialHistory.setEnableRevisionGraph(showGraph);
			try {
				mercurialHistory.load(monitor, from);
				if(resource != null) {
					currentWorkdirChangeset = LocalChangesetCache.getInstance().getCurrentChangeSet(resource);
				} else {
					currentWorkdirChangeset = LocalChangesetCache.getInstance().getCurrentChangeSet(hgRoot);
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return e.getStatus();
			}

			final Runnable runnable = new Runnable() {
				public void run() {
					clearSelection();

					TableColumn graphColumn = viewer.getTable().getColumn(0);

					//Hide or show the graph column if the user has toggled it
					if (showGraph && graphColumnHidden) {
						graphColumnHidden = false;

						int[] preferredColumnWidths = getColumnWidthsFromPrefsIfEnabled();
						int prefGraphWidth = preferredColumnWidths == null ? 50 : preferredColumnWidths[0];

						graphColumn.setWidth(prefGraphWidth);
						graphColumn.setResizable(true);
					} else if (!showGraph && !graphColumnHidden){
						graphColumnHidden = true;

						graphColumn.setWidth(0);
						graphColumn.setResizable(false);
					}

					viewer.setInput(mercurialHistory);
					viewer.refresh();
				}
			};

			// Internal code copied here from Utils.asyncExec
			if (viewer == null) {
				return Status.OK_STATUS;
			}

			final Control ctrl = viewer.getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				ctrl.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (!ctrl.isDisposed()) {
							BusyIndicator.showWhile(ctrl.getDisplay(),
									runnable);
						}
					}
				});
			}
			return Status.OK_STATUS;
		}
	}

	static class ChangeLogContentProvider implements IStructuredContentProvider {
		private IFileRevision[] entries;
		public ChangeLogContentProvider() {
			super();
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			entries = null;
		}

		public void dispose() {
			entries = null;
		}

		public Object[] getElements(Object parent) {
			if (entries != null) {
				return entries;
			}

			final IFileHistory fileHistory = (IFileHistory) parent;
			entries = fileHistory.getFileRevisions();
			return entries;
		}
	}

	static class ChangeSetLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public String getColumnText(Object obj, int index) {
			String ret;

			if (!(obj instanceof MercurialRevision)) {
				return "Type Error"; //$NON-NLS-1$
			}

			MercurialRevision revision = (MercurialRevision) obj;
			ChangeSet changeSet = revision.getChangeSet();

			switch (index) {
			case 1:
				ret = changeSet.toString();
				break;
			case 2:
				ret = revision.getTagsString();
				break;
			case 3:
				ret = changeSet.getBranch();
				break;
			case 4:
				ret = changeSet.getAuthor();
				break;
			case 5:
				ret = changeSet.getDateString();
				break;
			case 6:
				ret = changeSet.getSummary();
				break;
			default:
				ret = null;
				break;
			}
			return ret;
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

	public MercurialHistoryPage() {
		super();
	}

	public MercurialHistory getMercurialHistory() {
		return mercurialHistory;
	}

	@Override
	public boolean setInput(Object object) {
		if (!isValidInput(object)) {
			return false;
		}
		if(object instanceof HgRoot){
			actionShowParentHistory.setEnabled(false);
			HgRoot old = hgRoot;
			this.hgRoot = (HgRoot) object;
			super.setInput(object);
			if(hgRoot == null || (hgRoot != null && !hgRoot.equals(old))){
				if(hgRoot != null) {
					mercurialHistory = new MercurialHistory(hgRoot);
				} else {
					mercurialHistory = null;
				}
				if (!ECLISPE_BEFORE_431) {
					linkWithEditor();
				}
				refresh();
			}
			return true;
		}
		IResource old = resource;
		this.resource = MercurialEclipsePlugin.getAdapter(object, IResource.class);
		super.setInput(object);
		if(resource == null || (resource != null && !resource.equals(old))){
			if(resource != null) {
				mercurialHistory = new MercurialHistory(resource);
				actionShowParentHistory.setEnabled(!mercurialHistory.isRootHistory());
			} else {
				mercurialHistory = null;
			}
			if (!ECLISPE_BEFORE_431) {
				linkWithEditor();
			}
			refresh();
		}
		return true;
	}

	@Override
	public boolean inputSet() {
		return true;
	}

	@Override
	public void createControl(Composite parent) {
		IActionBars actionBars = getHistoryPageSite().getWorkbenchPageSite().getActionBars();
		IMenuManager actionBarsMenu = actionBars.getMenuManager();

		final IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		showTags = store.getBoolean(PREF_SHOW_ALL_TAGS);

		Action toggleShowTags = new Action(Messages.getString("HistoryView.showTags"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("actions/tag.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				showTags = isChecked();
				store.setValue(PREF_SHOW_ALL_TAGS, showTags);
				if(mercurialHistory != null) {
					refresh();
				}
			}
		};
		toggleShowTags.setChecked(showTags);
		actionBarsMenu.add(toggleShowTags);

		showGraph = store.getBoolean(PREF_SHOW_GRAPH);

		Action toggleShowGraph = new Action(Messages.getString("HistoryView.showGraph"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("actions/branch.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				showGraph = isChecked();
				store.setValue(PREF_SHOW_GRAPH, showGraph);
				if(mercurialHistory != null) {
					refresh();
				}
			}
		};
		toggleShowGraph.setChecked(showGraph);
		actionBarsMenu.add(toggleShowGraph);

		showGoTo = store.getBoolean(PREF_SHOW_GOTO_TEXT);
		Action toggleGotoText = new Action("Show 'Go To' Panel", //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("actions/goto.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				showGoTo = isChecked();
				store.setValue(PREF_SHOW_GOTO_TEXT, showGoTo);
				if(mercurialHistory != null) {
					GridData gd = (GridData) gotoPanel.getLayoutData();
					gd.exclude = !showGoTo;
					gotoPanel.setVisible(showGoTo);
					rootControl.layout(false);
					changedPaths.refreshLayout();
				}
			}
		};
		toggleGotoText.setChecked(showGoTo);
		actionBarsMenu.add(toggleGotoText);

		actionShowParentHistory = new Action("Show Parent History", //$NON-NLS-1$
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_UP)) {
			@Override
			public void run() {
				if(mercurialHistory == null || hgRoot != null || resource == null) {
					setEnabled(false);
					return;
				}
				if(resource instanceof IProject){
					HgRoot root = MercurialTeamProvider.getHgRoot(resource);
					if(root != null){
						getHistoryView().showHistoryFor(root, true);
					} else {
						setEnabled(false);
					}
				} else {
					IContainer parentRes = resource.getParent();
					if (parentRes instanceof IFolder || parentRes instanceof IProject) {
						getHistoryView().showHistoryFor(parentRes, true);
					} else {
						setEnabled(false);
					}
				}
			}
		};

		IToolBarManager tbm = actionBars.getToolBarManager();
		tbm.add(new Separator());
		tbm.add(toggleShowTags);
		tbm.add(toggleShowGraph);
		tbm.add(actionShowParentHistory);
		tbm.add(new Separator());
		tbm.add(toggleGotoText);

		rootControl = createComposite(parent);
		createGotoText(rootControl);
		changedPaths = new ChangedPathsPage(this, rootControl);
		createTableHistory(changedPaths.getControl());
		changedPaths.createControl();
		setSelectionProvider(viewer);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), new Action() {
			@Override
			public void run() {
				copyToClipboard();
			}
		});
	}

	private static Composite createComposite(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginLeft = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 1;
		gridLayout.verticalSpacing = 1;
		root.setLayout(gridLayout);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = SWT.DEFAULT;
		gridData.heightHint = SWT.DEFAULT;
		root.setLayoutData(gridData);

		return root;
	}

	private void createGotoText(Composite parent) {
		String tooltipForGoTo = "Type version, tag, branch, author or date \n" +
		"+ <Enter> to jump directly to the right version.\n" +
		"Use <Ctrl + Space> to get list of proposals.\n" +
		"Use <Esc> to stop retrieving history for big repositories.";
		gotoPanel = new Composite(parent, SWT.NONE);
		gotoPanel.setToolTipText(tooltipForGoTo);
		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginLeft = 0;
		gridLayout.marginBottom = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = 0;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = 1;
		gridLayout.verticalSpacing = 1;
		gotoPanel.setLayout(gridLayout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.exclude = !showGoTo;
		gotoPanel.setLayoutData(gd);
		gotoPanel.setVisible(showGoTo);
		gotoText = new Text(gotoPanel, SWT.SINGLE | SWT.BORDER | SWT.SEARCH
				| SWT.ICON_CANCEL);
		gotoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		gotoText.setToolTipText(tooltipForGoTo);
		gotoText.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				fetchEntireHistory(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
				fetchEntireHistory(false);
			}
		});

		gotoText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.keyCode == SWT.ESC) {
					fetchEntireHistory(false);
					return;
				}
				if(e.keyCode != SWT.CR) {
					return;
				}
				// try to retrieve right revision and if it's there, select it
				String text = gotoText.getText();
				guessAndSelectVersion(text);
			}
		});

		// hack is needed to make the text widget content length > 0, which allows us
		// to trigger the history retrieving as soon as content assist opens
		gotoText.setText(" ");

		Button gotoButton = new Button(gotoPanel, 0);
		gotoButton.setImage(MercurialEclipsePlugin.getImage("actions/goto.gif"));
		gotoButton.setToolTipText(tooltipForGoTo);
		gotoButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				fetchEntireHistory(true);
				guessAndSelectVersion(gotoText.getText());
			}
		});

		setupRevisionFieldAssistance();
	}

	/**
	 * @return an array of length NUMBER_OF_COLUMNS containing the user's preferred
	 * column widths or null if they have not been set or if the column width persistence
	 * pref (PREF_HISTORY_COLUMN_PERSISTENCE) is not enabled.
	 */
	private static int[] getColumnWidthsFromPrefsIfEnabled() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();

		if (store.getBoolean(PREF_HISTORY_COLUMN_PERSISTENCE) == false) {
			// Column width persistence pref is disabled
			return null;
		}

		String rawWidths = store.getString(PREF_HISTORY_COLUMN_WIDTHS);

		if (rawWidths == null || rawWidths.trim().length() == 0) {
			// No preferred widths have been recorded yet
			return null;
		}

		String[] rawWidthsSplit = rawWidths.split(",");
		int[] parsedWidths = new int[NUMBER_OF_COLUMNS];

		int colIdx = 0;
		while (colIdx < Math.min(rawWidthsSplit.length, NUMBER_OF_COLUMNS)) {
			parsedWidths[colIdx] = Integer.valueOf(rawWidthsSplit[colIdx]);
			colIdx++;
		}

		// This accounts for possible changes in the number of columns.
		if (colIdx < NUMBER_OF_COLUMNS) {
			// The number of columns has been increased. We set default widths and let the
			// user resize as they like.
			for (;colIdx < NUMBER_OF_COLUMNS; colIdx++) {
				parsedWidths[colIdx] = 50;
			}
		} else {
			// Nothing to do: either the number is unchanged or it is lower. We're covered
			// in both cases, meaning that the appropriate number of column data is created.
		}

		return parsedWidths;
	}

	private void createTableHistory(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout0 = new GridLayout();
		layout0.marginHeight = 0;
		layout0.marginWidth = 0;
		composite.setLayout(layout0);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		composite.setLayoutData(data);

		viewer = new GraphLogTableViewer(composite, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.VIRTUAL, this);
		Table changeLogTable = viewer.getTable();

		changeLogTable.setLinesVisible(true);
		changeLogTable.setHeaderVisible(true);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		changeLogTable.setLayoutData(gridData);

		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		if (layout == null) {
			layout = new TableLayout();

			// Get the user-defined column widths from the preferences.
			int[] preferredWidths = getColumnWidthsFromPrefsIfEnabled();

			// We set default widths when there's no widths in the preferences or the user is not
			// interested in having their custom widths saved.
			if (preferredWidths == null) {
				layout.addColumnData(new ColumnWeightData(7, true));
				layout.addColumnData(new ColumnWeightData(15, true));
				layout.addColumnData(new ColumnWeightData(10, true));
				layout.addColumnData(new ColumnWeightData(10, true));
				layout.addColumnData(new ColumnWeightData(12, true));
				layout.addColumnData(new ColumnWeightData(12, true));
				layout.addColumnData(new ColumnWeightData(25, true));
			} else {
				// We found column widths in the preference store. We use them.
				for (int colIdx=0; colIdx<NUMBER_OF_COLUMNS; colIdx++) {
					layout.addColumnData(new ColumnPixelData(preferredWidths[colIdx], true));
				}
			}
		}

		changeLogTable.setLayout(layout);
		for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
			TableColumn column = new TableColumn(changeLogTable, i == 0 ? SWT.CENTER : SWT.LEFT);
			switch (i) {
			case 0:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.graph")); //$NON-NLS-1$
				break;
			case 1:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.changeset")); //$NON-NLS-1$
				break;
			case 2:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.tag")); //$NON-NLS-1$
				break;
			case 3:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.branch")); //$NON-NLS-1$
				break;
			case 4:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.user")); //$NON-NLS-1$
				break;
			case 5:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.date")); //$NON-NLS-1$
				break;
			case 6:
				column.setText(Messages.getString("MercurialHistoryPage.columnHeader.summary")); //$NON-NLS-1$
				break;
			default:
				// If this shows up in the view, then there was a programming error: please update
				// the number of columns constant.
				column.setText("ERROR!!!"); //$NON-NLS-1$
				break;
			}
			//
			// Listener to be notified of changes in the column width.
			if (store.getBoolean(PREF_HISTORY_COLUMN_PERSISTENCE)) {
				column.addControlListener(new ControlListener() {

					public void controlResized(ControlEvent e) {
						persistColumnWidths();
					}

					public void controlMoved(ControlEvent e) {
						// We have no interest in this: the columns aren't defined as moveable.
					}
				});
			}
		}
		viewer.setLabelProvider(new ChangeSetLabelProvider());
		viewer.setContentProvider(new ChangeLogContentProvider());
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				getCompareWithPreviousAction();
				updateActionEnablement();
				if(compareWithPrevAction.isEnabled()) {
					compareWithPrevAction.run();
				}
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				if(!gotoText.isVisible()) {
					return;
				}
				IStructuredSelection selection = getSelection();
				if(selection.isEmpty()) {
					return;
				}
				gotoText.setText(((MercurialRevision) selection.getFirstElement()).getChangeSet()
						.toString());
				gotoText.selectAll();
			}
		});

		viewer.getControl().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				setSelectionProvider(viewer);
			}
		});
		contributeActions();
	}

	/**
	 * Reads the current width of all columns and saves the widths in the preference store.
	 * <p>
	 * <b>NOTE</b>: Unfortunately, the architecture of JFace causes this method to be called
	 * numerous times, way too many times to my taste. I have not been able to determine when the
	 * resizing is finished, so as to call this method only once.
	 */
	private void persistColumnWidths() {
		//
		// Get all widths into a string.
		String values = ""; // comma-separated list of column widths
		int size;
		Table changeLogTable = viewer.getTable();
		TableColumn[] columns = changeLogTable.getColumns();
		assert columns.length == NUMBER_OF_COLUMNS;
		for (int i = 0; i < columns.length; i++) {
			if (i == 0 && graphColumnHidden) {
				// If the graph column is hidden preserve its last preferred width
				// so it will be the correct width next time it is enabled
				int[] previousPreferredWidths = getColumnWidthsFromPrefsIfEnabled();
				size = previousPreferredWidths == null ? 50 : previousPreferredWidths[0];
			} else {
				size = columns[i].getWidth();
			}

			values += size;
			if (i != (columns.length - 1)) {
				values += ",";
			}
		}
		// Save the string to the preference store.
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		store.putValue(PREF_HISTORY_COLUMN_WIDTHS, values);
	}

	/**
	 * Adds field assistance to the revision text field.
	 */
	private void setupRevisionFieldAssistance() {
		proposalProvider = new HistoryContentProposalProvider(this);
		ContentAssistCommandAdapter contentAssist = new ContentAssistCommandAdapter(gotoText,
				new TextContentAdapter(), proposalProvider, null,
				null, true);

		// uncomment to open popup immediately on typing first character into the text field
		// contentAssist.setAutoActivationCharacters(null);
		contentAssist.setAutoActivationDelay(300);
		contentAssist.setPropagateKeys(true);
		contentAssist.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		contentAssist.addContentProposalListener(new IContentProposalListener2() {
			public void proposalPopupOpened(ContentProposalAdapter adapter) {
				fetchEntireHistory(true);
			}
			public void proposalPopupClosed(ContentProposalAdapter adapter) {
				fetchEntireHistory(false);
			}
		});
		contentAssist.addContentProposalListener(new IContentProposalListener() {
			public void proposalAccepted(IContentProposal proposal) {
				if(proposal instanceof RevisionContentProposal) {
					selectProposal(proposal);
				} else {
					// try to find something
					guessAndSelectVersion(proposal.getContent());
				}
			}
		});
	}

	private void copyToClipboard() {
		Iterator<?> iterator = getSelection().iterator();
		StringBuilder text = new StringBuilder();
		Table table = viewer.getTable();
		for(int columnIndex = 1; columnIndex < table.getColumnCount(); columnIndex++) {
			text.append(table.getColumn(columnIndex).getText()).append('\t');
		}

		String crlf = System.getProperty("line.separator"); //$NON-NLS-1$
		text.append(crlf);

		while(iterator.hasNext()) {
			Object next = iterator.next();
			ITableLabelProvider labelProvider = (ITableLabelProvider) viewer.getLabelProvider();
			for(int columnIndex = 1; columnIndex < table.getColumnCount(); columnIndex++) {
				text.append(labelProvider.getColumnText(next, columnIndex)).append('\t');
			}
			text.append(crlf);
		}
		Clipboard clipboard = null;
		try {
			clipboard = new Clipboard(getSite().getShell().getDisplay());
			clipboard.setContents(new String[]{text.toString()},
					new Transfer[]{ TextTransfer.getInstance() });
		} finally {
			if(clipboard != null){
				clipboard.dispose();
			}
		}
	}

	private IStructuredSelection getSelection() {
		return (IStructuredSelection) viewer.getSelection();
	}

	void clearSelection() {
		viewer.setSelection(StructuredSelection.EMPTY);
	}

	public MercurialRevision[] getSelectedRevisions() {
		Object[] obj = getSelection().toArray();
		if (obj != null && obj.length > 0) {
			MercurialRevision[] revs = new MercurialRevision[obj.length];
			int i = 0;
			for (Object o : obj) {
				MercurialRevision mr = (MercurialRevision) o;
				revs[i++] = mr;
			}
			return revs;
		}
		return null;
	}

	private void contributeActions() {

		final Action updateAction = new Action(Messages.getString("MercurialHistoryPage.updateAction.name")) { //$NON-NLS-1$
			private MercurialRevision rev;

			@Override
			public void run() {
				if(rev == null){
					return;
				}
				try {
					HgRoot root = resource != null ? MercurialTeamProvider.getHgRoot(resource) : hgRoot;
					Assert.isNotNull(root);

					UpdateJob job = new UpdateJob(rev.getContentIdentifier(), true, root, false);

					if (!job.confirmDataLoss(getControl().getShell())) {
						return;
					}

					JobChangeAdapter adap = new JobChangeAdapter() {
						@Override
						public void done(IJobChangeEvent event) {
							refresh();
						}
					};
					job.addJobChangeListener(adap);
					job.schedule();
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}

			@Override
			public boolean isEnabled() {
				MercurialRevision[] revs = getSelectedRevisions();
				if (revs != null && revs.length == 1) {
					rev = revs[0];
					return true;
				}
				rev = null;
				return false;
			}
		};
		updateAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/update.gif")); //$NON-NLS-1$

		stripAction = new Action() {
			{
				setText("Strip...");
				setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif"));
			}

			@Override
			public void run() {
				final Shell shell = MercurialEclipsePlugin.getActiveShell();

				shell.getDisplay().asyncExec(new Runnable() {

					public void run() {
						ChangeSet changeSet = null;

						MercurialRevision[] revisions = getSelectedRevisions();
						if (revisions == null || revisions.length != 1) {
							return;
						}
						changeSet = revisions[0].getChangeSet();

						StripHandler.openWizard(changeSet.getHgRoot(), shell, changeSet);
					}
				});
			}

			@Override
			public boolean isEnabled() {
				MercurialRevision[] revs = getSelectedRevisions();
				if (revs != null && revs.length == 1) {
					return true;
				}
				return false;
			}
		};

		backoutAction = new Action() {
			{
				setText("Backout...");
				setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif"));
			}

			@Override
			public void run() {
				final Shell shell = MercurialEclipsePlugin.getActiveShell();

				shell.getDisplay().asyncExec(new Runnable() {

					public void run() {
						ChangeSet changeSet = null;

						MercurialRevision[] revisions = getSelectedRevisions();
						if (revisions == null || revisions.length != 1) {
							return;
						}
						changeSet = revisions[0].getChangeSet();

						BackoutWizard backoutWizard = new BackoutWizard(changeSet.getHgRoot(), changeSet);
						WizardDialog dialog = new WizardDialog(shell, backoutWizard);
						dialog.setBlockOnOpen(true);
						int result = dialog.open();

						if (result == Window.OK) {
							new RefreshWorkspaceStatusJob(changeSet.getHgRoot(), RefreshRootJob.ALL).schedule();
						}
					}
				});
			}

			@Override
			public boolean isEnabled() {
				MercurialRevision[] revs = getSelectedRevisions();
				if (revs != null && revs.length == 1) {
					return true;
				}
				return false;
			}
		};

		// Contribute actions to popup menu
		final MenuManager menuMgr = new MenuManager();
		final MenuManager bisectMenu = new MenuManager("Bisect");
		final MenuManager undoMenu = new MenuManager("Undo",
				MercurialEclipsePlugin.getImageDescriptor("undo_edit.gif"), null);

		undoMenu.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				undoMenu.add(backoutAction);
				undoMenu.add(stripAction);
			}
		});

		bisectMenu.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuMgr1) {
				bisectMenu.add(bisectMarkBadAction);
				bisectMenu.add(bisectMarkGoodAction);
				bisectMenu.add(bisectResetAction);
			}
		});

		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager menuMgr1) {
				// enablement
				updateAction.setEnabled(updateAction.isEnabled());
				bisectMarkBadAction.setEnabled(bisectMarkBadAction.isEnabled());
				bisectMarkGoodAction.setEnabled(bisectMarkGoodAction.isEnabled());
				bisectResetAction.setEnabled(bisectResetAction.isEnabled());
				exportAsBundleAction.setEnabled(true);
				mergeWithCurrentChangesetAction.setEnabled(true);
				stripAction.setEnabled(stripAction.isEnabled());
				backoutAction.setEnabled(backoutAction.isEnabled());
				undoMenu.setVisible(stripAction.isEnabled() || backoutAction.isEnabled());

				// layout
				if(resource instanceof IFile){
					IStructuredSelection sel = updateActionEnablement();
					menuMgr1.add(openAction);
					menuMgr1.add(openEditorAction);
					menuMgr1.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
					if(sel.size() == 2){
						menuMgr1.add(compareTwo);
					} else {
						menuMgr1.add(compareWithPrevAction);
						menuMgr1.add(compareWithCurrAction);
						menuMgr1.add(compareWithOtherAction);
						menuMgr1.add(new Separator());
						menuMgr1.add(revertAction);
					}
				}
				menuMgr1.add(mergeWithCurrentChangesetAction);
				menuMgr1.add(undoMenu);
				menuMgr1.add(new Separator());
				menuMgr1.add(updateAction);
				menuMgr1.add(bisectMenu);
				menuMgr1.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				menuMgr1.add(exportAsBundleAction);
			}
		});

		bisectMenu.setRemoveAllWhenShown(true);
		undoMenu.setRemoveAllWhenShown(true);
		menuMgr.setRemoveAllWhenShown(true);
		viewer.getTable().setMenu(menuMgr.createContextMenu(viewer.getTable()));
		getSite().registerContextMenu(MercurialEclipsePlugin.ID + ".hgHistoryPage",  menuMgr, viewer);
	}

	private void createActions() {
		getOpenAction();
		getOpenEditorAction();
		getCompareWithCurrentAction();
		getCompareWithOtherAction();
		getRevertAction();
		compareTwo = new CompareRevisionAction(Messages.getString("CompareWithEachOtherAction.label")){ //$NON-NLS-1$
			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				if(selection.size() != 2){
					return false;
				}
				return super.updateSelection(selection);
			}
		};
	}

	OpenMercurialRevisionAction getOpenAction() {
		if(openAction != null){
			return openAction;
		}
		openAction = new OpenMercurialRevisionAction(Messages.getString("MercurialHistoryPage.openSelectedVersion")); //$NON-NLS-1$
		openAction.setPage(this);
		return openAction;
	}

	BaseSelectionListenerAction getOpenEditorAction() {
		if(openEditorAction != null){
			return openEditorAction;
		}

		openEditorAction = new BaseFileHistoryAction(Messages.getString("MercurialHistoryPage.openCurrentVersion")) {
			@Override
			public void run() {
				if(file == null){
					return;
				}
				ResourceUtils.openEditor(getSite().getPage(), file);
			}
		};
		return openEditorAction;
	}

	BaseSelectionListenerAction getFocusOnSelectedFileAction() {
		if(focusOnSelectedFileAction != null){
			return focusOnSelectedFileAction;
		}

		focusOnSelectedFileAction = new BaseFileHistoryAction(Messages.getString("MercurialHistoryPage.showSelectedFileHistory")) { //$NON-NLS-1$
			@Override
			public void run() {
				if(file == null){
					return;
				}
				getHistoryView().showHistoryFor(file, true);
			}

			@Override
			protected boolean updateSelection(IStructuredSelection selection) {
				boolean result = super.updateSelection(selection);
				if(result) {
					// disable "focus on" for the already focused file
					if(file != null && file.equals(resource)) {
						file = null;
						return false;
					}
				}
				return result;
			}
		};
		focusOnSelectedFileAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/goto.gif"));
		return focusOnSelectedFileAction;
	}

	CompareRevisionAction getCompareWithCurrentAction() {
		if(compareWithCurrAction == null) {
			compareWithCurrAction = new CompareRevisionAction(Messages.getString("CompareAction.label")); //$NON-NLS-1$
		}
		return compareWithCurrAction;
	}

	CompareRevisionAction getCompareWithPreviousAction() {
		if(compareWithPrevAction == null) {
			compareWithPrevAction = new CompareRevisionAction(Messages.getString("CompareWithPreviousAction.label")); //$NON-NLS-1$
			compareWithPrevAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("compare_view.gif")); //$NON-NLS-1$
			compareWithPrevAction.setCompareWithPrevousEnabled(true);
		}
		return compareWithPrevAction;
	}

	CompareRevisionAction getCompareWithOtherAction() {
		if(compareWithOtherAction == null) {
			compareWithOtherAction = new CompareRevisionAction(Messages.getString("CompareWithOtherAction.label")) { //$NON-NLS-1$

				private IFile file;
				private MercurialRevision selectedRev;

				@Override
				public void run() {
					if(file == null || selectedRev == null) {
						return;
					}
					String title = "Compare " + file.getName() + " ["
							+ selectedRev.getRevision() + "] with ...";
					RevisionChooserDialog dialog = new RevisionChooserDialog(getControl().getShell(),
							title, file);
					dialog.setRequireChangeset(true);
					int result = dialog.open();
					if (result == IDialogConstants.OK_ID) {
						JHgChangeSet cs = dialog.getChangeSet();
						MercurialRevision rev = new MercurialRevision(cs, file, null, null);
						super.updateSelection(new StructuredSelection(new Object[] {selectedRev, rev}));
						super.run();
					}
				}

				@Override
				protected boolean updateSelection(IStructuredSelection selection) {
					Object element = selection.getFirstElement();
					if(element instanceof MercurialHistory){
						MercurialHistory history = (MercurialHistory) element;
						IFileRevision[] revisions = history.getFileRevisions();
						if(revisions.length != 1 || !(revisions[0] instanceof MercurialRevision)){
							file = null;
							selectedRev = null;
							return false;
						}
						MercurialRevision rev = (MercurialRevision) revisions[0];
						if(rev.getResource() instanceof IFile){
							file = (IFile) rev.getResource();
							selectedRev = rev;
							return file.exists();
						}
					} else if (element instanceof MercurialRevision){
						MercurialRevision rev = (MercurialRevision) element;
						if(rev.getResource() instanceof IFile){
							file = (IFile) rev.getResource();
							selectedRev = rev;
							return file.exists();
						}
					}
					file = null;
					selectedRev = null;
					return false;
				}
			};
			compareWithOtherAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("compare_view.gif")); //$NON-NLS-1$
		}
		return compareWithOtherAction;
	}

	@Override
	public Control getControl() {
		return rootControl;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public String getDescription() {
		return resource != null? ResourceUtils.getPath(resource).toOSString() : hgRoot.getAbsolutePath();
	}

	public String getName() {
		return getDescription();
	}

	public boolean isValidInput(Object object) {
		return object == null || object instanceof IResource || object instanceof HgRoot
			|| MercurialEclipsePlugin.getAdapter(object, IResource.class) != null;
	}

	public ChangeSet getCurrentWorkdirChangeset() {
		return currentWorkdirChangeset;
	}

	/**
	 * @see org.eclipse.team.ui.history.IHistoryPage#refresh()
	 */
	public void refresh() {
		if (refreshFileHistoryJob == null) {
			refreshFileHistoryJob = new LoadMercurialHistoryJob(Integer.MAX_VALUE);
		}

		if (refreshFileHistoryJob.getState() != Job.NONE) {
			refreshFileHistoryJob.cancel();
			fetchEntireHistory(false);
		}
		scheduleInPage(refreshFileHistoryJob);
	}

	public void scheduleInPage(Job job, long delayInMillis) {
		IWorkbenchSiteProgressService progressService = getProgressService();

		if (progressService != null) {
			progressService.schedule(job, delayInMillis);
		} else {
			job.schedule(delayInMillis);
		}
	}
	public void scheduleInPage(Job job) {
		scheduleInPage(job, 0);
	}

	private IWorkbenchSiteProgressService getProgressService() {
		IWorkbenchSiteProgressService progressService = getHistoryPageSite().getWorkbenchPageSite()
				.getService(IWorkbenchSiteProgressService.class);
		return progressService;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	private IStructuredSelection updateActionEnablement() {
		createActions();
		IStructuredSelection selection = getSelection();
		openAction.selectionChanged(selection);
		openEditorAction.selectionChanged(selection);
		compareWithCurrAction.selectionChanged(selection);
		compareWithOtherAction.selectionChanged(selection);
		compareWithPrevAction.selectionChanged(selection);
		compareTwo.selectionChanged(selection);
		revertAction.selectionChanged(selection);
		return selection;
	}

	public BaseSelectionListenerAction getRevertAction() {
		if (revertAction == null) {
			final String replaceString = Messages.getString("MercurialHistoryPage.replaceCurrentWithSelected");
			final String restoreString = Messages.getString("MercurialHistoryPage.restoreDeletedFile");
			final String undoMoveString = Messages.getString("MercurialHistoryPage.undoMove");
			revertAction = new BaseSelectionListenerAction(replaceString) {
				@Override
				public void run() {
					IStructuredSelection selection = getStructuredSelection();
					if (selection.isEmpty()) {
						return;
					}
					ActionRevert revert = new ActionRevert();
					MercurialRevision revision = (MercurialRevision) selection.getFirstElement();
					IResource selectedElement = revision.getResource();
					MercurialStatusCache cache = MercurialStatusCache.getInstance();
					if (!cache.isUnknown(selectedElement) && !cache.isClean(selectedElement)
							&& !MessageDialog.openQuestion(getControl().getShell(), Messages.getString("MercurialHistoryPage.UncommittedChanges"), //$NON-NLS-1$
							Messages.getString("MercurialHistoryPage.file") + selectedElement.getName() //$NON-NLS-1$
									+ Messages.getString("MercurialHistoryPage.hasUncommittedChanges"))) { //$NON-NLS-1$
						return;
					}
					selection = new StructuredSelection(selectedElement);
					revert.setChangesetToRevert(revision.getChangeSet());
					revert.selectionChanged(this, selection);
					revert.run(this);
				}

				@Override
				protected boolean updateSelection(IStructuredSelection sSelection) {
					if(sSelection.size() != 1){
						return false;
					}
					if(sSelection.size() == 1){
						Object element = sSelection.getFirstElement();
						if(element instanceof MercurialRevision){
							MercurialRevision rev = (MercurialRevision) element;
							if(rev.getResource() instanceof IFile){
								if(rev.getChangeSet().isRemoved(rev.getResource())) {
									setText(restoreString);
								} else if (rev.getChangeSet().isMoved(rev.getResource())) {
									setText(undoMoveString);
								} else {
									setText(replaceString);
								}
								return true;
							}
						}
					}
					return false;
				}
			};
			revertAction.setImageDescriptor(MercurialEclipsePlugin
					.getImageDescriptor("actions/revert.gif")); //$NON-NLS-1$
		}
		return revertAction;
	}

	/**
	 * @param currentWorkdirChangeset the currentWorkdirChangeset to set
	 */
	public void setCurrentWorkdirChangeset(ChangeSet currentWorkdirChangeset) {
		this.currentWorkdirChangeset = currentWorkdirChangeset;
	}

	protected synchronized void fetchEntireHistory(boolean on) {
		if(!on) {
			if(fetchAllJob != null) {
				fetchAllJob.cancel();
			}
		} else {
			if(fetchAllJob == null) {
				fetchAllJob = new FetchEntireHistoryJob("Retrieving entire history");
				fetchAllJob.setRule(new ExclusiveHistoryRule());
			}
			scheduleInPage(fetchAllJob);
		}
	}

	/**
	 * @param proposal non null
	 */
	private void selectProposal(IContentProposal proposal) {
		RevisionContentProposal revProposal = (RevisionContentProposal) proposal;
		MercurialRevision revision = revProposal.getRevision();
		viewer.getControl().setFocus();
		viewer.setSelection(new StructuredSelection(revision));
		viewer.reveal(revision);
	}

	/**
	 * @param text non null
	 */
	private void guessAndSelectVersion(String text) {
		IContentProposal[] proposals = proposalProvider.getProposals(text, text.length());

		if (proposals.length == 0) {
			getHistoryPageSite().getWorkbenchPageSite().getActionBars().getStatusLineManager()
					.setErrorMessage("No matches found");
		} else if (proposals.length == 1) {
			selectProposal(proposals[0]);
		} else {
			getHistoryPageSite().getWorkbenchPageSite().getActionBars().getStatusLineManager()
					.setErrorMessage("Multiple matches found");
		}
	}

	/**
	 * Set the selection provider for current history view
	 */
	void setSelectionProvider(ISelectionProvider provider) {
		getHistoryPageSite().setSelectionProvider(provider);

		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=366468:
		// the code below doesn't work with Eclipse 3.8 anymore, but the hack is not needed anymore
		if(ECLISPE_BEFORE_38) {
			// it looks crazy, but the fact is that the page site doesn't set global
			// selection provider, so we must have it set properly to support Properties view
			getSite().getPage().findView(IHistoryView.VIEW_ID).getSite().setSelectionProvider(provider);
		}
	}

	/**
	 * @see org.eclipse.ui.part.Page#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();

		mercurialHistory = null;

		if (rulerSelectionListener != null) {
			rulerSelectionListener.dispose();
			rulerSelectionListener= null;
		}
	}

	private final class MercurialRevisionAnnotationController extends RevisionAnnotationController {
		public MercurialRevisionAnnotationController(IWorkbenchPage page, IFile file) {
			super(page, file, viewer);
		}

		public MercurialRevisionAnnotationController(IWorkbenchPage page, IStorageEditorInput editorInput) {
			super(page, editorInput, viewer);
		}

		@Override
		protected Object getHistoryEntry(Revision selected) {
			if (selected instanceof ShowAnnotationOperation.MercurialRevision) {
				return MercurialHistoryPage.this.mercurialHistory.getFileRevision(((ShowAnnotationOperation.MercurialRevision)selected).getChangeSet().getNode());
			}
			return null;
		}
	}

	public void linkWithEditor() {
		if (rulerSelectionListener != null) {
			rulerSelectionListener.dispose();
			rulerSelectionListener= null;
		}

		if (!getHistoryPageSite().isModal()) {
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				rulerSelectionListener= new MercurialRevisionAnnotationController(getHistoryPageSite().getWorkbenchPageSite().getPage(), file);
			} else {
				Object input = getInput();
				if (input instanceof IStorageEditorInput) {
					IStorageEditorInput editorInput = (IStorageEditorInput) input;
					rulerSelectionListener= new MercurialRevisionAnnotationController(getHistoryPageSite().getWorkbenchPageSite().getPage(), editorInput);
				}
			}
		}
	}

}
