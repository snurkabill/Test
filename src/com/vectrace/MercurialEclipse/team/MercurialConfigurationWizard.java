/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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

	public class NewWizardPage extends WizardPage {
		String hgPath;
		
		NewWizardPage(String hgPath) {
		    super( WizardPage.class.getName() );
		    this.hgPath=hgPath;
		    setTitle( "Mercurial Setup wizard" );
		    setDescription( "Put this project under mercurial version control" );
//		    String imgKey = "icons/sample.gif";
//		    setImageDescriptor( new ImageDescriptor( ( imgKey ) );
		  }
		  
		  
		  // interface methods of CreateRepositoryPage
		  ////////////////////////////////////////////
		  
		  public void createControl( final Composite parent ) {
		    Label label = new Label( parent, SWT.NONE );
		    label.setText( "Path:" + hgPath );
		    setControl( label );
		  }
		}

	public class ConnectWizardPage extends WizardPage {

		String projPath;
		String hgPath;
		ConnectWizardPage(String projPath, String hgPath) {
		    super( WizardPage.class.getName() );
		    this.projPath=projPath;
		    this.hgPath=hgPath;
		    setTitle( "Mercurial Connect to existing repository" );
		    setDescription( "Connect to an existing mercurial version control" );
//		    String imgKey = "icons/sample.gif";
//		    setImageDescriptor( new ImageDescriptor( ( imgKey ) );
		  }
		  
		  
		  // interface methods of CreateRepositoryPage
		  ////////////////////////////////////////////
		  
		  public void createControl( final Composite parent ) {
		    Label label = new Label( parent, SWT.NONE );
		    label.setText( "projPath=" + projPath + "hgPath="+hgPath );
		    setControl( label );
		  }
		}

	
	  private IProject project;
	
	
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

	    IPath projectLocation = project.getLocation();
	    String MercurialRootDir;
	    MercurialRootDir=findMercurialRoot(projectLocation.toFile());
	    if (MercurialRootDir != null)
	    {    
			addPage( new ConnectWizardPage(project.getLocation().toString(),MercurialRootDir ) );	
	    }
	    else
	    {
	    	
			addPage( new NewWizardPage(project.getLocation().toString()) );		    	
	    }
	    

    
	}

    private static String findMercurialRoot( final File file ) {
        String path = null;
        File parent = file;
        File hgFolder = new File( parent, ".hg" );
        System.out.println("pathcheck:" + parent.toString());
        while ((parent!=null) && !(hgFolder.exists() && hgFolder.isDirectory()) )
        {
        	parent=parent.getParentFile();
        	if(parent!=null)
        	{
              System.out.println("pathcheck:" + parent.toString());
              hgFolder = new File( parent, ".hg" );
        	}
        }
        if(parent!=null)
        {
          path = hgFolder.getParentFile().toString();
        }
        else
        {
          path = null;
        }
        System.out.println("pathcheck: >" + path + "<");
        return path;
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
		this.project=project;
	}

	

}
