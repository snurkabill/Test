/**
 * com.vectrace.MercurialEclipse (c) Vectrace Feb 3, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.RepositoryProvider;

/**
 * @author zingo
 *
 */
public class MercurialUtilities 
{

	/**
	 * This class is full of utilities metods, useful allover the place
	 */
	public MercurialUtilities() 
	{
	}
    
    static String search4MercurialRoot( final IProject project ) 
    {
        return MercurialUtilities.search4MercurialRoot(project.getLocation().toFile());    
    }

	
    static String search4MercurialRoot( final File file ) 
    {
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


	static IProject getProject(IStructuredSelection selection) 
	{
	 	Object obj;
	    obj = selection.getFirstElement();
	    if((obj!=null) && (obj instanceof IResource))
	    {
	    	return ((IResource) obj).getProject();
	    }
		return null;
	}
	static String getRepositoryPath(IProject proj)
	{
		//	 Get Repository path
		RepositoryProvider provider = RepositoryProvider.getProvider( proj );
		if (provider instanceof MercurialTeamProvider)
		{
			return(((MercurialTeamProvider) provider).getRepositoryPath());
		}
		else
		{
			return null;
		}
	}

	/* TODO 
	 * IProcess, ILaunch? Is this what should be used insted of java.io stuff ??? 	
	 */

	
	static void ExecuteCommand(String cmd[])
	{
	//Setup and run command
	//   System.out.println("hg --cwd " + Repository + " status");
	//	String launchCmd[] = { "hg","--cwd", Repository ,"status" };
	    System.out.println("ExecuteCommand:" + cmd.toString());
		try 
		{
		    int c;
			Process process = Runtime.getRuntime().exec(cmd); 
			InputStream in = process.getInputStream();
			System.out.println("Output:");
			while ((c = in.read()) != -1) 
			{
				System.out.print((char)c);
			}
			in.close();
			System.out.println("Error:");	        
			InputStream err = process.getErrorStream();
			while ((c = err.read()) != -1) 
			{
				System.out.print((char)c);
			}
			err.close();	        
			process.waitFor();
			//TODO put output in a window or something
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}		
	}
    
}
