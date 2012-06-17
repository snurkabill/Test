/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Andrei Loskutov 		 - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.model.FileStatus;

public class ChangePathsTableProvider extends TableViewer {

	private static final FileStatus[] EMPTY_CHANGE_PATHS = new FileStatus[0];
	private final ChangedPathsPage page;
	private final ChangePathsTableContentProvider contentProvider;

	public ChangePathsTableProvider(Composite parent, ChangedPathsPage page) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		this.page = page;
		contentProvider = new ChangePathsTableContentProvider(page);
		setContentProvider(contentProvider);

		setLabelProvider(new ChangePathLabelProvider(page, this));

		GridData data = new GridData(GridData.FILL_BOTH);

		final Table table = (Table) getControl();
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		table.setLayoutData(data);
	}

	public int getElementsCount(){
		MercurialRevision revision = page.getCurrentRevision();
		if(revision == null || !revision.isFile()){
			return 0;
		}
		return contentProvider.getElements(revision).length;
	}

	private static class ChangePathLabelProvider extends DecoratingLabelProvider implements
			ITableLabelProvider {

		private final ChangedPathsPage page;
		private final ChangePathsTableProvider tableProvider;

		public ChangePathLabelProvider(ChangedPathsPage page, ChangePathsTableProvider tableProvider) {
			super(new SimpleLabelImageProvider(), PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator());
			this.page = page;
			this.tableProvider = tableProvider;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getText(element);
		}

		@Override
		public Font getFont(Object element) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			MercurialRevision revision = page.getCurrentRevision();
			if(revision == null || !revision.isFile()){
				return null;
			}
			IPath basePath = revision.getIPath();
			IPath currentPath = ((FileStatus) element).getRootRelativePath();
			if(basePath.equals(currentPath) && tableProvider.getElementsCount() > 1) {
				// highlight current file in the changeset, if there are more files
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			}
			return JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
		}

	}

	private static class ChangePathsTableContentProvider implements
			IStructuredContentProvider {

		private final WeakHashMap<MercurialRevision, FileStatus[]> revToFiles;
		private final ChangedPathsPage page;
		private Viewer viewer;
		private boolean disposed;

		public ChangePathsTableContentProvider(ChangedPathsPage page) {
			this.page = page;
			revToFiles = new WeakHashMap<MercurialRevision, FileStatus[]>();
		}

		public Object[] getElements(Object inputElement) {
			if (!this.page.isShowChangePaths()) {
				return EMPTY_CHANGE_PATHS;
			}

			MercurialRevision rev = (MercurialRevision) inputElement;
			FileStatus[] fileStatus;
			synchronized(revToFiles){
				fileStatus = revToFiles.get(rev);
			}
			if(fileStatus != null){
				return fileStatus;
			}
			fetchPaths(rev);
			// but sometimes hg returns a null version map...
			return EMPTY_CHANGE_PATHS;
		}

		private void fetchPaths(final MercurialRevision rev) {
			synchronized(revToFiles){
				if(revToFiles.containsKey(rev)) {
					return;
				}
			}

			List <FileStatus> status = rev.getChangeSet().getChangedFiles();

			synchronized(revToFiles){
				if(!revToFiles.containsKey(rev)) {
					revToFiles.put(rev, status.toArray(new FileStatus[status.size()]));
				}
			}

			if(disposed){
				return;
			}

			Runnable refresh = new Runnable() {
				public void run() {
					if(!disposed && viewer != null) {
						viewer.refresh();
					}
				}
			};
			Display.getDefault().asyncExec(refresh);
		}

		public void dispose() {
			disposed = true;
			synchronized(revToFiles){
				revToFiles.clear();
			}
		}

		public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
			this.viewer = viewer1;
		}
	}
}
