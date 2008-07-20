/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.part.WorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.actions.RemoveRootAction;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;
import com.vectrace.MercurialEclipse.repository.model.RemoteContentProvider;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.NewLocationWizard;

/**
 * RepositoriesView is a view on a set of known Hg repositories
 */
public class RepositoriesView extends ViewPart implements ISelectionListener {
    public static final String VIEW_ID = "com.vectrace.MercurialEclipse.repository.RepositoriesView"; //$NON-NLS-1$

    // The root
    private AllRootsElement root;

    // Actions
    private Action newAction;
    private RemoveRootAction removeRootAction;

    // The tree viewer
    protected TreeViewer treeViewer;

    // Drill down adapter
    private DrillDownAdapter drillPart; // Home, back, and "drill into"

    private Action refreshAction;
    private Action refreshPopupAction;
    private Action collapseAllAction;
    private Action propertiesAction;

    private RemoteContentProvider contentProvider;
    // this listener is used when a repository is added, removed or changed
    private IRepositoryListener repositoryListener = new IRepositoryListener() {
        public void repositoryAdded(final HgRepositoryLocation loc) {
            getViewer().getControl().getDisplay().syncExec(new Runnable() {
                public void run() {
                    refreshViewer(null, false);
                    getViewer().setSelection(new StructuredSelection(loc));
                }
            });
        }

        public void repositoryRemoved(HgRepositoryLocation loc) {
            refresh(null, false);
        }

        public void repositoriesChanged(HgRepositoryLocation[] roots) {
            refresh(null, false);
        }

        private void refresh(Object object, boolean refreshRepositoriesFolders) {
            final Object finalObject = object;
            final boolean finalRefreshReposFolders = refreshRepositoriesFolders;
            Display display = getViewer().getControl().getDisplay();
            display.syncExec(new Runnable() {
                public void run() {
                    RepositoriesView.this.refreshViewer(finalObject,
                            finalRefreshReposFolders);
                }
            });
        }

        public void repositoryModified(HgRepositoryLocation loc) {
            refresh(null, false);
        }
    };

    /**
     * Constructor for RepositoriesView.
     * 
     * @param partName
     */
    public RepositoriesView() {
        // super(VIEW_ID);
    }

