/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 * 		Amenel Voglozin - Commenting + Javadoc,unique IDs in changeset names, refactoring of saving/loading to prefs file.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GroupedUncommittedChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * Loads and saves uncommitted changeset state in preferences.
 * <p>
 * (Amenel:) It is important to understand that a changeset is identified in the prefs file by its
 * "name" (aka "unique name", aka "persisted name"). Starting January 2017, the identifier is
 * composed of:
 * <ul>
 * <li>user-editable generated label
 * <li>changeset name separator
 * <li>unique ID (timestamp of the creation of the changeset)
 * </ul>
 *
 *
 * @author Andrei
 */
class UncommittedChangesetManager {

	private static final String PATH_NAME_SEPARATOR = "=";

	private static final String MAPPINGS_SEPARATOR = ";";

	/**
	 * One-character separator for persisting changeset names in the prefs file.
	 */
	private static final String CHANGESET_NAME_SEPARATOR = "@";

	/**
	 * "Encoded" version of the separator. This is to account for cases when a user-given changeset
	 * name contains the separator. We make the bold assumption that users will never use this
	 * encoded substring in the names that they give their changesets.
	 */
	private static final String CHANGESET_NAME_SEPARATOR_ENCODED = "%40";

	private static final String KEY_FILES_PER_PROJECT_PREFIX = "projectFiles/";
	private static final String KEY_CS_COMMENT_PREFIX = "changesetComment/";
	private static final String KEY_CS_DEFAULT = "changesetIsDefault/";
	private static final String KEY_CS_LIST = "changesets";

	private boolean loadingFromPrefs;

	private GroupedUncommittedChangeSet defaultChangeset;
	private IProject[] projects;

	private final UncommittedChangesetGroup group;

	public UncommittedChangesetManager(UncommittedChangesetGroup group) {
		this.group = group;
	}

	protected GroupedUncommittedChangeSet createDefaultChangeset() {
		GroupedUncommittedChangeSet changeset = new GroupedUncommittedChangeSet("Default Changeset", group);
		changeset.setDefault(true);
		changeset.setComment("(no commit message)");
		return changeset;
	}

	public UncommittedChangesetGroup getUncommittedGroup(){
		return group;
	}

	private void loadSavedChangesets(){
		Set<GroupedUncommittedChangeSet> sets = new HashSet<GroupedUncommittedChangeSet>();
		loadfromPreferences(sets);
		assignRemainingFiles();
	}

	private void assignRemainingFiles() {
		if(projects == null) {
			return;
		}
		group.update(null, null);
	}

	/**
	 * "Stores" the changesets to the prefs file. There is no actual writing to disk done because
	 * the Eclipse preferences API deals with writing dirty preference sets to disk. We are tasked
	 * with only giving to value of preference settings.
	 */
	public void storeChangesets(){
		if(loadingFromPrefs) {
			return;
		}
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		Set<ChangeSet> changesets = group.getChangesets();
		Map<IProject, Map<IFile, String>> projectMap = new HashMap<IProject, Map<IFile,String>>();
		StringBuilder changesetNames = new StringBuilder();
		for (ChangeSet changeSet : changesets) {
			//
			// A "unique name" is the name under which the changeset is stored in the prefs file.
			String uniqueName = buildChangesetUniqueName(changeSet);
			String comment = changeSet.getComment();
			changesetNames.append(uniqueName).append(MAPPINGS_SEPARATOR);
			store.putValue(KEY_CS_COMMENT_PREFIX + uniqueName, comment);
			if(((GroupedUncommittedChangeSet)changeSet).isDefault()) {
				store.putValue(KEY_CS_DEFAULT, uniqueName);
			}
			Set<IFile> files = changeSet.getFiles();
			for (IFile file : files) {
				IProject project = file.getProject();
				Map<IFile, String> fileToChangeset = projectMap.get(project);
				if(fileToChangeset == null){
					fileToChangeset = new HashMap<IFile, String>();
					projectMap.put(project, fileToChangeset);
				}
				fileToChangeset.put(file, uniqueName);
			}
		}
		store.putValue(KEY_CS_LIST, changesetNames.toString());

		Set<Entry<IProject,Map<IFile,String>>> entrySet = projectMap.entrySet();
		for (Entry<IProject, Map<IFile, String>> entry : entrySet) {
			IProject project = entry.getKey();
			Map<IFile, String> fileToChangeset = entry.getValue();
			store.putValue(KEY_FILES_PER_PROJECT_PREFIX + project.getName(), encode(fileToChangeset));
		}
	}

