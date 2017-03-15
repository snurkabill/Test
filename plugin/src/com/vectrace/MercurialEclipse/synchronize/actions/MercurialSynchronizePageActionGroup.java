/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov              - bug fixes
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland) - modifications
 *     Ilya Ivanov (Intland) - modifications
 *     Amenel VOGLOZIN - Added management of changesets from the context menu
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonViewer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.GroupedUncommittedChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.Messages;
import com.vectrace.MercurialEclipse.synchronize.PresentationMode;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetActionProvider;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

@SuppressWarnings("restriction")
public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

	static final String EDIT_CHANGESET_ACTION = "hg.editChangeset"; //$NON-NLS-1$
	private static final String HG_COMMIT_GROUP = "hg.commit"; //$NON-NLS-1$
	private static final String HG_PUSH_PULL_GROUP = "hg.push.pull"; //$NON-NLS-1$
	private static final String HG_REASSIGN_CHANGES_SUBMENU = "hg.reassign.changes"; //$NON-NLS-1$

	// see org.eclipse.ui.IWorkbenchCommandConstants.EDIT_DELETE which was introduced in 3.5
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=54581
	// TODO replace with the constant as soon as we drop Eclipse 3.4 support
	public static final String EDIT_DELETE = "org.eclipse.ui.edit.delete"; //$NON-NLS-1$

	public static final String HG_CHANGESETS_GROUP = "hg.changesets"; //$NON-NLS-1$
	public static final String HG_CHANGESETS_SUBMENU_CHANGESETS_GROUP = "hg.changesets.reassign.changesets"; //$NON-NLS-1$
	public static final String HG_CHANGESETS_SUBMENU_DEFAULT_GROUP = "hg.changesets.reassign.default"; //$NON-NLS-1$
	private final IAction expandAction;

	private PushPullSynchronizeAction pullUpdateAllAction;
	private PushPullSynchronizeAction pushAllAction;

	private final PreferenceAction allBranchesAction;

	private final ArrayList<PresentationModeAction> presentationModeActions;

	private OpenAction openAction;
	private final LocalChangeSetEnableAction localChangeSetEnableAction;

	private final IPreferenceStore prefStore;

	// constructor

	public MercurialSynchronizePageActionGroup() {
		super();
		expandAction = new Action(
				Messages.getString("MercurialSynchronizePageActionGroup.ExpandAll"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("elcl16/expandall.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				Viewer viewer = getConfiguration().getPage().getViewer();
				if (viewer instanceof AbstractTreeViewer) {
					AbstractTreeViewer treeViewer = (AbstractTreeViewer) viewer;
					treeViewer.expandAll();
				}
			}
		};

		prefStore = MercurialEclipsePlugin.getDefault().getPreferenceStore();

		presentationModeActions = new ArrayList<PresentationModeAction>();

		for (PresentationMode mode : PresentationMode.values()) {
			presentationModeActions.add(new PresentationModeAction(mode, prefStore));
		}

		localChangeSetEnableAction = new LocalChangeSetEnableAction(prefStore);

		allBranchesAction = new PreferenceAction(
				Messages.getString("MercurialSynchronizePageActionGroup.SynchronizeAllBranches"), //$NON-NLS-1$
				IAction.AS_CHECK_BOX, prefStore,
				MercurialPreferenceConstants.PREF_SYNC_ONLY_CURRENT_BRANCH) {
			@Override
			public void run() {
				prefStore.setValue(prefKey, !isChecked());
				MercurialSynchronizeParticipant participant = (MercurialSynchronizeParticipant) getConfiguration()
						.getParticipant();

				participant.refresh(getConfiguration().getSite().getWorkbenchSite(),
						participant.getContext().getScope().getMappings());
			}

			@Override
			protected void update() {
				setChecked(!prefStore.getBoolean(prefKey));
			}
		};
		allBranchesAction.setImageDescriptor(
				MercurialEclipsePlugin.getImageDescriptor("actions/branch.gif")); //$NON-NLS-1$
		allBranchesAction.update();
	}

	// operations

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		String keyOpen = SynchronizePageConfiguration.P_OPEN_ACTION;
		Object property = configuration.getProperty(keyOpen);
		if (property instanceof Action) {
			openAction = new OpenAction((Action) property, configuration);
			// override default action used on double click with our custom
			configuration.setProperty(keyOpen, openAction);
		}

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new OpenMergeEditorAction(
						Messages.getString("MercurialSynchronizePageActionGroup.OpenInMergeEditor"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new ShowHistorySynchronizeAction(
						Messages.getString("MercurialSynchronizePageActionGroup.ShowHistory"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction(
						Messages.getString("MercurialSynchronizePageActionGroup.Push"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider(), false, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction(
						Messages.getString("MercurialSynchronizePageActionGroup.PullAndUpdate"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider(), true, true));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction(
						Messages.getString("MercurialSynchronizePageActionGroup.Pull"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider(), true, false));

		pullUpdateAllAction = new PushPullSynchronizeAction(
				Messages.getString("MercurialSynchronizePageActionGroup.PullAndUpdate"), //$NON-NLS-1$
				configuration, getVisibleRootsSelectionProvider(), true, true);
		pullUpdateAllAction.setAllowAll(true);
		pushAllAction = new PushPullSynchronizeAction(
				Messages.getString("MercurialSynchronizePageActionGroup.PushAll"), configuration, //$NON-NLS-1$
				getVisibleRootsSelectionProvider(), false, false);
		pushAllAction.setAllowAll(true);

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_CHANGESETS_GROUP,
				new CreateNewChangesetSynchronizeAction(
						Messages.getString(
								"MercurialSynchronizePageActionGroup.CreateNewChangeSet"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider()));

		EditChangesetSynchronizeAction editAction = new EditChangesetSynchronizeAction(
				Messages.getString("MercurialSynchronizePageActionGroup.EditChangeSet"), //$NON-NLS-1$
				configuration, getVisibleRootsSelectionProvider());

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_CHANGESETS_GROUP,
				editAction);
		// remember action to allow OpenAction re-use it on double click
		configuration.setProperty(EDIT_CHANGESET_ACTION, editAction);

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, HG_CHANGESETS_GROUP,
				new SetDefaultChangesetSynchronizeAction(
						Messages.getString(
								"MercurialSynchronizePageActionGroup.SetAsDefaultChangeSet"), //$NON-NLS-1$
						configuration, getVisibleRootsSelectionProvider()));
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (menu.find(DeleteAction.HG_DELETE_GROUP) == null) {
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP,
					new Separator(DeleteAction.HG_DELETE_GROUP));
		}
		if (menu.find(HG_COMMIT_GROUP) == null) {
			menu.insertBefore(DeleteAction.HG_DELETE_GROUP, new Separator(HG_COMMIT_GROUP));
		}
		if (localChangeSetEnableAction.getBoolean()) {
			if (menu.find(HG_CHANGESETS_GROUP) == null) {
				menu.insertAfter(ISynchronizePageConfiguration.EDIT_GROUP,
						new Separator(HG_CHANGESETS_GROUP));
			}
		} else {
			menu.remove(HG_CHANGESETS_GROUP);
		}
		if (menu.find(HG_PUSH_PULL_GROUP) == null) {
			menu.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP,
					new Separator(HG_PUSH_PULL_GROUP));
		}

		addUndoMenu(menu);
		addReassignChangesMenu(menu);

		if (isSelectionUncommited()) {
			menu.insertAfter(HG_COMMIT_GROUP,
					new AddAction(Messages.getString("MercurialSynchronizePageActionGroup.Add"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(HG_COMMIT_GROUP,
					new CommitSynchronizeAction(
							Messages.getString("MercurialSynchronizePageActionGroup.Commit"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(HG_COMMIT_GROUP,
					new RevertSynchronizeAction(
							Messages.getString("MercurialSynchronizePageActionGroup.Revert"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(HG_COMMIT_GROUP,
					new ResolveSynchronizeAction(
							Messages.getString(
									"MercurialSynchronizePageActionGroup.MarkAsResolved"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));
		} else if (isSelectionOutgoing()) {
			menu.insertAfter(HG_COMMIT_GROUP,
					new ExportPatchSynchronizeAction(
							Messages.getString("MercurialSynchronizePageActionGroup.ExportAsPatch"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));
			menu.insertAfter(HG_COMMIT_GROUP,
					new SwitchToSynchronizeAction(
							Messages.getString("MercurialSynchronizePageActionGroup.SwitchTo"), //$NON-NLS-1$
							Messages.getString(
									"MercurialSynchronizePageActionGroup.SwitchToParent"), //$NON-NLS-1$
							getConfiguration(), getVisibleRootsSelectionProvider()));
		}

		super.fillContextMenu(menu);
		// menu.remove("org.eclipse.team.ui.synchronizeLast");
		replaceCompareAndMoveDeleteAction(menu);
	}

	private boolean isSelectionUncommited() {
		Object[] selectedObjects = getSelectedObjects();

		if (selectedObjects.length == 0) {
			return false;
		}

		for (Object object : selectedObjects) {
			if (object instanceof WorkingChangeSet) {
				continue;
			} else if (object instanceof FileFromChangeSet) {
				FileFromChangeSet file = (FileFromChangeSet) object;
				if (!(file.getChangeset() instanceof WorkingChangeSet)) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private boolean isSelectionOutgoing() {
		Object[] selectedObjects = getSelectedObjects();

		if (selectedObjects.length == 0) {
			return false;
		}

		for (Object object : selectedObjects) {
			if (object instanceof ChangesetGroup) {
				ChangesetGroup csGroup = (ChangesetGroup) object;
				if (csGroup.getDirection() != Direction.OUTGOING) {
					return false;
				}
			} else if (object instanceof ChangeSet) {
				ChangeSet cs = (ChangeSet) object;
				if (cs.getDirection() != Direction.OUTGOING || cs instanceof WorkingChangeSet) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private void addReassignChangesMenu(IMenuManager menu) {
		// The submenu for managing changesets shows up only when local changesets are enabled and
		// the preference for the submenu is ticked.
		if (!prefStore.getBoolean(
				MercurialPreferenceConstants.PREF_SYNC_ENABLE_LOCAL_CHANGESETS_CONTEXT_MENU)
				|| !prefStore.getBoolean(
						MercurialPreferenceConstants.PREF_SYNC_ENABLE_LOCAL_CHANGESETS_CONTEXT_MENU)) {
			return;
		}

		//
		// We get the files to be reassigned from the current selection.
		List<IFile> selectedFiles = new ArrayList<IFile>();

		// NOTE: for some strange reason I (@Amenel) didn't want to investigate, the selection
		// always appears null in the ReassignTo action classes when their menu entries are in the
		// submenu. When the entries are directly in the context menu, the actions can "see" the
		// selection. Therefore, I had to determine the list of files to operate on from right here
		// and not from within the ReassignTo action classes.
		ISelection selection = getContext().getSelection();

		if (!(selection instanceof StructuredSelection)) {
			return;
		}
		Object[] objects = PathAwareAction.normalize(((StructuredSelection) selection).toArray());

		for (Object object : objects) {
			if (object instanceof WorkingChangeSet) {
				selectedFiles.addAll(((WorkingChangeSet) object).getFiles());
			} else if (!(object instanceof ChangeSet)) {
				IResource resource = ResourceUtils.getResource(object);
				if (resource != null && resource instanceof IFile) {
					selectedFiles.add((IFile) resource);
				}
			}
		}

		if (selectedFiles.size() == 0) {
			return;
		}

		//
		// We get the Uncommitted changeset group.
		ISynchronizePageConfiguration configuration = getConfiguration();
		Viewer viewer = configuration.getPage().getViewer();
		if (!(viewer instanceof ContentViewer)) {
			return;
		}
		CommonViewer commonViewer = (CommonViewer) viewer;
		final HgChangeSetContentProvider csProvider = OpenAction
				.getProvider(commonViewer.getNavigatorContentService());
		IUncommitted uncommittedEntry = csProvider.getUncommittedEntry();
		if (!(uncommittedEntry instanceof UncommittedChangesetGroup)) {
			// The user is not using local changesets. Showing changeset actions
			// does not make sense.
			return;
		}
		UncommittedChangesetGroup ucg = (UncommittedChangesetGroup) uncommittedEntry;

		//
		// We build the context menu structure.
		IMenuManager reassignChangesSubmenu = new MenuManager(
				Messages.getString("MercurialSynchronizePageActionGroup.ReassignChanges"), //$NON-NLS-1$
				null, HG_REASSIGN_CHANGES_SUBMENU);

		menu.insertAfter(HG_CHANGESETS_GROUP, reassignChangesSubmenu);

		// New change set entry.
		ISelectionProvider visibleRootsSelectionProvider = getVisibleRootsSelectionProvider();
		ReassignToNewChangeSetSynchronizeAction action = new ReassignToNewChangeSetSynchronizeAction(
				Messages.getString("MercurialSynchronizePageActionGroup.ReassignToNewChangeSet"),
				configuration, visibleRootsSelectionProvider, selectedFiles, ucg);
		reassignChangesSubmenu.add(action);

		// Entries for the current changesets.
		GroupedUncommittedChangeSet defaultChangeset = null;
		Set<ChangeSet> changesets = ucg.getChangesets();
		int idx = 0;
		if (changesets.size() > 0) {
			reassignChangesSubmenu.add(new Separator(HG_CHANGESETS_SUBMENU_CHANGESETS_GROUP));
			for (ChangeSet cs : changesets) {
				GroupedUncommittedChangeSet gucs = (GroupedUncommittedChangeSet) cs;
				if (gucs.isDefault()) {
					defaultChangeset = gucs;
				}
				reassignChangesSubmenu.add(new ReassignToChangeSetSynchronizeAction(
						buildWorkingSetMenuLabel(gucs), configuration,
						visibleRootsSelectionProvider, ucg, selectedFiles, gucs, idx));
				idx++;
			}
		}

		// Entry for the default changeset.
		if (defaultChangeset != null) { // This condition should always be true.
			reassignChangesSubmenu.add(new Separator(HG_CHANGESETS_SUBMENU_DEFAULT_GROUP));
			reassignChangesSubmenu.add(new ReassignToChangeSetSynchronizeAction(
					Messages.getString(
							"MercurialSynchronizePageActionGroup.ReassignChanges.DefaultChangeset"), //$NON-NLS-1$
					configuration, visibleRootsSelectionProvider, ucg, selectedFiles,
					defaultChangeset, idx));
		}

	}

	/**
	 * Builds the label that represents a change set in the menu.
	 *
	 * @param gucs
	 *            a changeset
	 * @return
	 */
	private static String buildWorkingSetMenuLabel(GroupedUncommittedChangeSet gucs) {
		String res = gucs.getName() + " : " + gucs.getComment();//$NON-NLS-1$
		if (gucs.isDefault()) {
			res = "> " + res; //$NON-NLS-1$
		}
		if (res.length() > 100) { // Trim to an arbitrary value
			res = res.substring(0, 95) + "[...]"; //$NON-NLS-1$
		}
		return res;
	}

	private void addUndoMenu(IMenuManager menu) {
		MenuManager submenu = new MenuManager(
				Messages.getString("MercurialSynchronizePageActionGroup.Undo"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("undo_edit.gif"), null); //$NON-NLS-1$

		menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, submenu);

		ISelection selection = getContext().getSelection();

		if (!(selection instanceof StructuredSelection)) {
			return;
		}

		StructuredSelection stSelection = (StructuredSelection) selection;
		if (stSelection.size() != 1) {
			return;
		}

		Object object = stSelection.iterator().next();
		if (object instanceof WorkingChangeSet) {
			return;
		}

		if (object instanceof ChangesetGroup) {
			ChangesetGroup csGroup = ((ChangesetGroup) object);
			if (csGroup.getChangesets().isEmpty() || csGroup.getDirection() != Direction.OUTGOING) {
				return;
			}

			HgRoot hgRoot = csGroup.getChangesets().iterator().next().getHgRoot();
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP,
					new RollbackSynchronizeAction(
							Messages.getString("MercurialSynchronizePageActionGroup.Rollback"), //$NON-NLS-1$
							getConfiguration(), hgRoot, null));
		} else if (object instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) object;

			if (changeSet.getDirection() != Direction.OUTGOING) {
				return;
			}

			HgRoot hgRoot = changeSet.getHgRoot();
			submenu.add(new BackoutSynchronizeAction(
					Messages.getString("MercurialSynchronizePageActionGroup.Backout"), //$NON-NLS-1$
					getConfiguration(), hgRoot, changeSet));
			submenu.add(new StripSynchronizeAction(
					Messages.getString("MercurialSynchronizePageActionGroup.Strip"), //$NON-NLS-1$
					getConfiguration(), hgRoot, changeSet));
		}
	}

	/**
	 * @return Not null.
	 */
	private Object[] getSelectedObjects() {
		ISelection selection = getContext().getSelection();
		Object[] arr = null;

		if (selection instanceof StructuredSelection) {
			arr = ((StructuredSelection) selection).toArray();
		}

		return PathAwareAction.normalize(arr);
	}

	/**
	 * Replaces default "OpenInCompareAction" action with our custom, moves delete action
	 *
	 * @see OpenInCompareAction
	 * @see ModelSynchronizeParticipantActionGroup
	 * @see HgChangeSetActionProvider
	 */
	private void replaceCompareAndMoveDeleteAction(IMenuManager menu) {
		if (openAction == null) {
			return;
		}
		Object[] elements = ((IStructuredSelection) getContext().getSelection()).toArray();
		if (elements.length == 0) {
			return;
		}
		IContributionItem fileGroup = findGroup(menu, ISynchronizePageConfiguration.FILE_GROUP);
		if (fileGroup == null) {
			return;
		}
		IContributionItem[] items = menu.getItems();
		for (IContributionItem ci : items) {
			if (!(ci instanceof ActionContributionItem)) {
				continue;
			}
			ActionContributionItem ai = (ActionContributionItem) ci;
			IAction action = ai.getAction();
			if (action instanceof OpenInCompareAction) {
				menu.remove(ai);
				openAction.setImageDescriptor(action.getImageDescriptor());
				openAction.setText(action.getText());
				menu.prependToGroup(fileGroup.getId(), openAction);
			} else if (EDIT_DELETE.equals(action.getActionDefinitionId())) {
				menu.remove(ai);
				menu.appendToGroup(DeleteAction.HG_DELETE_GROUP, ai);
			}
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IToolBarManager manager = actionBars.getToolBarManager();
		appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, expandAction);
		appendToGroup(manager, ISynchronizePageConfiguration.MODE_GROUP, allBranchesAction);
		appendToGroup(manager, ISynchronizePageConfiguration.MODE_GROUP, pullUpdateAllAction);
		appendToGroup(manager, ISynchronizePageConfiguration.MODE_GROUP, pushAllAction);

		IMenuManager menu = actionBars.getMenuManager();
		IContributionItem group = findGroup(menu, ISynchronizePageConfiguration.LAYOUT_GROUP);

		if (menu != null && group != null) {
			menu.appendToGroup(group.getId(), allBranchesAction);

			MenuManager layout = new MenuManager(
					Messages.getString("MercurialSynchronizePageActionGroup.PresentationMode")); //$NON-NLS-1$
			menu.appendToGroup(group.getId(), layout);

			for (PresentationModeAction action : presentationModeActions) {
				layout.add(action);
			}

			menu.appendToGroup(group.getId(), localChangeSetEnableAction);
		}
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup#dispose()
	 */
	@Override
	public void dispose() {
		for (PresentationModeAction action : presentationModeActions) {
			action.dispose();
		}
		allBranchesAction.dispose();
		localChangeSetEnableAction.dispose();
		super.dispose();
	}

	// inner types

	/**
	 * Listens to a preference store. Must be disposed.
	 */
	private static abstract class PreferenceAction extends Action
			implements IPropertyChangeListener {
		protected final IPreferenceStore prefStore;
		protected final String prefKey;

		protected PreferenceAction(String name, int style, IPreferenceStore configuration,
				String prefKey) {
			super(name, style);

			this.prefStore = configuration;
			this.prefKey = prefKey;

			configuration.addPropertyChangeListener(this);
		}

		protected abstract void update();

		public void dispose() {
			prefStore.removePropertyChangeListener(this);
		}

		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(prefKey)) {
				update();
			}
		}

		protected boolean getBoolean() {
			return prefStore.getBoolean(prefKey);
		}

		protected void set(boolean val) {
			prefStore.setValue(prefKey, val);
		}
	}

	private static class PresentationModeAction extends PreferenceAction {
		private final PresentationMode mode;

		protected PresentationModeAction(PresentationMode mode, IPreferenceStore configuration) {
			super(mode.toString(), IAction.AS_RADIO_BUTTON, configuration,
					PresentationMode.PREFERENCE_KEY);

			this.mode = mode;
			update();
		}

		@Override
		public void run() {
			mode.set();
		}

		@Override
		public void update() {
			setChecked(mode.isSet());
		}
	}

	private static class LocalChangeSetEnableAction extends PreferenceAction {

		protected LocalChangeSetEnableAction(IPreferenceStore configuration) {
			super("Enable local changesets", IAction.AS_CHECK_BOX, configuration, //$NON-NLS-1$
					MercurialPreferenceConstants.PREF_SYNC_ENABLE_LOCAL_CHANGESETS);

			setToolTipText(Messages
					.getString("MercurialSynchronizePageActionGroup.EnableLocalChangesetsTooltip")); //$NON-NLS-1$
			update();
		}

		@Override
		public void run() {
			set(isChecked());
		}

		@Override
		public void update() {
			setChecked(getBoolean());
		}
	}
}
