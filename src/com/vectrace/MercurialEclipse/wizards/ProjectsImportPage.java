/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc - extensive changes to allow importing of Archive Files
 *     Philippe Ombredanne (pombredanne@nexb.com)
 *          - Bug 101180 [Import/Export] Import Existing Project into Workspace default widget is back button , should be text field
 *     Martin Oberhuber (martin.oberhuber@windriver.com)
 *          - Bug 187318[Wizards] "Import Existing Project" loops forever with cyclic symbolic links
 *     Remy Chi Jian Suen  (remy.suen@gmail.com)
 *          - Bug 210568 [Import/Export] [Import/Export] - Refresh button does not update list of projects
 *     Andrei Loskutov (Intland) - bug fixes
 *          - Stripped down all what we do not need in Mercurial
 *******************************************************************************/

package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.eclipse.ui.statushandlers.StatusManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * The ProjectsImportPage is the page that allows the user to import
 * projects from a particular location.
 *
 * see org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage
 */
public class ProjectsImportPage extends WizardPage implements IOverwriteQuery {

	private static final String DESCRIPTION = "Select projects to import into the workspace. " +
			"Checked projects will be imported. Double click on project to change project name.";

	/**
	 * The name of the folder containing metadata information for the workspace.
	 */
	public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

	private final class ProjectLabelProvider extends LabelProvider implements IColorProvider {

		@Override
		public String getText(Object element) {
			return ((ProjectRecord) element).getProjectLabel();
		}

		public Color getBackground(Object element) {
			return null;
		}

		public Color getForeground(Object element) {
			ProjectRecord projectRecord = (ProjectRecord) element;
			if(projectRecord.hasConflicts) {
				return getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
			}
			return null;
		}
	}

	private static class ProjectRecord {

		private final File projectSystemFile;
		final IProjectDescription description;

		String projectName;

		boolean hasConflicts;

		/**
		 * Create a record for a project based on the info in the file.
		 *
		 * @param file
		 */
		ProjectRecord(File file) {
			projectSystemFile = file;
			description = createDescription();
			projectName = description.getName();
		}

		/**
		 * Creates project description based on the projectFile.
		 * @return never null
		 */
		private IProjectDescription createDescription() {
			IProjectDescription tmpDescription;
			String tmpProjectName;
			// If we don't have the project name try again
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (projectSystemFile.isDirectory()) {
				tmpProjectName = projectSystemFile.getName();
				tmpDescription = workspace.newProjectDescription(tmpProjectName);
				// check if the directory matches the default location?
				// if yes, should NOT set location, as in the "else" branch below
				File wsRoot = workspace.getRoot().getLocation().toFile();
				if(!wsRoot.equals(projectSystemFile) && !wsRoot.equals(projectSystemFile.getParentFile())) {
					tmpDescription.setLocation(new Path(projectSystemFile.getAbsolutePath()));
				}
			} else {
				IPath path = new Path(projectSystemFile.getPath());
				tmpProjectName = path.segment(path.segmentCount() - 2);
				try {
					tmpDescription = workspace.loadProjectDescription(path);
					if (isDefaultLocation(path)){
						// for some strange reasons (bug?) Eclipse disallows to create
						// projects inside the workspace IF the project description has
						// a location attribute set...
						tmpDescription.setLocation(null);
					}
				} catch (CoreException e) {
					// no good, couldn't load
					tmpDescription = workspace.newProjectDescription(tmpProjectName);
				}
			}
			return tmpDescription;
		}

		/**
		 * Returns whether the given project description file path is in the
		 * default location for a project
		 *
		 * @param path
		 * 		The path to examine
		 * @return Whether the given path is the default location for a project
		 */
		private boolean isDefaultLocation(IPath path) {
			// The project description file must at least be within the project,
			// which is within the workspace location
			if (path.segmentCount() < 2) {
				return false;
			}
			return path.removeLastSegments(2).toFile().equals(
					Platform.getLocation().toFile());
		}

		/**
		 * Get the name of the project
		 *
		 * @return String
		 */
		public String getProjectName() {
			return projectName;
		}

