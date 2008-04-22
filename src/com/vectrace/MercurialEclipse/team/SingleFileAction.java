package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class SingleFileAction implements IActionDelegate {

	private IFile selection;

	public SingleFileAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			//the xml enables this action only for a selection of a single file
			this.selection = (IFile)((IStructuredSelection) selection).getFirstElement();
		}
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected IFile getSelectedFile() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedFile());
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
            MessageDialog.openError(getShell(), "Hg says...", e.getMessage()+"\nSee Error Log for more details.");
		}
	}
	
	protected abstract void run(IFile file) throws Exception ;
}