	public GroupedUncommittedChangeSet getDefaultChangeset(){
		if(defaultChangeset == null) {
			defaultChangeset = createDefaultChangeset();
		}
		return defaultChangeset;
	}

	/**
	 * Encodes the instances of the [changeset name] separator that happen to be in the changeset
	 * name so that the name can be saved and read correctly.
	 * <p>
	 * <b>NOTE</b>: We make the <u>strong</u> assumption that the encoded separator will never be
	 * found in the names. If this were to be proven wrong, a double encoding will be necessary.
	 *
	 * @param name
	 *            The (user-defined/user-modifiable) label that identifies this changeset in the
	 *            user's eyes.
	 *
	 * @return an encoded changeset name
	 */
	private static String encodeChangesetName(String name) {
		return name.replaceAll(CHANGESET_NAME_SEPARATOR, CHANGESET_NAME_SEPARATOR_ENCODED);
	}

	/**
	 * Decodes a changeset name (i.e. a "unique name") as read from the preferences file.
	 *
	 * @param text
	 *            the part of the unique name that comes before the changeset name separator
	 * @return the original user-given/user-editable name
	 */
	private static String decodeChangesetName(String text) {
		return text.replaceAll(CHANGESET_NAME_SEPARATOR_ENCODED, CHANGESET_NAME_SEPARATOR);
	}

	/**
	 * Builds a uniquely identifying name for a working changeset. This method is needed because
	 * changeset names can be modified by the user, who may happen to give the exact same name to
	 * two changesets. This will cause a problem when reading the prefs file at the start of
	 * another work session because files then run the risk of being assigned to the wrong change
	 * set.
	 *
	 * @param cset
	 *            A changeset (actually, a working changeset)
	 * @return a name (guaranteed to be unique) composed of the user-given name, a separator and a
	 *         unique ID normally not accessible to users.
	 */
	private static String buildChangesetUniqueName(ChangeSet cset) {
		if (cset instanceof WorkingChangeSet) {
			return encodeChangesetName(cset.getName()) + CHANGESET_NAME_SEPARATOR
					+ ((WorkingChangeSet) cset).getUniqueId();
		}
		return cset.getName();
	}

	/**
	 * Method to be called when a changeset is removed, for instance after a commit or from a menu
	 * entry.
	 *
	 * @param gucs
	 */
	public static void removeSavedChangeset(GroupedUncommittedChangeSet gucs) {
		/*
		 * Unfortunately, the IPreferenceStore API offers no way to remove a persisted setting. This
		 * to-do item is here to hint to a refactoring that would move changesets out of the
		 * preferences file. The current situation (as of 2017-01) is a progressive pollution of the
		 * prefs file with old changeset names, project files and changeset comments that cannot be
		 * removed.
		 */
		// TODO Use the new IEclipsePreferences API instead of the deprecated IPreferenceStore.
	}

