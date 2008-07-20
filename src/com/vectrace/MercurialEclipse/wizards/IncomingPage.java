package com.vectrace.MercurialEclipse.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.ui.ChangeSetLabelProvider;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

public class IncomingPage extends HgWizardPage {

    private TableViewer changeSetViewer;
    private TableViewer fileStatusViewer;
    private IProject project;
    private HgRepositoryLocation location;
    private Button revisionCheckBox;
    private ChangeSet revision;
    private SortedSet<ChangeSet> incoming;


    private class GetIncomingOperation extends HgOperation {

        
        
        /**
         * @param context
         */
        public GetIncomingOperation(IRunnableContext context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription
         * ()
         */
        @Override
        protected String getActionDescription() {
            return "Getting incoming changesets...";
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse
         * .core.runtime.IProgressMonitor)
         */
        @Override
        public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Getting incoming changesets...", 1);
            monitor.subTask("Calling Mercurial...");
            incoming = getIncomingInternal();
            monitor.worked(1);
            monitor.done();
        }

        private SortedSet<ChangeSet> getIncomingInternal() {
            try {
                HgRepositoryLocation remote = location;
                incoming = IncomingChangesetCache.getInstance()
                        .getIncomingChangeSets(project, remote);
                return incoming;
            } catch (HgException e) {
                MercurialEclipsePlugin.showError(e);
            }
            return new TreeSet<ChangeSet>();
        }

    }
    
    protected IncomingPage(String pageName) {
        super(pageName);
        this.setTitle(Messages.getString("IncomingPage.title")); //$NON-NLS-1$
        this.setDescription(Messages.getString("IncomingPage.description")); //$NON-NLS-1$
    }
    
    

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            try {
                getContainer().run(true, false,
                        new GetIncomingOperation(getContainer()));
                changeSetViewer.setInput(incoming);
            } catch (InvocationTargetException e) {
                MercurialEclipsePlugin.logError(e);
                setErrorMessage(e.getLocalizedMessage());
            } catch (InterruptedException e) {
                MercurialEclipsePlugin.logError(e);
                setErrorMessage(e.getLocalizedMessage());
            }
        }
    }    

    public void createControl(Composite parent) {

        Composite container = SWTWidgetHelper.createComposite(parent, 1);
        setControl(container);

        changeSetViewer = new TableViewer(container, SWT.SINGLE | SWT.BORDER
                | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        changeSetViewer.setContentProvider(new ArrayContentProvider());
        changeSetViewer.setLabelProvider(new ChangeSetLabelProvider());
        Table table = changeSetViewer.getTable();
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 150;
        gridData.minimumHeight = 50;
        table.setLayoutData(gridData);

        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        String[] titles = {
                Messages.getString("IncomingPage.columnHeader.revision"),
                Messages.getString("IncomingPage.columnHeader.global"),
                Messages.getString("IncomingPage.columnHeader.date"),
                Messages.getString("IncomingPage.columnHeader.author"),
                Messages.getString("IncomingPage.columnHeader.branch"), Messages.getString("IncomingPage.columnHeader.summary") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        int[] widths = { 42, 100, 122, 80, 80, 150 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }

        fileStatusViewer = new TableViewer(container, SWT.SINGLE | SWT.BORDER
                | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        fileStatusViewer.setContentProvider(new ArrayContentProvider());
        fileStatusViewer.setLabelProvider(new FileStatusLabelProvider());

        table = fileStatusViewer.getTable();
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 150;
        gridData.minimumHeight = 50;
        table.setLayoutData(gridData);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        titles = new String[] {
                Messages
                        .getString("IncomingPage.fileStatusTable.columnTitle.status"), Messages.getString("IncomingPage.fileStatusTable.columnTitle.path") }; //$NON-NLS-1$ //$NON-NLS-2$
        widths = new int[] { 80, 400 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }

        Group group = SWTWidgetHelper.createGroup(container, Messages
                .getString("IncomingPage.group.title")); //$NON-NLS-1$
        this.revisionCheckBox = SWTWidgetHelper.createCheckBox(group, Messages
                .getString("IncomingPage.revisionCheckBox.title")); //$NON-NLS-1$
        makeActions();
    }

    ChangeSet getSelectedChangeSet() {
        IStructuredSelection sel = (IStructuredSelection) changeSetViewer
                .getSelection();
        Object firstElement = sel.getFirstElement();
        if (firstElement instanceof ChangeSet) {
            return (ChangeSet) firstElement;
        }
        return null;
    }

    private void makeActions() {
        changeSetViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent event) {
                        ChangeSet change = getSelectedChangeSet();
                        revision = change;
                        if (change != null) {
                            fileStatusViewer.setInput(change.getChangedFiles());
                        } else {
                            fileStatusViewer.setInput(new Object[0]);
                        }
                    }
                });

        fileStatusViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ChangeSet cs = getSelectedChangeSet();
                IStructuredSelection sel = (IStructuredSelection) event
                        .getSelection();
                FileStatus clickedFileStatus = (FileStatus) sel
                        .getFirstElement();
                if (cs != null && clickedFileStatus != null) {
                    IPath hgRoot;
                    try {
                        hgRoot = new Path(cs.getHgRoot().getCanonicalPath());
                        IPath fileRelPath = new Path(clickedFileStatus
                                .getPath());
                        IPath fileAbsPath = hgRoot.append(fileRelPath);
                        IResource file = project.getWorkspace().getRoot()
                                .getFileForLocation(fileAbsPath);
                        CompareUtils.openEditor(file, cs, true, true);                        
                    } catch (IOException e) {
                        setErrorMessage(e.getLocalizedMessage());
                        MercurialEclipsePlugin.logError(e);
                    }
                }
            }
        });
    }

    private static class FileStatusLabelProvider extends LabelProvider
            implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof FileStatus)) {
                return Messages.getString("IncomingPage.unknownElement") + element; //$NON-NLS-1$
            }
            FileStatus status = (FileStatus) element;
            switch (columnIndex) {
            case 0:
                return status.getAction().name();
            case 1:
                return status.getPath();
            }
            return Messages.getString("IncomingPage.notApplicable"); //$NON-NLS-1$
        }
    }

    /**
     * @param project
     */
    public void setProject(IProject project) {
        this.project = project;
    }

    /**
     * @param repo
     */
    public void setLocation(HgRepositoryLocation repo) {
        this.location = repo;
    }

    /**
     * @return the revisionCheckBox
     */
    public Button getRevisionCheckBox() {
        return revisionCheckBox;
    }

    /**
     * @return the revision
     */
    public ChangeSet getRevision() {
        return revision;
    }

    public SortedSet<ChangeSet> getIncoming() {
        return incoming;
    }
}