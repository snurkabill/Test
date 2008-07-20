/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 * 
 */
public class TransplantPage extends ConfigurationWizardMainPage {

    private IProject project;
    private List<String> nodeIds = new ArrayList<String>();
    private boolean branch;
    private String branchName;
    private boolean all;
    private ChangesetTable changesetTable;
    private Button branchCheckBox;
    private Combo branchNameCombo;
    private Button allCheckBox;
    private SortedSet<ChangeSet> changesets = new TreeSet<ChangeSet>(
            Collections.reverseOrder());

    public TransplantPage(String pageName, String title,
            ImageDescriptor titleImage, IProject project) {
        super(pageName, title, titleImage);
        this.project = project;
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        ModifyListener urlModifyListener = new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                try {
                    SortedSet<ChangeSet> changes = IncomingChangesetCache
                            .getInstance().getIncomingChangeSets(project,
                                    new HgRepositoryLocation(getUrlCombo()
                                            .getText()));
                    if (changes != null) {
                        changesets.clear();
                        changesets.addAll(changes);
                        populateChangesetTable();
                    }
                } catch (HgException e1) {
                    setErrorMessage(Messages
                            .getString("TransplantPage.errorLoadChangesets")); //$NON-NLS-1$)
                    MercurialEclipsePlugin.logError(e1);
                } catch (URISyntaxException e1) {
                    MercurialEclipsePlugin.logError(e1);
                    setErrorMessage(e1.getLocalizedMessage());
                }

            }

        };
        getUrlCombo().addModifyListener(urlModifyListener);

        addBranchGroup(composite);
        addChangesetGroup(composite);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vectrace.MercurialEclipse.wizards.ConfigurationWizardMainPage#canFlipToNextPage()
     */
    @Override
    public boolean canFlipToNextPage() {
        return super.canFlipToNextPage();
    }

    private void validatePage() {
        boolean valid = true;
        if (branch) {
            valid &= branchName != null;
            if (!all) {
                valid &= nodeIds.size() > 0;
            }
        } else {
            valid &= nodeIds.size() > 0;
        }
        setPageComplete(valid);
    }

    /**
     * @param composite
     */
    private void addBranchGroup(Composite composite) {
        // now the branch group
        Group branchGroup = SWTWidgetHelper.createGroup(composite, Messages
                .getString("TransplantPage.branchGroup.title")); //$NON-NLS-1$
        createBranchCheckBox(branchGroup);
        createAllCheckBox(branchGroup);
        createBranchNameCombo(branchGroup);
    }

    /**
     * @param branchGroup
     */
    private void createBranchNameCombo(Group branchGroup) {
        SWTWidgetHelper.createLabel(branchGroup, Messages
                .getString("TransplantPage.branchLabel.title")); //$NON-NLS-1$
        this.branchNameCombo = SWTWidgetHelper.createCombo(branchGroup);
        this.branchNameCombo.setEnabled(false);
        populateBranchNameCombo();

        SelectionListener branchNameComboListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                // TODO filter changeset table
                branchName = branchNameCombo.getText();
                if (branchName.equals("default")) { //$NON-NLS-1$
                    branchName = ""; //$NON-NLS-1$
                }

                try {
                    changesets.clear();
                    changesets.addAll(LocalChangesetCache.getInstance()
                            .getLocalChangeSetsByBranch(project, branchName));
                    populateChangesetTable();
                } catch (HgException e1) {
                    setErrorMessage(Messages
                            .getString("TransplantPage.errorLoadChangesets")); //$NON-NLS-1$
                    MercurialEclipsePlugin.logError(e1);
                }

                validatePage();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.branchNameCombo.addSelectionListener(branchNameComboListener);
    }

    /**
     * @param branchGroup
     */
    private void createAllCheckBox(Group branchGroup) {
        this.allCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
                .getString("TransplantPage.allCheckBox.title.1") //$NON-NLS-1$
                + Messages.getString("TransplantPage.allCheckBox.title.2")); //$NON-NLS-1$
        this.allCheckBox.setEnabled(false);

        SelectionListener allCheckBoxListener = new SelectionListener() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                all = allCheckBox.getSelection();
                validatePage();
            }
        };

        this.allCheckBox.addSelectionListener(allCheckBoxListener);
    }

    /**
     * @param branchGroup
     */
    private void createBranchCheckBox(Group branchGroup) {
        this.branchCheckBox = SWTWidgetHelper.createCheckBox(branchGroup, Messages
                .getString("TransplantPage.branchCheckBox.title")); //$NON-NLS-1$

        SelectionListener branchCheckBoxListener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                TransplantPage.this.getUrlCombo().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.getUserCombo().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.getPasswordText().setEnabled(
                        !branchCheckBox.getSelection());
                TransplantPage.this.allCheckBox.setEnabled(branchCheckBox
                        .getSelection());
                TransplantPage.this.branchNameCombo.setEnabled(branchCheckBox
                        .getSelection());
                branch = branchCheckBox.getSelection();
                if (branch) {
                    changesets.clear();
                    branchNameCombo.deselectAll();
                } else {
                    changesets.clear();
                    getUrlCombo().deselectAll();
                }
                validatePage();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.branchCheckBox.addSelectionListener(branchCheckBoxListener);
    }

    /**
     * @param composite
     */
    private void addChangesetGroup(Composite composite) {
        // table of changesets
        Group changeSetGroup = SWTWidgetHelper.createGroup(
                composite,
                Messages.getString("TransplantPage.changesetGroup.title"), GridData.FILL_BOTH); //$NON-NLS-1$

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 200;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(changeSetGroup, SWT.MULTI
                | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(true);

        SelectionListener changeSetTableListener = new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                ChangeSet[] changeSets = changesetTable.getSelections();
                ChangeSet last = changeSets[0];
                setErrorMessage(null);
                nodeIds.clear();
                for (ChangeSet changeSet : changeSets) {
                    if (Math.abs(changeSet.getChangesetIndex()
                            - last.getChangesetIndex()) > 1) {
                        setErrorMessage(Messages
                                .getString("TransplantPage.errorNotSequential")); //$NON-NLS-1$
                        setPageComplete(false);
                        break;
                    }
                    nodeIds.add(changeSet.getChangeset());
                    last = changeSet;
                }

                validatePage();
            }
        };

        changesetTable.addSelectionListener(changeSetTableListener);
        populateChangesetTable();
    }

    /**
     * 
     */
    private void populateBranchNameCombo() {
        try {
            Branch[] branches = HgBranchClient.getBranches(project);
            for (Branch myBranch : branches) {
                this.branchNameCombo.add(myBranch.getName());
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.showError(e);
            MercurialEclipsePlugin.logError(e);
        }
    }

    private void populateChangesetTable() {
        changesetTable.setChangesets(changesets
                .toArray(new ChangeSet[changesets.size()]));
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    /**
     * @return the project
     */
    public IProject getProject() {
        return project;
    }

    /**
     * @param project
     *            the project to set
     */
    public void setProject(IProject project) {
        this.project = project;
    }

    /**
     * @return
     */
    public boolean isBranch() {
        return this.branch;
    }

    /**
     * @return the nodeIds
     */
    public List<String> getNodeIds() {
        return nodeIds;
    }

    /**
     * @return
     */
    public String getBranchName() {
        return this.branchName;
    }

    /**
     * @return
     */
    public boolean isAll() {
        return this.all;
    }

    /**
     * @return the changesets
     */
    public SortedSet<ChangeSet> getChangesets() {
        return changesets;
    }

}