	/**
	 * Builds the set of changesets from what was saved to the preference file.
	 *
	 * @param sets
	 *            (I/O) The set of changesets
	 */
	private void loadfromPreferences(Set<GroupedUncommittedChangeSet> sets) {
		loadingFromPrefs = true;
		try {
			IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
			// Get the list of *active* changesets. There are also relics in the file.
			String changesets = store.getString(KEY_CS_LIST);

			// Get the name of the default cset
			String defName = store.getString(KEY_CS_DEFAULT);

			// Rebuild the csets so as to have them available in "sets"
			if(!StringUtils.isEmpty(changesets)){
				String[] uniqueNames = changesets.split(MAPPINGS_SEPARATOR);
				for (String uniqueName : uniqueNames) {
					if(!StringUtils.isEmpty(uniqueName)) {
						String[] parts = uniqueName.split(CHANGESET_NAME_SEPARATOR);
						String name = decodeChangesetName(parts[0]);
						GroupedUncommittedChangeSet changeset = new GroupedUncommittedChangeSet(name, group);
						if (parts.length > 1) {
							// We've got a unique ID from the prefs file.
							changeset.setUniqueId(parts[1]);
						}
						sets.add(changeset);
					}
				}
			}
			for (GroupedUncommittedChangeSet changeSet : sets) {
				// Set the comment of each changeset that was read from the prefs. An important
				// corollary of this is that changesets saved to the prefs should have unique names:
				// csets will otherwise be confused and assignments of files to csets will be wrong.
				String comment = store.getString(KEY_CS_COMMENT_PREFIX + buildChangesetUniqueName(changeSet));
				if ("".equals(comment)) {
					// This accounts for legacy changeset names (which have no unique ID).
					comment = store.getString(KEY_CS_COMMENT_PREFIX + changeSet.getName());
				}
				changeSet.setComment(comment);

				// Identify the default set
				if(buildChangesetUniqueName(changeSet).equals(defName) || changeSet.getName().equals(defName)) {
					makeDefault(changeSet);
				}
			}
			if(projects == null){
				return;
			}
			//
			// Assign each file of each project available in the workspace to the appropriate cset
			for (IProject project : projects) {
				String filesStr = store.getString(KEY_FILES_PER_PROJECT_PREFIX + project.getName());
				if(StringUtils.isEmpty(filesStr)){
					continue;
				}
				Map<IFile, String> fileToChangeset = decode(filesStr, project);
				Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
				for (Entry<IFile, String> entry : entrySet) {
					String name = entry.getValue();
					GroupedUncommittedChangeSet changeset = getChangeset(name, sets);
					if(changeset == null){
						continue;
						//					changeset = new WorkingChangeSet(name, group);
						//					sets.add(changeset);
					}
					changeset.add(entry.getKey());
				}
			}
		} finally {
			loadingFromPrefs = false;
		}
	}

	private static GroupedUncommittedChangeSet getChangeset(String name, Set<GroupedUncommittedChangeSet> sets){
		for (GroupedUncommittedChangeSet set : sets) {
			if(name.equals(set.getName())){
				return set;
			}
		}
		return null;
	}

	/**
	 * @param filesStr non null string encoded like "(project_rel_path=changeset_name;)*"
	 * @param project
	 * @return
	 */
	private static Map<IFile, String> decode(String filesStr, IProject project) {
		Map<IFile, String> fileToChangeset = new HashMap<IFile, String>();
		String[] mappings = filesStr.split(MAPPINGS_SEPARATOR);
		for (String mapping : mappings) {
			if(StringUtils.isEmpty(mapping)){
				continue;
			}
			String[] pathAndName = mapping.split(PATH_NAME_SEPARATOR);
			if(pathAndName.length != 2){
				continue;
			}
			IFile file = project.getFile(new Path(pathAndName[0]));
			if(file != null) {
				fileToChangeset.put(file, pathAndName[1]);
			}
		}
		return fileToChangeset;
	}

	private static String encode(Map<IFile, String> fileToChangeset){
		Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
		StringBuilder sb = new StringBuilder();
		for (Entry<IFile, String> entry : entrySet) {
			IFile file = entry.getKey();
			String changesetName = entry.getValue();
			sb.append(file.getProjectRelativePath()).append(PATH_NAME_SEPARATOR).append(changesetName);
			sb.append(MAPPINGS_SEPARATOR);
		}
		return sb.toString();
	}

	public void setProjects(IProject[] projects) {
		this.projects = projects;
		loadSavedChangesets();
		if(projects != null){
			assignRemainingFiles();
		}

	}

	public IProject[] getProjects() {
		return projects;
	}

	public void makeDefault(GroupedUncommittedChangeSet set) {
		if(set == null || !group.getChangesets().contains(set)){
			return;
		}
		if(defaultChangeset != null) {
			defaultChangeset.setDefault(false);
		}
		defaultChangeset = set;
		defaultChangeset.setDefault(true);
		group.changesetChanged(set);
	}
}
