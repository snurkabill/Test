/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.INavigatable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;

import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;

public class HgCompareEditorInput extends CompareEditorInput {
	private static final HgDifferencer DIFFERENCER = new HgDifferencer();

	private final RevisionNode left;
	private final RevisionNode ancestor;
	private final RevisionNode right;

	private final ISynchronizePageConfiguration syncConfig;

	/**
	 * Does either a 2-way or 3-way compare, depending on if one is an ancestor
	 * of the other. If they are divergent, then it finds the common ancestor
	 * and does 3-way compare.
	 * @param syncConfig
	 */
	public HgCompareEditorInput(CompareConfiguration configuration,
			RevisionNode left, RevisionNode right, RevisionNode ancestor, ISynchronizePageConfiguration syncConfig) {
		this(configuration, left, right, ancestor, true, syncConfig);

		setTitle("Compare " + left.getName());
	}

	private HgCompareEditorInput(CompareConfiguration configuration,
			RevisionNode left, RevisionNode right, RevisionNode ancestor, boolean bLeftEditable,
			ISynchronizePageConfiguration syncConfig) {
		super(configuration);
		this.syncConfig = syncConfig;
		this.left = left;
		this.ancestor = ancestor;
		this.right = right;
		configuration.setLeftLabel(getLabel(left));
		configuration.setLeftEditable(bLeftEditable);
		configuration.setRightLabel(getLabel(right));
		configuration.setRightEditable(false);
		configuration.setAncestorLabel(getLabel(ancestor));
	}

	private static String getLabel(RevisionNode node) {
		if (node != null) {
			return node.getLabel();
		}
		return "";
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		byte[] content = null;
		RevisionNode parent = ancestor;
		if(parent != null){
			content = parent.getContent();
		}
		if(content == null || content.length == 0){
			// See issue 11149: sometimes we fail to determine the ancestor version, but we
			// see it too late as the editor is opened with an "empty" parent node.
			// In such case ENTIRE file is considered as a huge merge conflict.
			// So as quick and dirty workaround we avoid using 3-way merge if parent content is unknown
			parent = null;
		}
		return DIFFERENCER.findDifferences(parent != null, monitor, null, parent, left, right);
	}


	@Override
	public String getOKButtonLabel() {
		if (getCompareConfiguration().isLeftEditable() || getCompareConfiguration().isRightEditable()) {
			return "Save Changes";
		}
		return super.getOKButtonLabel();
	}

	/**
	 *  Overriden to allow navigation through multiple changes in the sync view via shortcuts
	 *  "Ctrl + ." (Navigate->Next) or "Ctrl + ," (Navigate->Previous).
	 *  @see SyncInfoCompareInput
	 */
	@Override
	public synchronized ICompareNavigator getNavigator() {
		CompareNavigator navigator = (CompareNavigator) super.getNavigator();
		if (syncConfig != null) {
			CompareNavigator nav = (CompareNavigator) syncConfig.getProperty(
					SynchronizePageConfiguration.P_NAVIGATOR);

			return new SyncNavigatorWrapper(navigator, nav);
		}
		return navigator;
	}

	private class SyncNavigatorWrapper extends CompareNavigator {

		private final CompareNavigator textDfiffDelegate;
		private final CompareNavigator syncViewDelegate;

		public SyncNavigatorWrapper(CompareNavigator textDfiffDelegate,
				CompareNavigator syncViewDelegate) {
			this.textDfiffDelegate = textDfiffDelegate;
			this.syncViewDelegate = syncViewDelegate;
		}

		@Override
		public boolean selectChange(boolean next) {
			boolean endReached = textDfiffDelegate.selectChange(next);
			if(endReached && syncViewDelegate != null && isSelectedInSynchronizeView()){
				// forward navigation to the sync view
				return syncViewDelegate.selectChange(next);
			}
			return endReached;
		}

		@Override
		// This method won't be used by our implementation
		protected INavigatable[] getNavigatables() {
			return new INavigatable[0];
		}

		@Override
		public boolean hasChange(boolean next) {
			return (textDfiffDelegate.hasChange(next) || syncViewDelegate.hasChange(next));
		}
	}

	private boolean isSelectedInSynchronizeView() {
		if (syncConfig == null) {
			return false;
		}
		ISelection s = syncConfig.getSite().getSelectionProvider().getSelection();
		if (!(s instanceof IStructuredSelection)) {
			return false;
		}
		IStructuredSelection ss = (IStructuredSelection) s;
		Object element = ss.getFirstElement();
		if (element instanceof FileFromChangeSet
				|| element instanceof ChangesetGroup) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ancestor == null) ? 0 : ancestor.hashCode());
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof HgCompareEditorInput)) {
			return false;
		}
		HgCompareEditorInput other = (HgCompareEditorInput) obj;
		if (ancestor == null) {
			if (other.ancestor != null) {
				return false;
			}
		} else if (!ancestor.equals(other.ancestor)) {
			return false;
		}
		if (left == null) {
			if (other.left != null) {
				return false;
			}
		} else if (!left.equals(other.left)) {
			return false;
		}
		if (right == null) {
			if (other.right != null) {
				return false;
			}
		} else if (!right.equals(other.right)) {
			return false;
		}
		return true;
	}
}