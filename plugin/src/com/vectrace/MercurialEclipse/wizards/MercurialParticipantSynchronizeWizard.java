/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberScopeManager;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope.RepositoryLocationMap;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 *
 */
public class MercurialParticipantSynchronizeWizard extends ParticipantSynchronizeWizard implements IWorkbenchWizard {

	private static final String SECTION_NAME = "MercurialParticipantSynchronizeWizard";

	private static final String PROP_HGROOT = "MercurialParticipantSynchronizeWizard.PROP_HG_ROOT";

	private final IWizard importWizard;
	private ConfigurationWizardMainPage repoPage;
	private SelectProjectsToSyncPage selectionPage;
	private IProject [] projects;

	private MercurialSynchronizeParticipant createdParticipant;

	public MercurialParticipantSynchronizeWizard() {
		projects = new IProject[0];
		importWizard = new CloneRepoWizard();
		IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(SECTION_NAME);
		if (section == null) {
			section = workbenchSettings.addNewSection(SECTION_NAME);
		}
		setDialogSettings(section);
	}

	@Override
	protected IWizard getImportWizard() {
		return importWizard;
	}

	@Override
	protected String getPageTitle() {
		return Messages.getString("MercurialParticipantSynchronizeWizard.pageTitle"); //$NON-NLS-1$
	}

	/**
	 * @return a list of selected managed projects, or all managed projects, if there was
	 * no selection. Never returns null, but can return empty list
	 * {@inheritDoc}
	 */
	@Override
	protected IProject[] getRootResources() {
		return selectionPage != null && selectionPage.isCreated()? selectionPage.getSelectedProjects() : projects;
	}

	@Override
	public void addPages() {
		// creates selection page, but only if there is something selected.
		// the point is, that the sync view starts the wizard with ZERO selection and
		// expects that the first page is somehow created (and it is created by the super class
		// but only if the getRootResources() returns something).
		// so in order to support it, we init the selection to ALL projects...
		if(projects.length == 0){
			projects = MercurialTeamProvider.getKnownHgProjects().toArray(new IProject[0]);
		}
		repoPage = createrepositoryConfigPage();
		super.addPages();
		addPage(repoPage);
	}

	@Override
	public IWizardPage getStartingPage() {
		return  selectionPage;
	}

	private ConfigurationWizardMainPage createrepositoryConfigPage() {
		ConfigurationWizardMainPage mainPage = new ConfigurationWizardMainPage(
				Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.name"), //$NON-NLS-1$
				Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.title"),
				MercurialEclipsePlugin.getImageDescriptor(Messages
						.getString("MercurialParticipantSynchronizeWizard.repositoryPage.image"))); //$NON-NLS-1$

		mainPage.setShowBundleButton(false);
		mainPage.setShowCredentials(true);
		mainPage.setDescription(Messages
				.getString("MercurialParticipantSynchronizeWizard.repositoryPage.description")); //$NON-NLS-1$
		mainPage.setDialogSettings(getDialogSettings());
		if(projects != null && projects.length > 0){
			Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(Arrays.asList(projects));
			if(byRoot.size() == 1){
				mainPage.setHgRoot(byRoot.keySet().iterator().next());
			}
		}
		return mainPage;
	}

	/**
	 * Get settings using location manager for the given root.
	 *
	 * @return properties object if all information needed to synchronize is available,
	 * @throws HgException If there's a missing property
	 * @see {@link #initProperties(HgRoot)}
	 */
	private List<Map<String, Object>> prepareDefaultSettings() throws HgException {
		IResource[] resources = getRootResources();
		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(Arrays.asList(resources));
		Set<HgRoot> roots = byRoot.keySet();
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (HgRoot hgRoot : roots) {
			Map<String, Object> pageProperties = propertiesToMap(initProperties(hgRoot));
			pageProperties.put(PROP_HGROOT, hgRoot);
			if(isValid(pageProperties, ConfigurationWizardMainPage.PROP_URL)) {
				result.add(pageProperties);
			} else {
				throw new HgException("Missing default synchronize URL for " + hgRoot.getName());
			}
		}
		return result;
	}

	private static boolean isValid(Map<String, Object> pageProperties, String key){
		Object value = pageProperties.get(key);
		return value instanceof String && ((String) value).trim().length() > 0;
	}

	/**
	 * Sets URL, USER, and PASSWORD for initial values by looking up saved values in the repo
	 * location manager for the given root.
	 *
	 * @param hgRoot
	 *            non null
	 * @return non null properties with possible repository data initialised from given root (may be
	 *         empty)
	 */
	static Properties initProperties(HgRoot hgRoot) {
		IHgRepositoryLocation repoLocation = MercurialEclipsePlugin.getRepoManager()
				.getDefaultRepoLocation(hgRoot);
		Properties properties = new Properties();
		if(repoLocation != null){
			if(repoLocation.getLocation() != null) {
				properties.put(ConfigurationWizardMainPage.PROP_URL, repoLocation.getLocation());
				if(repoLocation.getUser() != null) {
					properties.put(ConfigurationWizardMainPage.PROP_USER, repoLocation.getUser());
					if(repoLocation.getPassword() != null) {
						properties.put(ConfigurationWizardMainPage.PROP_PASSWORD, repoLocation.getPassword());
					}
				}
			}
		}
		return properties;
	}

