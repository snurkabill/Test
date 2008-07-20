/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - removeResources()
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgRemoveClient {

    public static void removeResource(IResource resource,
            IProgressMonitor monitor) throws HgException {
        if (monitor != null) {
            monitor.subTask("Removing " + resource.getName()
                    + " from repository");
        }
        HgCommand command = new HgCommand("remove", resource.getProject(), true);
        command.addOptions("--force");
        command.addFiles(resource);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
        command.executeToBytes();
    }

    /**
     * @param filesToRemove
     * @throws HgException 
     */
    public static void removeResources(List<IResource> resources) throws HgException {
        Map<IProject, List<IResource>> resourcesByProject = HgCommand
                .groupByProject(resources);
        for (IProject project : resourcesByProject.keySet()) {            
            // if there are too many resources, do several calls
            int size = resources.size();
            int delta = AbstractShellCommand.MAX_PARAMS - 1;
            for (int i = 0; i < size; i += delta) {
                AbstractShellCommand command = new HgCommand("remove", project,
                        true);
                command
                        .setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
                command.addFiles(resourcesByProject.get(project).subList(i,
                        Math.min(i + delta, size - i)));
                command.executeToBytes();
            }
        }

    }
}