		public File getDataDir(){
			return projectSystemFile.isDirectory() ? projectSystemFile
					: projectSystemFile.getParentFile();
		}

		/**
		 * Gets the label to be used when rendering this project record in the
		 * UI.
		 *
		 * @return String the label
		 */
		public String getProjectLabel() {
			String path = projectSystemFile.isDirectory() ? projectSystemFile.getAbsolutePath()
					: projectSystemFile.getParent();

			return projectName + " (" + path + ")";
		}

	}

	private CheckboxTreeViewer projectsList;

	private ProjectRecord[] selectedProjects = new ProjectRecord[0];

	private IProject[] wsProjects;

	// The last selected path to minimize searches
	private String lastPath;

	// The last time that the file or folder at the selected path was modified
	// to mimize searches
	private long lastModified;

	private WorkingSetGroup workingSetGroup;
	private List<IProject> createdProjects;
	private String destinationDirectory;

	private File repositoryRoot;

	/**
	 * Creates a new project creation wizard page.
	 */
	public ProjectsImportPage(String pageName) {
		super(pageName);

		setPageComplete(false);
		setTitle("Import Projects");
		setDescription(DESCRIPTION);
	}

	public void createControl(Composite parent) {

		initializeDialogUnits(parent);

		Composite workArea = new Composite(parent, SWT.NONE);
		setControl(workArea);

		workArea.setLayout(new GridLayout());
		workArea.setLayoutData(new GridData(GridData.FILL_BOTH
				| GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

		createProjectsList(workArea);
//        createOptionsArea(workArea);
		createWorkingSetGroup(workArea);
		restoreWidgetValues();
		Dialog.applyDialogFont(workArea);

	}

	/**
	 * Create the area with the extra options.
	 */
//    private void createOptionsArea(Composite workArea) {
//        Composite optionsGroup = new Composite(workArea, SWT.NONE);
//        optionsGroup.setLayout(new GridLayout());
//        optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//        copyCheckbox = new Button(optionsGroup, SWT.CHECK);
//        copyCheckbox.setText("&Copy projects into workspace");
//        copyCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        copyCheckbox.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                copyFiles = copyCheckbox.getSelection();
//            }
//        });
//    }

	private void createWorkingSetGroup(Composite workArea) {
		String[] workingSetIds = new String[] {"org.eclipse.ui.resourceWorkingSetPage",  //$NON-NLS-1$
		"org.eclipse.jdt.ui.JavaWorkingSetPage"};  //$NON-NLS-1$
		workingSetGroup = new WorkingSetGroup(workArea, null, workingSetIds);
	}

	/**
	 * Create the checkbox list for the found projects.
	 */
	private void createProjectsList(Composite workArea) {

		Label title = new Label(workArea, SWT.NONE);
		title.setText("&Projects:");

		Composite listComposite = new Composite(workArea, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = false;
		listComposite.setLayout(layout);

		listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

		projectsList = new CheckboxTreeViewer(listComposite, SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = convertWidthInCharsToPixels(25);
		gridData.heightHint = convertHeightInCharsToPixels(10);
		projectsList.getControl().setLayoutData(gridData);
		projectsList.setContentProvider(new ITreeContentProvider() {
			public Object[] getChildren(Object parentElement) {
				return null;
			}
			public Object[] getElements(Object inputElement) {
				return getProjectRecords();
			}
			public boolean hasChildren(Object element) {
				return false;
			}
			public Object getParent(Object element) {
				return null;
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}
		});

		final ProjectLabelProvider labelProvider = new ProjectLabelProvider();
		projectsList.setLabelProvider(labelProvider);

		projectsList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				ProjectRecord element = (ProjectRecord) event.getElement();
				if(element.hasConflicts) {
					projectsList.setChecked(element, false);
				}
				setPageComplete(projectsList.getCheckedElements().length > 0);
			}
		});

		projectsList.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object element = selection.getFirstElement();
				if(!(element instanceof ProjectRecord)){
					return;
				}
				final ProjectRecord record = (ProjectRecord) element;
				IInputValidator validator = new IInputValidator() {
					public String isValid(String newText) {
						ProjectRecord[] projects = selectedProjects;
						for (ProjectRecord pr : projects) {
							if(pr != record && pr.projectName.equals(newText)){
								return "A project with same name already exists";
							}
						}
						if(isProjectInWorkspace(newText)){
							return "A project with same name already exists";
						}
						if (newText.length() == 0) {
							return "Project name cannot be empty";
						}
						return null;
					}
				};
				InputDialog input = new InputDialog(getShell(), "Clone repository",
						"Change project name for imported project",
						record.projectName, validator);
				int ok = input.open();
				if(ok == Window.OK){
					record.projectName = input.getValue();
					record.hasConflicts = isProjectInWorkspace(record.projectName);
				}
				validateSelectedProjects();
				projectsList.refresh(true);
			}
		});

		projectsList.setInput(this);
		projectsList.setComparator(new ViewerComparator());
		createSelectionButtons(listComposite);
	}

	/**
	 * Create the selection buttons in the listComposite.
	 *
	 * @param listComposite
	 */
	private void createSelectionButtons(Composite listComposite) {
		Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonsComposite.setLayout(layout);

		buttonsComposite.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING));

		Button selectAll = new Button(buttonsComposite, SWT.PUSH);
		selectAll.setText("&Select All");
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateSelectedProjects();
				projectsList.refresh(true);
			}
		});
		Dialog.applyDialogFont(selectAll);
		setButtonLayoutData(selectAll);

		Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
		deselectAll.setText("&Deselect All");
		deselectAll.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				projectsList.setCheckedElements(new Object[0]);
				setPageComplete(false);
			}
		});
		Dialog.applyDialogFont(deselectAll);
		setButtonLayoutData(deselectAll);

		Button refresh = new Button(buttonsComposite, SWT.PUSH);
		refresh.setText("R&efresh");
		refresh.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateProjectsList(destinationDirectory);
			}
		});
		Dialog.applyDialogFont(refresh);
		setButtonLayoutData(refresh);
	}

	/**
	 * Set the focus on path fields when page becomes visible.
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			IWizardPage previousPage = getPreviousPage();
			if(previousPage instanceof ClonePage){
				ClonePage page = (ClonePage) previousPage;
				File directory = page.getDestinationDirectory();
				setInitialSelection(directory);
			}
		}
	}

	/**
	 * Update the list of projects based on path.
	 *
	 * @param path
	 */
	private void updateProjectsList(final String path) {
		// on an empty path empty selectedProjects
		if (path == null || path.length() == 0) {
			setMessage(DESCRIPTION);
			selectedProjects = new ProjectRecord[0];
			projectsList.refresh(true);
			projectsList.setCheckedElements(selectedProjects);
			setPageComplete(projectsList.getCheckedElements().length > 0);
			lastPath = path;
			return;
		}

		repositoryRoot = new File(path);
		long modified = repositoryRoot.lastModified();
		if (path.equals(lastPath) && lastModified == modified) {
			// since the file/folder was not modified and the path did not
			// change, no refreshing is required
			return;
		}

		lastPath = path;
		lastModified = modified;

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {

					monitor.beginTask("Searching for projects", 100);
					selectedProjects = new ProjectRecord[0];
					Collection<File> files = new ArrayList<File>();
					monitor.worked(10);
					if (repositoryRoot.isDirectory()) {

						if (!collectProjectFilesFromDirectory(files, repositoryRoot,
								null, monitor)) {
							return;
						}
						Iterator<File> filesIterator = files.iterator();
						selectedProjects = new ProjectRecord[files.size()];
						int index = 0;
						monitor.worked(50);
						monitor.subTask("Processing results");
						if(files.isEmpty()){
							selectedProjects = new ProjectRecord[1];
							selectedProjects[0] = new ProjectRecord(new File(
									destinationDirectory));
						} else {
							while (filesIterator.hasNext()) {
								File file = filesIterator.next();
								selectedProjects[index] = new ProjectRecord(file);
								index++;
							}
						}
					} else {
						monitor.worked(60);
					}
					monitor.done();
				}

			});
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.logError(e.getMessage(), e.getCause());
		} catch (InterruptedException e) {
			// Nothing to do if the user interrupts.
		}

		projectsList.refresh(false);
		validateSelectedProjects();
		projectsList.refresh(true);
	}

	/**
	 * Get the title for an error dialog. Subclasses should override.
	 */
	protected String getErrorDialogTitle() {
		return "Internal error";
	}

	/**
	 * Collect the list of .project files that are under directory into files.
	 *
	 * @param files
	 * @param directory
	 * @param directoriesVisited
	 * 		Set of canonical paths of directories, used as recursion guard
	 * @param monitor
	 * 		The monitor to report to
	 * @return boolean <code>true</code> if the operation was completed.
	 */
	private boolean collectProjectFilesFromDirectory(Collection<File> files,
			File directory, Set<String> directoriesVisited, IProgressMonitor monitor) {

		if (monitor.isCanceled() || directory.getName().equals(".hg")) {
			return false;
		}
		monitor.subTask("Checking: " + directory.getPath());
		File[] contents = directory.listFiles();
		if (contents == null) {
			return false;
		}

		// Initialize recursion guard for recursive symbolic links
		if (directoriesVisited == null) {
			directoriesVisited = new HashSet<String>();
			try {
				directoriesVisited.add(directory.getCanonicalPath());
			} catch (IOException exception) {
				StatusManager.getManager().handle(
						MercurialEclipsePlugin.createStatus(exception
								.getLocalizedMessage(), IStatus.ERROR, IStatus.ERROR, exception));
			}
		}

		// first look for project description files
		final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
		for (int i = 0; i < contents.length; i++) {
			File file = contents[i];
			if (file.isFile() && file.getName().equals(dotProject)) {
				files.add(file);
				// don't search sub-directories since we can't have nested
				// projects
				return true;
			}
		}
		// no project description found, so recurse into sub-directories
		for (int i = 0; i < contents.length; i++) {
			if (contents[i].isDirectory()) {
				if (!contents[i].getName().equals(METADATA_FOLDER)) {
					try {
						String canonicalPath = contents[i].getCanonicalPath();
						if (!directoriesVisited.add(canonicalPath)) {
							// already been here --> do not recurse
							continue;
						}
					} catch (IOException exception) {
						StatusManager.getManager().handle(
								MercurialEclipsePlugin.createStatus(exception
										.getLocalizedMessage(), IStatus.ERROR, IStatus.ERROR, exception));

					}
					collectProjectFilesFromDirectory(files, contents[i],
							directoriesVisited, monitor);
				}
			}
		}
		return true;
	}

	/**
	 * Create the selected projects
	 *
	 * @return boolean <code>true</code> if all project creations were
	 * 	successful.
	 */
	public boolean createProjects() {
		saveWidgetValues();

		final Object[] selected = projectsList.getCheckedElements();
		createdProjects = new ArrayList<IProject>();
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("", selected.length); //$NON-NLS-1$
					if (monitor.isCanceled()) {
						throw new OperationCanceledException();
					}
					for (int i = 0; i < selected.length; i++) {
						createExistingProject((ProjectRecord) selected[i],
								new SubProgressMonitor(monitor, 1));
					}
				} finally {
					monitor.done();
				}
			}
		};
		// run the new project creation operation
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			// one of the steps resulted in a core exception
			Throwable t = e.getTargetException();
			String message = "Creation Problems";
			IStatus status;
			if (t instanceof CoreException) {
				status = ((CoreException) t).getStatus();
			} else {
				status = MercurialEclipsePlugin.createStatus(message, ERROR, IStatus.ERROR, t);
			}
			MercurialEclipsePlugin.logError(t);
			ErrorDialog.openError(getShell(), message, null, status);
			return false;
		}

		// Adds the projects to the working sets
		addToWorkingSets();

		return true;
	}

	private void addToWorkingSets() {

		IWorkingSet[] selectedWorkingSets = workingSetGroup.getSelectedWorkingSets();
		if(selectedWorkingSets == null || selectedWorkingSets.length == 0) {
			return; // no Working set is selected
		}
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		for (Iterator<IProject> i = createdProjects.iterator(); i.hasNext();) {
			IProject project = i.next();
			workingSetManager.addToWorkingSets(project, selectedWorkingSets);
		}
	}

	/**
	 * Create the project described in record. If it is successful return true.
	 *
	 * @param record
	 * @return boolean <code>true</code> if successful
	 * @throws InterruptedException
	 */
	private boolean createExistingProject(final ProjectRecord record,
			IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		String projectName = record.getProjectName();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		createdProjects.add(project);

		// See issue 10412: in case the clone is inside the workspace AND the clone directory name doesn't match
		// the selected project name, we have to rename cloned directory, otherwise
		// Eclipse will create a new EMPTY project
		if (record.description.getLocationURI() == null && !projectName.equals(record.getDataDir().getName())) {
			File newRoot = new File(record.getDataDir().getParentFile(), projectName);
			if(!newRoot.exists()) {
				// we CAN use renameTo here because src and dest folders are on same file system
				// renameTo does NOT work on different file systems
				boolean renamed = record.getDataDir().renameTo(newRoot);
				if(!renamed){
					// XXX report issue
					return false;
				}
			} else {
				// XXX report issue
				return false;
			}
		}
		record.description.setName(projectName);


		try {
			monitor.beginTask("Creating Projects", 100);
			project.create(record.description, new SubProgressMonitor(monitor, 30));
			project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}

		try {
			registerWithTeamProvider(project, monitor);
		} catch (CoreException e) {
			try {
				project.delete(true, new SubProgressMonitor(monitor, 30));
			} catch (CoreException ex) {
				throw new InvocationTargetException(ex);
			}
			throw new InvocationTargetException(e);
		}

		return true;
	}

	private void registerWithTeamProvider(IProject p, IProgressMonitor monitor) throws CoreException {
		IWizard wizard = getWizard();
		if(!(wizard instanceof CloneRepoWizard)){
			return;
		}
		HgRepositoryLocation repo = ((CloneRepoWizard) wizard).getRepository();
		// Register the project with Team. This will bring all the
		// files that we cloned into the project.
		monitor
		.subTask(Messages
				.getString("CloneRepoWizard.subTask.registeringProject1") + " " + p.getName() //$NON-NLS-1$
				+ Messages
				.getString("CloneRepoWizard.subTaskRegisteringProject2")); //$NON-NLS-1$
		RepositoryProvider.map(p, MercurialTeamProvider.class.getName());
		monitor.worked(1);

		// It appears good. Stash the repo location.
		monitor
		.subTask(Messages
				.getString("CloneRepoWizard.subTask.addingRepository.1") + " " + repo //$NON-NLS-1$
				+ Messages
				.getString("CloneRepoWizard.subTask.addingRepository.2")); //$NON-NLS-1$
		MercurialEclipsePlugin.getRepoManager().addRepoLocation(p, repo);
		monitor.worked(1);
	}

	/**
	 * The <code>WizardDataTransfer</code> implementation of this
	 * <code>IOverwriteQuery</code> method asks the user whether the existing
	 * resource at the given path should be overwritten.
	 *
	 * @param pathString
	 * @return the user's reply: one of <code>"YES"</code>, <code>"NO"</code>,
	 * 	<code>"ALL"</code>, or <code>"CANCEL"</code>
	 */
	public String queryOverwrite(String pathString) {

		Path path = new Path(pathString);

		String messageString;
		// Break the message up if there is a file name and a directory
		// and there are at least 2 segments.
		if (path.getFileExtension() == null || path.segmentCount() < 2) {
			messageString = "'" + pathString + "' already exists.  Would you like to overwrite it?";
		} else {
			messageString = "Overwrite '" + path.lastSegment() + "' in folder '"
			+ path.removeLastSegments(1).toOSString() + "'?";
		}

		final MessageDialog dialog = new MessageDialog(getContainer()
				.getShell(), "Question", null,
				messageString, MessageDialog.QUESTION, new String[] {
			IDialogConstants.YES_LABEL,
			IDialogConstants.YES_TO_ALL_LABEL,
			IDialogConstants.NO_LABEL,
			IDialogConstants.NO_TO_ALL_LABEL,
			IDialogConstants.CANCEL_LABEL }, 0) {
			@Override
			protected int getShellStyle() {
				// TODO add  "| SWT.SHEET" flag as soon as we drop Eclipse 3.4 support
				return super.getShellStyle()/* | SWT.SHEET*/;
			}
		};
		String[] response = new String[] { YES, ALL, NO, NO_ALL, CANCEL };
		// run in syncExec because callback is from an operation,
		// which is probably not running in the UI thread.
		getControl().getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		return dialog.getReturnCode() < 0 ? CANCEL : response[dialog.getReturnCode()];
	}


	/**
	 * Method used for test suite.
	 *
	 * @return CheckboxTreeViewer the viewer containing all the projects found
	 */
	public CheckboxTreeViewer getProjectsList() {
		return projectsList;
	}

	/**
	 * Retrieve all the projects in the current workspace.
	 *
	 * @return IProject[] array of IProject in the current workspace
	 */
	private IProject[] getProjectsInWorkspace() {
		if (wsProjects == null) {
			wsProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		}
		return wsProjects;
	}

	/**
	 * Get the array of  project records that can be imported from the
	 * source workspace or archive, selected by the user.
	 *
	 * @return ProjectRecord[] array of projects that can be imported into the
	 * 	workspace
	 */
	private ProjectRecord[] getProjectRecords() {
		List<ProjectRecord> projectRecords = new ArrayList<ProjectRecord>();
		for (int i = 0; i < selectedProjects.length; i++) {
			projectRecords.add(selectedProjects[i]);
		}
		return projectRecords.toArray(new ProjectRecord[projectRecords.size()]);
	}

	/**
	 * Determine if the project with the given name is in the current workspace.
	 *
	 * @param projectName
	 * 		String the project name to check
	 * @return boolean true if the project with the given name is in this
	 * 	workspace
	 */
	private boolean isProjectInWorkspace(String projectName) {
		if (projectName == null) {
			return false;
		}
		IProject[] workspaceProjects = getProjectsInWorkspace();
		for (int i = 0; i < workspaceProjects.length; i++) {
			if (projectName.equalsIgnoreCase(workspaceProjects[i].getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Use the dialog store to restore widget values to the values that they
	 * held last time this wizard was used to completion, or alternatively,
	 * if an initial path is specified, use it to select values.
	 *
	 * Method declared public only for use of tests.
	 */
	public void restoreWidgetValues() {
		// First, check to see if we have resore settings, and
		// take care of the checkbox
		// IDialogSettings settings = getDialogSettings();
	}

	/**
	 * Since Finish was pressed, write widget values to the dialog store so that
	 * they will persist into the next invocation of this wizard page.
	 *
	 * Method declared public only for use of tests.
	 */
	public void saveWidgetValues() {
		// IDialogSettings settings = getDialogSettings();
	}


	public void setInitialSelection(File destinationDirectory) {
		this.destinationDirectory = destinationDirectory.getAbsolutePath();
		updateProjectsList(this.destinationDirectory);
	}

	/**
	 * If a project with the
	 * same name exists in both the source workspace and the current workspace,
	 * then the hasConflicts flag would be set on that project record.
	 */
	private void validateSelectedProjects() {
		boolean hasConflict = false;
		for (int i = 0; i < selectedProjects.length; i++) {
			ProjectRecord record = selectedProjects[i];
			if (isProjectInWorkspace(record.getProjectName())) {
				record.hasConflicts = true;
				projectsList.setGrayed(record, true);
				projectsList.setChecked(record, false);
				hasConflict = true;
			} else {
				record.hasConflicts = false;
				projectsList.setChecked(record, true);
				projectsList.setGrayed(record, false);
			}
		}
		if(!hasConflict) {
			setMessage(null, WARNING);
			setDescription(DESCRIPTION);
		} else {
			setMessage(
					"Some projects cannot be imported because they already exist in the workspace. " +
					"Double click on project to change project name.",
					WARNING);
		}
		if(selectedProjects.length == 0) {
			setMessage("No projects are selected to import", WARNING);
		}
		setPageComplete(selectedProjects.length > 0);
	}

}