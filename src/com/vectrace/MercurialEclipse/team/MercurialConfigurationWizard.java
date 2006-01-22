/**
 * Mercurial create Wizard
 * 
 * This wizard will dig up the project root and then 
 * it lets you modify the directory, when you are done it will 
 * create a mercurial repository
 * 
 * It will follow the dirictory chain to the bottom to se is
 * there is a .hg directory someware, is so it will suggest that you use it
 * instead of creating a new repository.
 * 
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author zingo
 *
 */
public class MercurialConfigurationWizard extends Wizard implements IConfigurationWizard 
{
	public class NewWizardPage extends WizardPage implements SelectionListener 
	{
		Button changeDirButton;
		Button restoreDefaultDirButton;
		Button restoreExistingDirButton;
		boolean newMercurialProject;
		
		NewWizardPage(boolean newMercurialProject) 
		{
		    super( WizardPage.class.getName() );
		    this.newMercurialProject=newMercurialProject;
		    if(newMercurialProject)
		    {
		    	setTitle( "Mercurial Setup wizard" );
		    	setDescription( "Put this project under mercurial version control." );
		    }
		    else
		    {
		    	setTitle( "Mercurial Connect to existing repository" );
		    	setDescription( "Connect to an existing mercurial version control" );
		    }
		    
		    //		    String imgKey = "icons/sample.gif";
//		    setImageDescriptor( new ImageDescriptor( ( imgKey ) );
		    setPageComplete(true); /* Thel that it has the needed info */
		  } 
		  
		  // interface methods of CreateRepositoryPage
		  ////////////////////////////////////////////
		  
		  public void createControl( final Composite parent ) 
		  {
			Composite mainControl;
			Label label;
			
			mainControl = new Composite(parent, SWT.NONE);
		    mainControl.setLayout( new GridLayout(3, false) );

		    label = new Label( mainControl, SWT.CENTER);
		    label.setText("Select Directory");

		    directoryText = new Text(mainControl, SWT.CENTER | SWT.SINGLE);
		    directoryText.setText(hgPath);
		    directoryText.addSelectionListener(this);
		    
		    changeDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
		    changeDirButton.setText("Change Dir");
		    changeDirButton.addSelectionListener(this);

			restoreDefaultDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
			restoreDefaultDirButton.setText("Use Project Root");
			restoreDefaultDirButton.addSelectionListener(this);

		    if(!newMercurialProject)
		    {
		    	restoreExistingDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
		    	restoreExistingDirButton.setText("Use Existing .hg directory");
		    	restoreExistingDirButton.addSelectionListener(this);
		    }
		    
		    setControl(mainControl);
		    setPageComplete(true);
	  
		  }

		public void widgetSelected(SelectionEvent e) 
		{
			if(e.widget==changeDirButton )
			{
				// TODO Auto-generated method stub
				DirectoryDialog directoryDialog;
			    directoryDialog = new DirectoryDialog( new Shell() );
			    directoryDialog.setText("Select mercurial root");
			    directoryDialog.setMessage("Select mercurial root, should be project root.");
			 
			    hgPath=directoryDialog.open();
				directoryText.setText(hgPath);
				//directoryDialog.close();
			}
			else if(e.widget == restoreDefaultDirButton)
			{
				hgPath=hgPathOrginal;
				directoryText.setText(hgPath);
			}
			else if(e.widget == directoryText)
			{
				hgPath=directoryText.getText();
				directoryText.setText(hgPath);
			}
			else if((!newMercurialProject) && (e.widget == restoreExistingDirButton))
			{
				hgPath=foundhgPath;
				directoryText.setText(hgPath);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) 
		{
			//System.out.println("widgetDefaultSelected");
			if(e.widget == directoryText)
			{
				hgPath=directoryText.getText();
				directoryText.setText(hgPath);
			}
//      	This should not happend from a button widget.
		}
	}
	
	private IProject project;
	private String hgPath;
	private String hgPathOrginal;
	private String foundhgPath;
	private String pathProject;
	private Text directoryText;
	
	
	/**
	 * 
	 */
	public MercurialConfigurationWizard() {
		//super();
	    setWindowTitle( "MercurialConfigurationWizard" );
//		System.out.println("MercurialConfigurationWizard.MercurialConfigurationWizard()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
//		System.out.println("MercurialConfigurationWizard.addPages()");

	    IPath projectLocation = project.getLocation();
	    String MercurialRootDir;
	    MercurialRootDir=findMercurialRoot(projectLocation.toFile());
	    pathProject=project.getLocation().toString();
	    if (MercurialRootDir == null)
	    {   
	    	hgPathOrginal=project.getLocation().toString();
	    	hgPath=hgPathOrginal;
			addPage( new NewWizardPage(true) );
	    }
	    else
	    {
	    	foundhgPath=MercurialRootDir;
	    	hgPathOrginal=MercurialRootDir;
	    	hgPath=hgPathOrginal;
			addPage( new NewWizardPage(false) );
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinish() {
		if(directoryText!=null)
		{
			hgPath=directoryText.getText();
		}
		// System.out.println("MercurialConfigurationWizard.preformFinish()");
		System.out.println("execute >cd " + hgPath + ";hg init<");
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IConfigurationWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.core.resources.IProject)
	 */
	public void init(IWorkbench workbench, IProject project) {
		//System.out.println("MercurialConfigurationWizard.init()");
		this.project=project;
	}

    private static String findMercurialRoot( final File file ) {
        String path = null;
        File parent = file;
        File hgFolder = new File( parent, ".hg" );
//        System.out.println("pathcheck:" + parent.toString());
        while ((parent!=null) && !(hgFolder.exists() && hgFolder.isDirectory()) )
        {
        	parent=parent.getParentFile();
        	if(parent!=null)
        	{
//              System.out.println("pathcheck:" + parent.toString());
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
//        System.out.println("pathcheck: >" + path + "<");
        return path;
      }
	

	

}