	@Override
	public boolean performFinish() {
		boolean performFinish = true;
		if(repoPage != null) {
			repoPage.finish(new NullProgressMonitor());
			performFinish = super.performFinish();
		} else {
			// UI was not created, so we just need to continue with synchronization
			try {
				List<Map<String, Object>> properties = prepareDefaultSettings();
				if(properties != null && properties.size() > 0) {
					createdParticipant = createParticipant(properties, projects);
				} else {
					performFinish = false;
					throw new HgException("No properties found for synchronizing");
				}
			} catch (final HgException e) {
				MercurialEclipsePlugin.logWarning(e.getLocalizedMessage(), e);
				MercurialEclipsePlugin.getStandardDisplay().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openWarning(getShell(), "Could not synchronize",
								e.getMessage());

					}
				});
			}
		}
		if(performFinish && createdParticipant != null) {
			openSyncView(createdParticipant);
		}
		return performFinish;
	}

	public static void openSyncView(MercurialSynchronizeParticipant participant) {
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(
				new ISynchronizeParticipant[] { participant });
		// We don't know in which site to show progress because a participant could actually be
		// shown in multiple sites.
		participant.run(null /* no site */);
	}

	protected static MercurialSynchronizeParticipant createParticipant(
			List<Map<String, Object>> properties, IProject[] selectedProjects) throws HgException {
		RepositoryLocationMap byLocation = new RepositoryLocationMap(1);
		HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(Arrays.asList(selectedProjects));

		for (Map<String, Object> prop : properties) {
			String url = (String) prop.get(ConfigurationWizardMainPage.PROP_URL);
			String user = (String) prop.get(ConfigurationWizardMainPage.PROP_USER);
			String pass = (String) prop.get(ConfigurationWizardMainPage.PROP_PASSWORD);

			IHgRepositoryLocation repo;
			HgRoot hgRoot = (HgRoot) prop.get(PROP_HGROOT);
			List<IResource> curProjects = byRoot.get(hgRoot);

			repo = repoManager.getRepoLocation(url, user, pass);
			if (pass != null && user != null && curProjects.size() > 0) {
				if (!pass.equals(repo.getPassword())) {
					// At least 1 project exists, update location for that project
					repo = repoManager.updateRepoLocation(hgRoot, url, null, user, pass);
				}
			}

			byLocation.add(repo, hgRoot, curProjects.toArray(new IProject[curProjects.size()]));
			try {
				repoManager.addRepoLocation(hgRoot, repo);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		return createParticipant(byLocation);
	}

	public static MercurialSynchronizeParticipant createParticipant(IHgRepositoryLocation repo, HgRoot root,
			IProject[] selectedProjects) {
		RepositoryLocationMap map = new RepositoryLocationMap(1);
		map.add(repo, root, selectedProjects);
		return createParticipant(map);
	}

	protected static MercurialSynchronizeParticipant createParticipant(RepositoryLocationMap repos) {

		/*ISynchronizeParticipantReference participant = TeamUI.getSynchronizeManager().get(
				MercurialSynchronizeParticipant.class.getName(), repo.getLocation());*/
		RepositorySynchronizationScope scope = new RepositorySynchronizationScope(repos);

		// do not reuse participants which may already existing, not doing this would lead to the state where many sync. participants would listen
		// to resource changes and update/request same data/caches many times
		// we can not reuse participants because their scope can be different (if there are
		// more then one project under same repository)
		ISynchronizeParticipantReference[] synchronizeParticipants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference participant : synchronizeParticipants) {
			if (participant != null) {
				try {
					ISynchronizeParticipant participant2 = participant.getParticipant();

					if (participant2 instanceof MercurialSynchronizeParticipant) {
						TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[] { participant2 });
						while (Display.getCurrent().readAndDispatch()) {
							// give Team UI a chance to dispose the sync page, if any
						}
					}
				} catch (TeamException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}

		// Create a new participant for given repo/project pair
		MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(scope);
		IProject[] selectedProjects = scope.getProjects();
		ResourceMapping[] selectedMappings = new ResourceMapping[selectedProjects.length];
		for (int i = 0; i < selectedProjects.length; i++) {
			selectedMappings[i] = (ResourceMapping) selectedProjects[i].getAdapter(ResourceMapping.class);
		}
		HgSubscriberScopeManager manager = new HgSubscriberScopeManager(selectedMappings, subscriber);
		HgSubscriberMergeContext ctx = new HgSubscriberMergeContext(subscriber, manager);
		MercurialSynchronizeParticipant participant2 = new MercurialSynchronizeParticipant(ctx,	repos, scope);
		subscriber.setParticipant(participant2);
		return participant2;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		if(selection != null && !selection.isEmpty()){
			Object[] array = selection.toArray();
			Set<IProject> roots = new HashSet<IProject>();
			List<IProject> managed = MercurialTeamProvider.getKnownHgProjects();
			for (Object object : array) {
				IResource iResource = ResourceUtils.getResource(object);
				if (iResource != null) {
					IProject another = iResource.getProject();
					for (IProject project : managed) {
						if(project.equals(another)) {
							// add project as a root of resource
							roots.add(project);
						}
					}
					if(roots.isEmpty()) {
						roots.add((IProject) iResource);
					}
				}
			}
			projects = roots.toArray(new IProject[roots.size()]);
		}
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#createParticipant()
	 */
	@Override
	protected void createParticipant() {
		Map<String, Object> map = propertiesToMap(repoPage.getProperties());

		map.put(PROP_HGROOT, repoPage.getHgRoot());

		try {
			createdParticipant = createParticipant(Collections.singletonList(map),
					selectionPage.getSelectedProjects());
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			repoPage.setErrorMessage(e.getLocalizedMessage());
		}
	}

	private static Map<String, Object> propertiesToMap(Properties properties) {
		Map<String, Object> properties2 = new Hashtable<String, Object>();

		for (Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();
			properties2.put((String) entry.getKey(), entry.getValue());
		}
		return properties2;
	}

	@Override
	protected SelectProjectsToSyncPage createScopeSelectionPage() {
		selectionPage = new SelectProjectsToSyncPage(getRootResources());
		return selectionPage;
	}

}
