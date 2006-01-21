/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.preferences.PreferenceConstants;


/**
 * @author zingo
 *
 */
public class MercurialConfigurationWizard extends Wizard implements IConfigurationWizard {


	class SummaryPage extends WizardPage {
	  public static final String PAGE_NAME = "Summary";

	  private Label textLabel;

	  public SummaryPage() {
	    super(PAGE_NAME, "Summary Page", null);
	  }

	  public void createControl(Composite parent) {
	    Composite topLevel = new Composite(parent, SWT.NONE);
	    topLevel.setLayout(new FillLayout());

	    textLabel = new Label(topLevel, SWT.CENTER);
	    textLabel.setText("");

	    setControl(topLevel);
	    setPageComplete(true);
	  }

	  public void updateText(String newText) {
	    textLabel.setText(newText);
	  }
	}

	class DirectoryPage extends WizardPage {
	  public static final String PAGE_NAME = "Directory";

	  private Button button;

	  public DirectoryPage() {
	    super(PAGE_NAME, "Directory Page", null);
	  }

	  public void createControl(Composite parent) {
	    Composite topLevel = new Composite(parent, SWT.NONE);
	    topLevel.setLayout(new GridLayout(2, false));

	    Label l = new Label(topLevel, SWT.CENTER);
	    l.setText("Use default directory?");

	    button = new Button(topLevel, SWT.CHECK);

    
	    setControl(topLevel);
	    setPageComplete(true);
	  }

	  public boolean useDefaultDirectory() {
	    return button.getSelection();
	  }
	}

	class ChooseDirectoryPage extends WizardPage {
	  public static final String PAGE_NAME = "Choose Directory";

	  private Text text;

	  public ChooseDirectoryPage() {
	    super(PAGE_NAME, "Choose Directory Page", null);
	  }

	  public void createControl(Composite parent) {
	    Composite topLevel = new Composite(parent, SWT.NONE);
	    topLevel.setLayout(new GridLayout(2, false));

	    Label l = new Label(topLevel, SWT.CENTER);
	    l.setText("Enter the directory to use:");

	    text = new Text(topLevel, SWT.SINGLE);
	    text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	    setControl(topLevel);
	    setPageComplete(true);
	  }

	  public String getDirectory() {
	    return text.getText();
	  }
	}
	
	
	public class NewWizardPage1 extends WizardPage implements SelectionListener 
	{
		String hgPath;
		String hgPathOrginal;
		Text directoryText;
		Button changeDirButton;
		Button restoreDefaultDirButton;
		
		NewWizardPage1(String hgPath) 
		{
		    super( WizardPage.class.getName() );
		    this.hgPath=hgPath;
		    this.hgPathOrginal=hgPath;
		    setTitle( "Mercurial Setup wizard" );
		    setDescription( "Put this project under mercurial version control.\n Press Next to continue" );
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

		    changeDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
		    changeDirButton.setText("Change Dir");
		    changeDirButton.addSelectionListener(this);

			restoreDefaultDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
			restoreDefaultDirButton.setText("Set Default");
			restoreDefaultDirButton.addSelectionListener(this);

		    
		    setControl(mainControl);
		    setPageComplete(true);
/*
		    Label label = new Label( parent, SWT.NONE );
		    label.setText( "Path:" + hgPath );
		    setControl( label );
*/
//		    setControl(new DirectoryFieldEditor("first","&Directory preference:", parent));

		  
		  }

		public void widgetSelected(SelectionEvent e) 
		{
			if(e.widget==changeDirButton )
			{
				// TODO Auto-generated method stub
				DirectoryDialog directoryDialog;
				System.out.println("widgetSelected()");
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
		}

		public void widgetDefaultSelected(SelectionEvent e) 
		{
			// TODO Auto-generated method stub
			System.out.println("widgetDefaultSelected()");
			
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
	    if (MercurialRootDir == null)
	    {    
			addPage( new NewWizardPage1(project.getLocation().toString()) );
	    }
	    else
	    {
			addPage( new ConnectWizardPage(project.getLocation().toString(),MercurialRootDir ) );   	
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
