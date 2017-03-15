/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Zsolt Kopany (Intland)    - bug fixes
 *     Philip Graf               - bug fix
 *     Amenel Voglozin           - Feature. Configurable project labels.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import static com.vectrace.MercurialEclipse.preferences.HgDecoratorConstants.*;
import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import com.google.common.base.Strings;
import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgBisectClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.HgDecoratorConstants;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.BranchUtils;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author zingo
 */
public class ResourceDecorator extends LabelProvider implements ILightweightLabelDecorator, Observer {
	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();
	private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache.getInstance();
	private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache.getInstance();

	private static final String[] FONTS = new String[] {
		//@formatter:off
		ADDED_FONT,
		CONFLICT_FONT,
		DELETED_FONT,
		REMOVED_FONT,
		UNKNOWN_FONT,
		IGNORED_FONT, CHANGE_FONT };
		//@formatter:on

	private static final String[] COLORS = new String[] {
		//@formatter:off
		ADDED_BACKGROUND_COLOR,
		ADDED_FOREGROUND_COLOR,
		CHANGE_BACKGROUND_COLOR,
		CHANGE_FOREGROUND_COLOR,
		CONFLICT_BACKGROUND_COLOR,
		CONFLICT_FOREGROUND_COLOR,
		IGNORED_BACKGROUND_COLOR,
		IGNORED_FOREGROUND_COLOR,
		DELETED_BACKGROUND_COLOR,
		DELETED_FOREGROUND_COLOR,
		REMOVED_BACKGROUND_COLOR,
		REMOVED_FOREGROUND_COLOR,
		UNKNOWN_BACKGROUND_COLOR,
		UNKNOWN_FOREGROUND_COLOR };
		//@formatter:on

	/**
	 * These are prefs that we want to be notified about when they are changed. In order to save on
	 * preference store querying operations, these preferences are read once in the constructor and
	 * reloaded we are notified of a change.
	 */
	private static final Set<String> INTERESTING_PREFS = new HashSet<String>();
	static {
		INTERESTING_PREFS.add(LABELDECORATOR_LOGIC_2MM);
		INTERESTING_PREFS.add(LABELDECORATOR_LOGIC);
		INTERESTING_PREFS.add(PREF_DECORATE_WITH_COLORS);
		INTERESTING_PREFS.add(RESOURCE_DECORATOR_SHOW_CHANGESET_IN_PROJECT_LABEL);
		INTERESTING_PREFS.add(RESOURCE_DECORATOR_SHOW_CHANGESET);
		INTERESTING_PREFS.add(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET);
		INTERESTING_PREFS.add(RESOURCE_DECORATOR_SHOW_SUMMARY);
		INTERESTING_PREFS.add(PREF_ENABLE_SUBREPO_SUPPORT);
		INTERESTING_PREFS.add(PREF_SHOW_LOGICAL_NAME_OF_REPOSITORIES);
		INTERESTING_PREFS.add(PREF_DECORATE_PROJECT_LABEL_SYNTAX);
	}

	/** set to true when having 2 different statuses in a folder flags it has modified */
	private boolean folderLogic2MM;
	private ITheme theme;
	private boolean colorise;
	private boolean showChangesetInProjectLabel;
	private boolean showChangeset;
	private boolean showIncomingChangeset;
	private boolean enableSubrepos;
	private boolean showRepoLogicalName;
	private String userSyntax;
	private boolean disposed;
	private final IPropertyChangeListener themeListener;
	private final IPropertyChangeListener prefsListener;
	private boolean showSummary;

	/** Bean used when the user configures the project label syntax from the preference page. */
	private static ProjectInfoBean previewInfoBean = null;

