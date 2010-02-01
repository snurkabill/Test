/*******************************************************************************

 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Bastian Doetsch           - new qualified name for project sets
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - Fix encoding
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.ui.IPropertyListener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgRootContainer;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider {

	public static final String ID = "com.vectrace.MercurialEclipse.team.MercurialTeamProvider"; //$NON-NLS-1$

	/**
	 * Qualified Name for the repository a project was cloned from.
	 */
	public static final QualifiedName QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY = new QualifiedName(ID
			+ ".projectSourceRepository", //$NON-NLS-1$
			"MercurialEclipseProjectSourceRepository"); //$NON-NLS-1$

	public static final QualifiedName QUALIFIED_NAME_DEFAULT_REVISION_LIMIT = new QualifiedName(ID
			+ ".defaultRevisionLimit", "defaultRevisionLimit"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * The value is one element array, which single element is NON null if the project has
	 * a hg root
	 */
	private static final Map<IProject, HgRoot[]> HG_ROOTS = new HashMap<IProject, HgRoot[]>();

	/** key is project, value is the *current* branch */
	private static final Map<IProject, String> BRANCH_MAP = new ConcurrentHashMap<IProject, String>();

	private MercurialHistoryProvider FileHistoryProvider;

	private static final ListenerList branchListeners = new ListenerList(ListenerList.IDENTITY);

	/** @see #getRuleFactory() */
	private IResourceRuleFactory resourceRuleFactory;

	public MercurialTeamProvider() {
		super();
	}

	public static List<IProject> getKnownHgProjects(){
		List<IProject> projects = new ArrayList<IProject>();
		IProject[] iProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : iProjects) {
			HgRoot hgRoot = hasHgRoot(project);
			if(hgRoot != null){
				projects.add(project);
			}
		}
		return projects;
	}

	public static Set<IProject> getKnownHgProjects(HgRoot hgRoot){
		getKnownHgProjects();
		Set<IProject> projects = new HashSet<IProject>();
		Map<IProject, HgRoot[]> rootMap = new HashMap<IProject, HgRoot[]>(HG_ROOTS);
		Set<Entry<IProject,HgRoot[]>> entrySet = rootMap.entrySet();
		for (Entry<IProject, HgRoot[]> entry : entrySet) {
			if(entry.getValue()[0] == hgRoot){
				IProject project = entry.getKey();
				if(project.exists()) {
					projects.add(project);
				}
			}
		}
		return projects;
	}

	@Override
	public void configureProject() throws CoreException {
		IProject project = getProject();
		HgRoot hgRoot = getHgRoot(project);
		HG_ROOTS.put(project, new HgRoot[]{hgRoot});
		project.refreshLocal(IResource.DEPTH_ONE, null);

		// try to find .hg directory to set it as private member
		IResource hgDir = project.findMember(".hg"); //$NON-NLS-1$
		if (hgDir != null && hgDir.exists()) {
			hgDir.setTeamPrivateMember(true);
			hgDir.setDerived(true);
		}
	}

	public void deconfigure() throws CoreException {
		IProject project = getProject();
		Assert.isNotNull(project);
		// cleanup
		HG_ROOTS.put(project, new HgRoot[]{null});
		BRANCH_MAP.remove(project);
//		project.setPersistentProperty(ResourceProperties.HG_ROOT, null);
		project.setSessionProperty(ResourceProperties.HG_BRANCH, null);
		HgStatusClient.clearMergeStatus(project);
	}

	/**
	 * Checks if the given project is controlled by MercurialEclipse.
	 *
	 * @return true, if MercurialEclipse provides team functions to this
	 *         project, false otherwise.
	 */
	public static boolean isHgTeamProviderFor(IProject project){
		Assert.isNotNull(project);
		if(!project.isOpen()){
			return false;
		}
		HgRoot[] result = HG_ROOTS.get(project);
		if(result == null || result[0] == null){
			return hasHgRoot(project) != null;
		}
		return true;
	}

	public static void addBranchListener(IPropertyListener listener){
		branchListeners.add(listener);
	}

	public static void removeBranchListener(IPropertyListener listener){
		branchListeners.remove(listener);
	}

	/**
	 * Determines if the resources hg root is known. If it isn't known, Mercurial is called to determine it. The result
	 * will be saved as project session property on the resource's project with the qualified name
	 * {@link ResourceProperties#HG_ROOT}.
	 *
	 * This property can be used to create a {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for, not null
	 * @return the canonical file path of the HgRoot
	 * @throws HgException
	 */
	private static HgRoot getAndStoreHgRoot(IResource resource) throws HgException {
		IProject project = resource.getProject();
		if (project == null || !resource.exists()) {
			return AbstractClient.getHgRoot(resource);
		}
		HgRoot hgRoot;
		try {
			HgRoot[] rootElt = HG_ROOTS.get(project);
			if(rootElt == null){
				rootElt = new HgRoot[1];
				HG_ROOTS.put(project, rootElt);
			}
			hgRoot = rootElt[0]; // (HgRoot) project.getSessionProperty(ResourceProperties.HG_ROOT);
			if (hgRoot == null) {
				hgRoot = AbstractClient.getHgRoot(resource);
				rootElt[0] = hgRoot;
				setRepositoryEncoding(project, hgRoot);
				// project.setSessionProperty(ResourceProperties.HG_ROOT, hgRoot);
			}
		} catch (CoreException e) {
			throw new HgException(e);
		}
		return hgRoot;
	}

	private static void setRepositoryEncoding(IProject project, HgRoot hgRoot) throws CoreException {
		if (project != null) {
			hgRoot.setEncoding(Charset.forName(MercurialEclipsePlugin.getDefaultEncoding(project)));
		}
	}

	private static HgRoot getHgRootFile(File file) throws HgException {
		return HgRootClient.getHgRoot(file);
	}

	/**
	 * Determines if the resources hg root is known. If it isn't known, Mercurial is called to determine it. The result
	 * will be saved as project persistent property on the resource's project with the qualified name
	 * {@link ResourceProperties#HG_ROOT}.
	 *
	 * This property can be used to create a {@link java.io.File}.
	 *
	 * @param file
	 *            the {@link java.io.File} to get the hg root for, non null
	 * @return the canonical file path of the HgRoot or null if no resource could be found in workspace that matches the
	 *         file
	 * @throws HgException
	 *             if no hg root was found or a critical error occurred.
	 */
	private static HgRoot getAndStoreHgRoot(File file) throws CoreException {
		IResource resource = ResourceUtils.convert(file);
		if (resource != null) {
			return getAndStoreHgRoot(resource);
		}
		return getHgRootFile(file);
	}

	/**
	 * Gets the resource hgrc as a {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hgrc for, not null
	 * @return the {@link java.io.File} referencing the hgrc file, <code>null</code> if it doesn't exist.
	 * @throws HgException
	 *             if an error occured (e.g., no root could be found).
	 */
	public static File getHgRootConfig(IResource resource) throws HgException {
		HgRoot hgRoot = getHgRoot(resource);
		return hgRoot.getConfig();
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for, not null
	 * @return the {@link java.io.File} referencing the hg root directory
	 * @throws HgException
	 *             if an error occurred (e.g. no root could be found)
	 */
	public static HgRoot getHgRoot(IResource resource) throws HgException {
		if(resource instanceof HgRootContainer){
			HgRootContainer rootContainer = (HgRootContainer) resource;
			return rootContainer.getHgRoot();
		}
		try {
			return getAndStoreHgRoot(resource);
		} catch (CoreException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for
	 * @return the {@link java.io.File} referencing the hg root directory, or null if no
	 * hg root can't be found
	 */
	public static HgRoot hasHgRoot(IResource resource) {
		if(resource == null){
			return null;
		}
		IProject project = resource.getProject();
		HgRoot[] result = HG_ROOTS.get(project);
		if(result != null && result[0] != null){
			return result[0];
		}
		HgRoot hgRoot = HgRootClient.hasHgRoot(ResourceUtils.getFileHandle(resource));
		if(hgRoot != null && (RepositoryProvider.getProvider(project, ID) != null)) {
			HG_ROOTS.put(project, new HgRoot[]{hgRoot});
		}
		return hgRoot;
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param file
	 *            a {@link java.io.File}, not null
	 * @return the file object of the root.
	 * @throws CoreException
	 */
	public static HgRoot getHgRoot(File file) throws CoreException {
		return getAndStoreHgRoot(file);
	}

	/**
	 * @param res non null
	 * @return current resource branch, never null.
	 */
	public static String getCurrentBranch(IResource res){
		Assert.isNotNull(res);
		IProject project = res.getProject();
		String branch = BRANCH_MAP.get(project);
		if(branch == null){
			try {
				branch = (String) project.getSessionProperty(ResourceProperties.HG_BRANCH);
				branch = branch == null? Branch.DEFAULT : branch;
				BRANCH_MAP.put(project, branch);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return Branch.DEFAULT;
			}
		}
		return branch;
	}

	public static void setCurrentBranch(String branch, IProject project){
		String oldBranch = null;
		if(branch != null){
			oldBranch = BRANCH_MAP.put(project, branch);
		} else {
			BRANCH_MAP.remove(project);
		}
		try {
			if(project.isAccessible()) {
				project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);
			}
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if(branch != null && !Branch.same(branch, oldBranch)){
			Object[] listeners = branchListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				IPropertyListener listener = (IPropertyListener) listeners[i];
				listener.propertyChanged(project, 0);
			}
		}
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return new HgMoveDeleteHook();
	}

	@Override
	public IFileHistoryProvider getFileHistoryProvider() {
		if (FileHistoryProvider == null) {
			FileHistoryProvider = new MercurialHistoryProvider();
		}
		// System.out.println("getFileHistoryProvider()");
		return FileHistoryProvider;
	}

	@Override
	public boolean canHandleLinkedResources() {
		return true;
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		return canHandleLinkedResources();
	}

	/**
	 * Overrides the default pessimistic resource rule factory which locks the workspace for all
	 * operations. This causes problems when opening a project. This method returns the default
	 * non-pessimistic resource rule factory which locks on a finer level.
	 */
	@Override
	public IResourceRuleFactory getRuleFactory() {
		if (resourceRuleFactory == null) {
			resourceRuleFactory = new ResourceRuleFactory() {};
		}
		return resourceRuleFactory;
	}
}
