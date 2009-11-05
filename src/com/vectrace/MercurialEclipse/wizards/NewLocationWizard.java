/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Properties;

import org.eclipse.jface.wizard.IWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Wizard to add a new location. Uses ConfigurationWizardMainPage for entering
 * informations about SVN repository location
 */
public class NewLocationWizard extends HgWizard {

    public NewLocationWizard() {
        super(Messages.getString("NewLocationWizard.name")); //$NON-NLS-1$
    }

    public NewLocationWizard(Properties initialProperties) {
        this();
        this.properties = initialProperties;
    }

    @Override
    public void addPages() {
        page = createPage(Messages.getString("NewLocationWizard.repoCreationPage.name"),
                Messages.getString("NewLocationWizard.repoCreationPage.title"), //$NON-NLS-1$
                Messages.getString("NewLocationWizard.repoCreationPage.image"), //$NON-NLS-1$
                Messages.getString("NewLocationWizard.repoCreationPage.description")); //$NON-NLS-1$
        addPage(page);
    }

    /**
     * @see IWizard#performFinish
     */
    @Override
    public boolean performFinish() {
        super.performFinish();
        // TODO: Temporarily commented. A project selector must added to
        // this wizard
        /*
        Properties props = page.getProperties();
        final HgRepositoryLocation[] root = new HgRepositoryLocation[1];
        HgRepositoryLocationManager provider = MercurialEclipsePlugin
                .getRepoManager();
        try {
            root[0] = provider.createRepository(props);
            return true;
        } catch (TeamException e) {
            MercurialEclipsePlugin.logError(e);
        }
        */
        return false;
    }

    /**
     * Creates a ConfigurationWizardPage.
     */
    protected HgWizardPage createPage(String pageName, String pageTitle,
            String iconPath, String description) {
        ConfigurationWizardMainPage mainPage = new ConfigurationWizardMainPage(pageName, pageTitle,
                MercurialEclipsePlugin.getImageDescriptor(iconPath));

        mainPage.setShowBundleButton(false);
        page = mainPage;
        initPage(description, page);
        return page;
    }
}
