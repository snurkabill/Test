/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.jface.wizard.WizardDialog;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.PullRepoWizard;

public class PullHandler extends RootHandler {

	@Override
	protected void run(final HgRoot hgRoot) {
		PullRepoWizard pullRepoWizard = new PullRepoWizard(hgRoot);
		WizardDialog pullWizardDialog = new WizardDialog(getShell(), pullRepoWizard);
		pullWizardDialog.open();
	}

}
