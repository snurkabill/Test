/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.wizards.StripWizard;

public class StripHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        StripWizard stripWizard = new StripWizard(project);
        WizardDialog dialog = new WizardDialog(getShell(), stripWizard);
        dialog.setBlockOnOpen(true);
        dialog.open();
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        LocalChangesetCache.getInstance().clear(resource.getProject());
        LocalChangesetCache.getInstance().refreshAllLocalRevisions(resource.getProject());
    }

}