/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public final class RefreshWorkspaceStatusJob extends SafeWorkspaceJob {
    private final IProject project;
    private final boolean refreshOnly;

    public RefreshWorkspaceStatusJob(IProject project) {
        this(project, false);
    }

    public RefreshWorkspaceStatusJob(IProject project, boolean refreshOnly) {
        super("Refreshing status for project " + project.getName() + "...");
        this.project = project;
        this.refreshOnly = refreshOnly;
    }

    @Override
    protected IStatus runSafe(IProgressMonitor monitor) {
        try {
            if(!refreshOnly){
                final String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());
                // update branch name
                MercurialTeamProvider.setCurrentBranch(branch, project);

                // reset merge properties
                project.setPersistentProperty(ResourceProperties.MERGING, null);
                project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);
            }

            // refresh resources
            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            return super.runSafe(monitor);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return e.getStatus();
        }
    }
}