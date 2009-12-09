/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch	         - saving repository to project-specific repos
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgPushPullClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgSvnClient;
import com.vectrace.MercurialEclipse.commands.extensions.forest.HgFpushPullClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author zingo
 *
 */
public class PushRepoWizard extends HgWizard {

    private IProject project;
    private OutgoingPage outgoingPage;

    private PushRepoWizard() {
        super(Messages.getString("PushRepoWizard.title")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    public PushRepoWizard(IResource resource) {
        this();
        this.project = resource.getProject();
    }

    @Override
    public void addPages() {
        super.addPages();
        PushPullPage myPage = new PushRepoPage(
                Messages.getString("PushRepoWizard.pushRepoPage.name"), //$NON-NLS-1$
                Messages.getString("PushRepoWizard.pushRepoPage.title"), null, project); //$NON-NLS-1$
        initPage(Messages.getString("PushRepoWizard.pushRepoPage.description"), //$NON-NLS-1$
                myPage);
        myPage.setShowCredentials(true);
        page = myPage;
        addPage(page);
        outgoingPage = new OutgoingPage("OutgoingPage"); //$NON-NLS-1$
        initPage(outgoingPage.getDescription(), outgoingPage);
        outgoingPage.setProject(project);
        addPage(outgoingPage);
    }

    @Override
    public boolean performFinish() {
        super.performFinish();
        try {
            Properties props = page.getProperties();
            HgRepositoryLocation repo = MercurialEclipsePlugin.getRepoManager()
                    .fromProperties(props);

            // Check that this project exist.
            if (project.getLocation() == null) {
                String msg = Messages.getString("PushRepoWizard.project") + project.getName() //$NON-NLS-1$
                        + Messages.getString("PushRepoWizard.notExists"); //$NON-NLS-1$
                MercurialEclipsePlugin.logError(msg, null);
                // System.out.println( string);
                return false;
            }

            PushPullPage pushRepoPage = (PushPullPage) page;

            int timeout = HgClients.getTimeOut(MercurialPreferenceConstants.PUSH_TIMEOUT);
            if (!pushRepoPage.isTimeout()) {
                timeout = Integer.MAX_VALUE;
            }

            String changeset = null;
            if (outgoingPage.getRevisionCheckBox().getSelection()) {
                ChangeSet cs = outgoingPage.getRevision();
                if (cs != null) {
                    changeset = cs.getChangeset();
                }
            }
            String result = Messages.getString("PushRepoWizard.pushOutput.header"); //$NON-NLS-1$
            boolean isForest = false;
            if (pushRepoPage.isShowSvn() && pushRepoPage.getSvnCheckBox().getSelection()) {
                result += HgSvnClient.push(project.getLocation().toFile());
            } else if (pushRepoPage.isShowForest() && pushRepoPage.getForestCheckBox().getSelection()) {
                File forestRoot = MercurialTeamProvider.getHgRoot(
                        project.getLocation().toFile()).getParentFile();

                File snapFile = null;
                String snapFileText = pushRepoPage.getSnapFileCombo().getText();
                if (snapFileText.length() > 0) {
                    snapFile = new File(snapFileText);
                }
                result += HgFpushPullClient.fpush(forestRoot, repo, changeset, timeout, snapFile);
                isForest = true;
            } else {
                result += HgPushPullClient.push(project, repo, pushRepoPage.isForce(), changeset, timeout);
            }

            updateAfterPush(result, project, repo, isForest);

        } catch (URISyntaxException e) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    Messages.getString("PushRepoWizard.malformedUrl"), e.getMessage()); //$NON-NLS-1$
            return false;

        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(), e
                    .getMessage(), e.getMessage());
            return false;
        }
        return true;
    }

    private static void updateAfterPush(String result, IProject project, HgRepositoryLocation repo, boolean isForest) throws HgException {
        if (result.length() != 0) {
            HgClients.getConsole().printMessage(result, null);
        }

        // It appears good. Stash the repo location.
        MercurialEclipsePlugin.getRepoManager().addRepoLocation(project, repo);

        if(isForest){
            IncomingChangesetCache.getInstance().clear(repo);
            OutgoingChangesetCache.getInstance().clear(repo);
        } else {
            IncomingChangesetCache.getInstance().clear(repo, project, true);
            OutgoingChangesetCache.getInstance().clear(repo, project, true);
        }
        MercurialStatusCache.getInstance().refreshStatus(project, null);
    }

    /**
     * Creates a ConfigurationWizardPage.
     */
    protected HgWizardPage createPage(String pageName, String pageTitle,
            String iconPath, String description) {
        page = new ConfigurationWizardMainPage(pageName, pageTitle,
                MercurialEclipsePlugin.getImageDescriptor(iconPath));
        initPage(description, page);
        return page;
    }
}
