/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author zingo
 *
 */
public class MercurialConfigurationWizard extends Wizard implements IConfigurationWizard {

	public class MyWizardPage extends WizardPage {

		MyWizardPage() {
		    super( MyWizardPage.class.getName() );
		    setTitle( "MyWizardPage Title" );
		    setDescription( "Cool here is a description" );
//		    String imgKey = "icons/sample.gif";
//		    setImageDescriptor( new ImageDescriptor( ( imgKey ) );
		  }
		  
		  
		  // interface methods of CreateRepositoryPage
		  ////////////////////////////////////////////
		  
		  public void createControl( final Composite parent ) {
		    Label label = new Label( parent, SWT.NONE );
		    label.setText( "Here is some text...." );
		    setControl( label );
		  }
		}
	

	/**
	 * 
	 */
	public MercurialConfigurationWizard() {
		//super();
		// TODO Auto-generated constructor stub
	    setWindowTitle( "MercurialConfigurationWizard" );
		System.out.println("MercurialConfigurationWizard.MercurialConfigurationWizard()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		// TODO Auto-generated method stub
		System.out.println("MercurialConfigurationWizard.addPages()");
		addPage( new MyWizardPage() );
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		// TODO Auto-generated method stub
		System.out.println("MercurialConfigurationWizard.preformFinish()");
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IConfigurationWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.core.resources.IProject)
	 */
	public void init(IWorkbench workbench, IProject project) {
		System.out.println("MercurialConfigurationWizard.init()");
	}

	

}