    /**
     * Contribute actions to the view
     */
    protected void contributeActions() {

        final Shell shell = getShell();

        // Create actions

        // New Repository (popup)
        newAction = new Action("Create Repository", MercurialEclipsePlugin
                .getImageDescriptor("wizards/newlocation_wiz.gif")) {
            @Override
            public void run() {
                NewLocationWizard wizard = new NewLocationWizard();
                WizardDialog dialog = new WizardDialog(shell, wizard);
                dialog.open();
            }
        };

        // Properties
        propertiesAction = new PropertyDialogAction(
                new SameShellProvider(shell), getViewer());
        getViewSite().getActionBars().setGlobalActionHandler(
                ActionFactory.PROPERTIES.getId(), propertiesAction);
        IStructuredSelection selection = (IStructuredSelection) getViewer()
                .getSelection();
        if (selection.size() == 1
                && selection.getFirstElement() instanceof HgRepositoryLocation) {
            propertiesAction.setEnabled(true);
        } else {
            propertiesAction.setEnabled(false);
        }
        getViewer().addSelectionChangedListener(
                new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        IStructuredSelection ss = (IStructuredSelection) event
                                .getSelection();
                        boolean enabled = ss.size() == 1
                                && ss.getFirstElement() instanceof HgRepositoryLocation;
                        propertiesAction.setEnabled(enabled);
                    }
                });

        // Remove Root
        removeRootAction = new RemoveRootAction(treeViewer.getControl()
                .getShell());
        removeRootAction.selectionChanged((IStructuredSelection) null);

        IActionBars bars = getViewSite().getActionBars();
        bars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
                removeRootAction);

        // Refresh action (toolbar)
        refreshAction = new Action("Refresh repositories",
                MercurialEclipsePlugin.getImageDescriptor("elcl16/refresh.gif")) {
            @Override
            public void run() {
                refreshViewer(null, true);
            }
        };
        refreshAction.setToolTipText("Refresh"); 
        refreshAction.setDisabledImageDescriptor(MercurialEclipsePlugin
                .getImageDescriptor("dlcl16/refresh.gif"));
        refreshAction.setHoverImageDescriptor(MercurialEclipsePlugin
                .getImageDescriptor("clcl16/refresh.gif"));
        getViewSite().getActionBars().setGlobalActionHandler(
                ActionFactory.REFRESH.getId(), refreshAction);

        refreshPopupAction = new Action("Refresh", MercurialEclipsePlugin
                .getImageDescriptor("clcl16/refresh.gif")) {
            @Override
            public void run() {
                refreshViewerNode();
            }
        };

        // Collapse action
        collapseAllAction = new Action("RepositoriesView.collapseAll",
                MercurialEclipsePlugin
                        .getImageDescriptor("elcl16/collapseall.gif")) {
            @Override
            public void run() {
                collapseAll();
            }
        };
        collapseAllAction.setToolTipText("Collapse all");
        collapseAllAction.setHoverImageDescriptor(MercurialEclipsePlugin
                .getImageDescriptor("clcl16/collapseall.gif"));

        // Create the popup menu
        MenuManager menuMgr = new MenuManager();
        Tree tree = treeViewer.getTree();
        Menu menu = menuMgr.createContextMenu(tree);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                addWorkbenchActions(manager);
            }

        });
        menuMgr.setRemoveAllWhenShown(true);
        tree.setMenu(menu);
        getSite().registerContextMenu(menuMgr, treeViewer);

        // Create the local tool bar
        IToolBarManager tbm = bars.getToolBarManager();
        drillPart.addNavigationActions(tbm);
        tbm.add(refreshAction);
        tbm.add(new Separator());
        tbm.add(collapseAllAction);
        tbm.update(false);

        bars.updateActionBars();
    } // contributeActions

    /**
     * @see org.tigris.subversion.subclipse.ui.repo.RemoteViewPart#addWorkbenchActions(org.eclipse.jface.action.IMenuManager)
     */
    protected void addWorkbenchActions(IMenuManager manager) {
        // New actions go next

        MenuManager sub = new MenuManager("New",
                IWorkbenchActionConstants.GROUP_ADD);
        sub.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(sub);

        // File actions go first (view file)
        manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
        // Misc additions
        manager.add(new Separator("historyGroup"));
        manager.add(new Separator("checkoutGroup"));
        manager.add(new Separator("exportImportGroup"));
        manager.add(new Separator("miscGroup"));

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.add(refreshPopupAction);

        IStructuredSelection selection = (IStructuredSelection) getViewer()
                .getSelection();
        removeRootAction.selectionChanged(selection);
        if (removeRootAction.isEnabled()) {
            manager.add(removeRootAction);
        }

        if (selection.size() == 1
                && selection.getFirstElement() instanceof HgRepositoryLocation) {
            manager.add(new Separator());
            manager.add(propertiesAction);
        }
        sub.add(newAction);
    }

    /*
     * @see WorkbenchPart#createPartControl
     */
    @Override
    public void createPartControl(Composite parent) {
        treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        contentProvider = new RemoteContentProvider();
        treeViewer.setContentProvider(contentProvider);
        treeViewer.setLabelProvider(new WorkbenchLabelProvider());
        getSite().setSelectionProvider(treeViewer);
        root = new AllRootsElement();
        treeViewer.setInput(root);
        treeViewer.setSorter(new RepositorySorter());
        drillPart = new DrillDownAdapter(treeViewer);

        contributeActions();

        initializeListeners();
        MercurialEclipsePlugin.getRepoManager().addRepositoryListener(
                repositoryListener);
    }

    /**
     * initialize the listeners
     */
    protected void initializeListeners() {
        getSite().getWorkbenchWindow().getSelectionService()
                .addPostSelectionListener(this);
        treeViewer.addSelectionChangedListener(removeRootAction);

        // when F5 is pressed, refresh this view
        treeViewer.getControl().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.keyCode == SWT.F5) {
                    /*
                     * IStructuredSelection selection =
                     * (IStructuredSelection)getViewer().getSelection(); if
                     * (selection.size() == 1) {
                     * getViewer().refresh(selection.getFirstElement()); }
                     */
                    refreshAction.run();
                }
            }
        });

        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                handleDoubleClick(e);
            }
        });

    }

    /**
     * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        String msg = getStatusLineMessage(selection);
        getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
    }

    /**
     * When selection is changed we update the status line
     */
    private String getStatusLineMessage(ISelection selection) {
        if (selection == null || selection.isEmpty())
            return "";
        if (!(selection instanceof IStructuredSelection))
            return "";
        IStructuredSelection s = (IStructuredSelection) selection;

        if (s.size() > 1) {
            return String.valueOf(s.size()) + " selected.";
        }
        return "1 selected.";
    }

    /**
     * @see WorkbenchPart#setFocus
     */
    @Override
    public void setFocus() {
        treeViewer.getControl().setFocus();
    }

    /**
     * Method getShell.
     * 
     * @return Shell
     */
    protected Shell getShell() {
        return treeViewer.getTree().getShell();
    }

    /**
     * Returns the viewer.
     * 
     * @return TreeViewer
     */
    protected TreeViewer getViewer() {
        return treeViewer;
    }

    /**
     * this is called whenever a new repository location is added for example or
     * when user wants to refresh
     */
    protected void refreshViewer(Object object,
            boolean refreshRepositoriesFolders) {
        if (treeViewer == null)
            return;
        if (refreshRepositoriesFolders) {
            try {
                MercurialEclipsePlugin.getRepoManager().refreshRepositories(
                        null);
            } catch (HgException e) {
                MercurialEclipsePlugin.logError(e);
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
        if (object == null)
            treeViewer.refresh();
        else
            treeViewer.refresh(object);
    }

    @SuppressWarnings("unchecked")
    protected void refreshViewerNode() {
        IStructuredSelection selection = (IStructuredSelection) treeViewer
                .getSelection();
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
            Object object = iter.next();
            if (object instanceof HgRepositoryLocation) {
                refreshAction.run();
                break;
            }
            treeViewer.refresh(object);
        }
    }

    public void collapseAll() {
        if (treeViewer == null)
            return;
        treeViewer.getControl().setRedraw(false);
        treeViewer.collapseToLevel(treeViewer.getInput(),
                AbstractTreeViewer.ALL_LEVELS);
        treeViewer.getControl().setRedraw(true);
    }

    /**
     * The mouse has been double-clicked in the tree, perform appropriate
     * behaviour.
     */
    private void handleDoubleClick(DoubleClickEvent e) {
        // Only act on single selection
        ISelection selection = e.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            if (structured.size() == 1) {
                Object first = structured.getFirstElement();
                // Try to expand/contract
                treeViewer.setExpandedState(first, !treeViewer
                        .getExpandedState(first));
            }
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        MercurialEclipsePlugin.getRepoManager().removeRepositoryListener(
                repositoryListener);
        super.dispose();
        treeViewer = null;
    }

}