	public ResourceDecorator() {
		configureFromPreferences();
		STATUS_CACHE.addObserver(this);
		INCOMING_CACHE.addObserver(this);
		theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		ensureFontAndColorsCreated(FONTS, COLORS);

		themeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(!IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty())){
					return;
				}
				theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				ensureFontAndColorsCreated(FONTS, COLORS);
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeListener);

		prefsListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(INTERESTING_PREFS.contains(event.getProperty())){
					configureFromPreferences();
					fireLabelProviderChanged(new LabelProviderChangedEvent(ResourceDecorator.this));
				}
			}
		};
		MercurialEclipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(prefsListener);
	}

	/**
	 * This method will ensure that the fonts and colors used by the decorator
	 * are cached in the registries. This avoids having to syncExec when
	 * decorating since we ensure that the fonts and colors are pre-created.
	 *
	 * @param f
	 *            fonts ids to cache
	 * @param c
	 *            color ids to cache
	 */
	private void ensureFontAndColorsCreated(final String[] f, final String[] c) {
		MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				for (int i = 0; i < c.length; i++) {
					theme.getColorRegistry().get(c[i]);
				}
				for (int i = 0; i < f.length; i++) {
					theme.getFontRegistry().get(f[i]);
				}
			}
		});
	}

	@Override
	public void dispose() {
		if(disposed) {
			return;
		}
		disposed = true;
		STATUS_CACHE.deleteObserver(this);
		INCOMING_CACHE.deleteObserver(this);
		PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
		MercurialEclipsePlugin.getDefault().getPreferenceStore().removePropertyChangeListener(prefsListener);
		super.dispose();
	}

	/**
	 * Init all the options we need from preferences to avoid doing this all the time
	 */
	private void configureFromPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		folderLogic2MM = LABELDECORATOR_LOGIC_2MM.equals(store.getString(LABELDECORATOR_LOGIC));
		colorise = store.getBoolean(PREF_DECORATE_WITH_COLORS);
		showChangesetInProjectLabel = store.getBoolean(RESOURCE_DECORATOR_SHOW_CHANGESET_IN_PROJECT_LABEL);
		showChangeset = store.getBoolean(RESOURCE_DECORATOR_SHOW_CHANGESET);
		showIncomingChangeset = store.getBoolean(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET);
		showSummary = store.getBoolean(RESOURCE_DECORATOR_SHOW_SUMMARY);
		enableSubrepos = store.getBoolean(PREF_ENABLE_SUBREPO_SUPPORT);
		userSyntax = store.getString(PREF_DECORATE_PROJECT_LABEL_SYNTAX);
		showRepoLogicalName = store.getBoolean(PREF_SHOW_LOGICAL_NAME_OF_REPOSITORIES);
	}

	public void decorate(Object element, IDecoration d) {
		IResource resource = (IResource) element;
		IProject project = resource.getProject();
		if (project == null || !project.isAccessible()) {
			return;
		}

		try {
			if (!MercurialTeamProvider.isHgTeamProviderFor(project)) {
				return;
			}

			if (!STATUS_CACHE.isStatusKnown(project)) {
				// simply wait until the cache sends us an event
				d.addOverlay(DecoratorImages.NOT_TRACKED);
				if(resource == project){
					d.addSuffix(" [Hg status pending...]");
				}
				return;
			}

			ImageDescriptor overlay = null;
			StringBuilder prefix = new StringBuilder(2);
			Integer output = STATUS_CACHE.getStatus(resource);
			if (output != null) {
				overlay = decorate(output.intValue(), prefix, d, colorise);
			} else {
				if (resource.getType() == IResource.FILE) {
					overlay = decorate(MercurialStatusCache.BIT_IGNORE, prefix, d, colorise);
				}
				// empty folder, do nothing
			}
			if (overlay != null) {
				d.addOverlay(overlay);
			}

			if (!showChangeset) {
				if (resource.getType() == IResource.PROJECT || shouldCheckSubrepo(resource)) {
					d.addSuffix(getSuffixForContainer((IContainer)resource));
				}
			} else {
				addChangesetInfo(d, resource, project, prefix);
			}

			// we want a prefix, even if no changeset is displayed
			if (prefix.length() > 0) {
				d.addPrefix(prefix.toString());
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private boolean shouldCheckSubrepo(IResource resource) throws HgException {
		return enableSubrepos && resource.getType() == IResource.FOLDER
				&& AbstractClient.isHgRoot(resource) != null;
	}

	/**
	 * @param statusBits non null hg status bits from cache
	 */
	private ImageDescriptor decorate(int statusBits, StringBuilder prefix, IDecoration d, boolean coloriseLabels) {
		ImageDescriptor overlay = null;
		// BitSet output = fr.getStatus();
		// "ignore" does not really count as modified
		if (folderLogic2MM
				&& (Bits.cardinality(statusBits) > 2 || (Bits.cardinality(statusBits) == 2 && !Bits.contains(statusBits,
						MercurialStatusCache.BIT_IGNORE)))) {
			overlay = DecoratorImages.MODIFIED;
			if (coloriseLabels) {
				setBackground(d, CHANGE_BACKGROUND_COLOR);
				setForeground(d, CHANGE_FOREGROUND_COLOR);
				setFont(d, CHANGE_FONT);
			} else {
				prefix.append('>');
			}
		} else {
			switch (Bits.highestBit(statusBits)) {
			case MercurialStatusCache.BIT_IGNORE:
				if (coloriseLabels) {
					setBackground(d, IGNORED_BACKGROUND_COLOR);
					setForeground(d, IGNORED_FOREGROUND_COLOR);
					setFont(d, IGNORED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_MODIFIED:
				overlay = DecoratorImages.MODIFIED;
				if (coloriseLabels) {
					setBackground(d, CHANGE_BACKGROUND_COLOR);
					setForeground(d, CHANGE_FOREGROUND_COLOR);
					setFont(d, CHANGE_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_ADDED:
				overlay = DecoratorImages.ADDED;
				if (coloriseLabels) {
					setBackground(d, ADDED_BACKGROUND_COLOR);
					setForeground(d, ADDED_FOREGROUND_COLOR);
					setFont(d, ADDED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_UNKNOWN:
				overlay = DecoratorImages.NOT_TRACKED;
				if (coloriseLabels) {
					setBackground(d, UNKNOWN_BACKGROUND_COLOR);
					setForeground(d, UNKNOWN_FOREGROUND_COLOR);
					setFont(d, UNKNOWN_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_CLEAN:
				overlay = DecoratorImages.MANAGED;
				break;
				// case BIT_IGNORE:
				// do nothing
			case MercurialStatusCache.BIT_REMOVED:
				overlay = DecoratorImages.REMOVED;
				if (coloriseLabels) {
					setBackground(d, REMOVED_BACKGROUND_COLOR);
					setForeground(d, REMOVED_FOREGROUND_COLOR);
					setFont(d, REMOVED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_MISSING:
				overlay = DecoratorImages.DELETED_STILL_TRACKED;
				if (coloriseLabels) {
					setBackground(d, DELETED_BACKGROUND_COLOR);
					setForeground(d, DELETED_FOREGROUND_COLOR);
					setFont(d, DELETED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_CONFLICT:
				overlay = DecoratorImages.CONFLICT;
				if (coloriseLabels) {
					setBackground(d, CONFLICT_BACKGROUND_COLOR);
					setForeground(d, CONFLICT_FOREGROUND_COLOR);
					setFont(d, CONFLICT_FONT);
				} else {
					prefix.append('>');
				}
				break;
			default:
				break;
			}
		}
		return overlay;
	}

	private void addChangesetInfo(IDecoration d, IResource resource, IProject project, StringBuilder prefix) throws CoreException {
		// label info for incoming changesets
		ChangeSet newestIncomingChangeSet = null;
		if(showIncomingChangeset) {
			try {
				newestIncomingChangeSet = INCOMING_CACHE.getNewestChangeSet(resource);
			} catch (HgException e) {
				// if an error occurs we want the rest of the decoration to succeed nonetheless
				MercurialEclipsePlugin.logError(e);
			}
		}

		if (newestIncomingChangeSet != null) {
			if (prefix.length() == 0) {
				prefix.append('<').append(' ');
			} else {
				prefix.insert(0, '<');
			}
		}

		// local changeset info
		try {
			// init suffix with project changeset information, or for folders that contain a subrepos
			String suffix = ""; //$NON-NLS-1$
			if (resource.getType() == IResource.PROJECT || shouldCheckSubrepo(resource)) {
				suffix = getSuffixForContainer((IContainer)resource);
			}

			// overwrite suffix for files
			if (resource.getType() == IResource.FILE) {
				suffix = getSuffixForFiles(resource, newestIncomingChangeSet);
			}

			// only decorate files and project with suffix
			if ((resource.getType() != IResource.FOLDER || enableSubrepos) && suffix != null && suffix.length() > 0) {
				d.addSuffix(suffix);
			}

		} catch (HgException e) {
			MercurialEclipsePlugin.logWarning(Messages.getString("ResourceDecorator.couldntGetVersionOfResource") + resource, e);
		}
	}

	private void setBackground(IDecoration d, String id) {
		d.setBackgroundColor(theme.getColorRegistry().get(id));
	}

	private void setForeground(IDecoration d, String id) {
		d.setForegroundColor(theme.getColorRegistry().get(id));
	}

	private void setFont(IDecoration d, String id) {
		d.setFont(theme.getFontRegistry().get(id));
	}

	private static String getSuffixForFiles(IResource resource, ChangeSet cs) throws HgException {
		String suffix = ""; //$NON-NLS-1$
		// suffix for files
		if (!STATUS_CACHE.isAdded(ResourceUtils.getPath(resource))) {
			ChangeSet fileCs = LOCAL_CACHE.getNewestChangeSet(resource);
			if (fileCs != null) {
				suffix = " [" + fileCs.getIndex() + " - " //$NON-NLS-1$ //$NON-NLS-2$
					+ fileCs.getAgeDate() + " - " + fileCs.getAuthor() + "]";

				if (cs != null) {
					suffix += " < [" + cs.getIndex() + ":" //$NON-NLS-1$
						+ cs.getNodeShort() + " - " + cs.getAgeDate()
						+ " - " + cs.getAuthor() + "]";
				}
			}
		}
		return suffix;
	}

	/**
	 * Builds the decoration suffix for containers (aka "projects" in our context). First,
	 * information about the project is collected, and then, a delegate function is tasked with
	 * interpreting the user-given syntax, which is read from the preference store.
	 *
	 * @param container
	 *            the project to decorate
	 * @return the string used by the workbench as a decoration suffix
	 * @throws CoreException
	 */
	private String getSuffixForContainer(IContainer container) throws CoreException {
		ChangeSet changeSet = null;

		HgRoot root;
		if (container instanceof IProject) {
			root = MercurialTeamProvider.getHgRoot(container);
			if (root == null) {
				return "";
			}
			changeSet = LOCAL_CACHE.getCurrentChangeSet(container);
		} else {
			root = AbstractClient.isHgRoot(container);
			if (root == null) {
				return "";
			}
			changeSet = LOCAL_CACHE.getCurrentChangeSet(root);
		}

		ProjectInfoBean infoBean = new ProjectInfoBean();
		if (changeSet == null) {
			infoBean.isNew = true;
		} else {
			// Add logical name of the repo if there's one configured and the appropriate pref is
			// set
			if (container instanceof IProject) {
				IHgRepositoryLocation repoLocation = MercurialEclipsePlugin.getRepoManager()
						.getDefaultRepoLocation(root);
				if (repoLocation != null && !Strings.isNullOrEmpty(repoLocation.getLogicalName())
						&& showRepoLogicalName) {
					infoBean.repoLogicalName = "[" + repoLocation.getLogicalName() + "]";
				}
			}
			String tags = ChangeSetUtils.getPrintableTagsString(changeSet);
			boolean merging = !StringUtils.isEmpty(STATUS_CACHE.getMergeChangesetId(container));

			// XXX should use map, as there can be 100 projects under the same root
			if (HgBisectClient.isBisecting(root)) {
				infoBean.bisectMsg = "BISECTING"; //
			}

			// branch
			String branch = MercurialTeamProvider.getCurrentBranch(root);
			if (branch.length() == 0) {
				branch = BranchUtils.DEFAULT;
			}
			infoBean.branch = branch;

			// tags
			if (tags.length() > 0) {
				infoBean.tags = tags;
			}

			if (showSummary) {
				boolean bDraftShown = false;
				int n;
				if (HgFeatures.PHASES.isEnabled()) {
					n = HgLogClient.countChangesets(root, "draft()");
					if (n > 0) {
						bDraftShown = true;
						infoBean.outgoing = String.valueOf(n);
					}
				}

				n = HgLogClient.numHeadsInBranch(root, branch);
				if (n > 1) {
					if (bDraftShown) {
					}
					infoBean.heads = String.valueOf(n);
				}
			}

			// rev info
			if (showChangesetInProjectLabel) {
				infoBean.index = String.valueOf(changeSet.getIndex());
				infoBean.hex = changeSet.getNodeShort();
				infoBean.node = changeSet.getNode();
				infoBean.author = changeSet.getAuthor();
			}

			// merge flag
			if (merging) {
				// XXX should use map, as there can be 100 projects under the same root
				if (HgRebaseClient.isRebasing(root)) {
					infoBean.mergeMsg = Messages.getString("ResourceDecorator.rebasing");
				} else {
					infoBean.rebaseMsg = Messages.getString("ResourceDecorator.merging");
				}
			}

		}
		return buildSuffixForProject(infoBean, userSyntax);
	}

	/**
	 * Gets the value (among the collected project information) that matches the given keyword.
	 *
	 * @param keyword
	 *            the keyword part of a lexem extracted from the user syntax string: e.g. "tags" in
	 *            the "{tags}" lexem.
	 * @param infoBean
	 *            Information collected about the project
	 * @return <code>null</code> if the keyword is not supported, or otherwise the value of the
	 *         appropriate collected info.
	 */
	private static String getLexemValue(String keyword, ProjectInfoBean infoBean) {

		if (HgDecoratorConstants.LEX_AUTHOR.equals(keyword)) {
			return infoBean.author;
		}
		if (HgDecoratorConstants.LEX_BRANCH.equals(keyword)) {
			return infoBean.branch;
		}
		if (HgDecoratorConstants.LEX_HEADS.equals(keyword)) {
			return infoBean.heads;
		}
		if (HgDecoratorConstants.LEX_HEX.equals(keyword)) {
			return infoBean.hex;
		}
		if (HgDecoratorConstants.LEX_INDEX.equals(keyword)) {
			return infoBean.index;
		}
		if (HgDecoratorConstants.LEX_MERGING_STATUS.equals(keyword)) {
			return infoBean.mergeMsg;
		}
		if (HgDecoratorConstants.LEX_NODE.equals(keyword)) {
			return infoBean.node;
		}
		if (HgDecoratorConstants.LEX_OUTGOING.equals(keyword)) {
			return infoBean.outgoing;
		}
		if (HgDecoratorConstants.LEX_REPO.equals(keyword)) {
			return infoBean.repoLogicalName;
		}
		if (HgDecoratorConstants.LEX_TAGS.equals(keyword)) {
			return infoBean.tags;
		}
		if (HgDecoratorConstants.LEX_MERGING_STATUS.equals(keyword)) {
			return infoBean.mergeMsg;
		}
		if (HgDecoratorConstants.LEX_REBASING_STATUS.equals(keyword)) {
			return infoBean.rebaseMsg;
		}
		if (HgDecoratorConstants.LEX_BISECTING_STATUS.equals(keyword)) {
			return infoBean.bisectMsg;
		}
		return null;
	}

	/**
	 * Builds the string that will appear (in views such as Package Explorer) as the suffix of the
	 * project, respecting both the format that the user has given and the conventions below.
	 * <p>
	 * Conventions:
	 * <ul>
	 * <li>Suffixes are not supported: some syntaxes (such as enclosing the branch name within
	 * parentheses) are not possible.
	 * <li>Keywords are enclosed within curly braces and stand as placeholders for "information
	 * values" (e.g. changeset index or short node, author, repository logical name, etc.).
	 * <li>Empty information values are omitted.
	 * <li>The concept of <em>conditional prefixes</em> is defined:
	 * <ul>
	 * <li>A conditional prefix must immediately precede a supported keyword.
	 * <li>An absent or empty information value prevents the conditional prefix from showing up.
	 * <li>Conditional prefixes are in the output string only when immediately followed by the
	 * information of which they are a prefix.
	 * <li>Conditional prefixes cannot be chained/repeated, i.e. one conditional prefix per keyword.
	 * </ul>
	 * <li>A keyword is either a supported placeholder, which will provide an "information value",
	 * or a conditional prefix.
	 * <li>Supported keywords cannot stand as conditional prefixes.
	 * <li>Leading and trailing spaces are removed.
	 * <li>Unless contributed by a conditional prefix or by the value of a keyword, spaces are not
	 * repeated.
	 * </ul>
	 * <p>
	 * Algorithm: we copy the user syntax string to the output buffer, character per character, only
	 * triggering special processing when a "keyword" (aka "lexem") delimited by braces is
	 * encountered. The keyword is of one of two types: either a supported lexem or a conditional
	 * prefix (everything not supported is treated as a cond prefix). A conditional prefix is left
	 * hanging and will only be copied to the output buffer if the next keyword is a supported
	 * lexem. Everything in-between lexems is copied as-is to the output buffer except repeated
	 * spaces which are simply skipped.
	 * <p>
	 *
	 * @param infoBean
	 *            The information collected about the project
	 * @param syntax
	 *            The format specified by the user
	 * @return A string respecting the user format and the conventions.
	 */
	private static String buildSuffixForProject(ProjectInfoBean infoBean, String syntax) {
		if (infoBean.isNew) {
			return Messages.getString("ResourceDecorator.new");
		}
		StringBuilder res = new StringBuilder(128);

		String hangingConditionalPrefix = null;
		int index = 0, openingIdx, closingIdx;

		// Using TRUE has the effect of not allowing leading whitespace.
		boolean lastCharIsSpace = true;

		res.append(" ["); //$NON-NLS-1$
		while (index < syntax.length()) {
			char c = syntax.charAt(index);
			if (c == '{') {
				openingIdx = index;
				closingIdx = syntax.indexOf('}', openingIdx + 1);
				if (closingIdx == -1) {
					/*
					 * We bail out at the first sign of incomplete/incorrect syntax. However, we
					 * don't return a null string so that the user can see where the syntax is
					 * wrong.
					 */
					break;
				}
				index = closingIdx + 1;
				//
				String keyword = syntax.substring(openingIdx + 1, closingIdx);
				String replacement = getLexemValue(keyword, infoBean);
				if (replacement == null) {
					hangingConditionalPrefix = keyword;
				} else {
					//
					if (replacement.length() > 0) {
						if (hangingConditionalPrefix != null) {
							res.append(hangingConditionalPrefix);
						}
						res.append(replacement);
						lastCharIsSpace = false;
					}
					hangingConditionalPrefix = null;
				}
			} else {
				hangingConditionalPrefix = null;
				if (c != ' ' || !lastCharIsSpace) { // Space characters are not repeated.
					res.append(c);

					lastCharIsSpace = (c == ' ');
				}
				index++;
			}
		}

		//
		// The 'lastCharIsSpace' flag prevents leading spaces and repetition of internal spaces, but
		// it doesn't help with the possible trailing space, which we therefore remove here.
		int lastCharPos = res.length() - 1;
		if (res.charAt(lastCharPos) == ' ') {
			res.deleteCharAt(lastCharPos);
		}
		res.append(']');

		return res.toString();
	}

	/**
	 * Entry point for the preference page.
	 * <p>
	 * NOTE: There is a preference setting that controls whether the user wants to see logical names
	 * of repos in the IDE. Because the project information bean is non-configurable and lazily
	 * created (so that the same object will be reused), we do not honor that preference setting
	 * here. The preference setting is always respected elsewhere but here, we knowingly do not
	 * honor it, solely for conceptual reasons (I -@Amenel- see no reason to create this object
	 * multiple times).
	 *
	 * @param previewUserSyntax
	 *            text entered by the user in the input of the preference page.
	 * @return a string rendered according to the user text, but on a non-configurable set of
	 *         project information.
	 */
	synchronized public static String previewProjectLabel(String previewUserSyntax) {
		if (previewInfoBean == null) {
			previewInfoBean = new ProjectInfoBean();
			previewInfoBean.author = "Jean Bosco";
			previewInfoBean.branch = "Issue502";
			previewInfoBean.heads = "2";
			previewInfoBean.hex = "206f49079726";
			previewInfoBean.index = "375";
			previewInfoBean.outgoing = "7";
			previewInfoBean.repoLogicalName = "[Repo-PUB]";
			previewInfoBean.tags = "v2.3.0";
			previewInfoBean.node = "206f4907972600593c740928d08f61ca21f18092";
		}

		return buildSuffixForProject(previewInfoBean, previewUserSyntax);
	}

	public static String getDecoratorId() {
		String decoratorId = ResourceDecorator.class.getName();
		return decoratorId;
	}

	@SuppressWarnings("unchecked")
	public void update(Observable o, Object updatedObject) {
		if (updatedObject instanceof Set<?>) {
			Set<IResource> changed = (Set<IResource>) updatedObject;
			if(changed.isEmpty()){
				return;
			}
			if (changed.size() < 10) {
				fireNotification(changed);
			} else {
				// if we have a lot of updates, it's easier (faster) to ask clients to update themselves
				// otherwise unneeded decorator updates may cause Eclipse to be busy for minutes, see issue #11928
				updateClientDecorations();
			}
		}
	}

	private void fireNotification(Set<IResource> notification) {
		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, notification.toArray());
		fireLabelProviderChanged(event);
		notification.clear();
	}

	/**
	 * Fire a LabelProviderChangedEvent for this decorator if it is enabled, otherwise do nothing.
	 * <p>
	 * This method can be called from any thread as it will asynchroniously run a job in the user
	 * interface thread as widget updates may result.
	 * </p>
	 */
	public static void updateClientDecorations() {
		Runnable decoratorUpdate = new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getDecoratorManager().update(getDecoratorId());
			}
		};
		Display.getDefault().asyncExec(decoratorUpdate);
	}
}
