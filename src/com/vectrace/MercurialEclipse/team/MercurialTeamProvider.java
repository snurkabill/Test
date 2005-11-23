/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider {

	/**
	 * 
	 */
	public MercurialTeamProvider() {
		super();
		System.out.println("MercurialTeamProvider.MercurialTeamProvider()");
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	//@Override
	public void configureProject() throws CoreException {
		// TODO Auto-generated method stub
		System.out.println("MercurialTeamProvider.configureProject()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#getID()
	 */
	//@Override
	public String getID() {
		// TODO Auto-generated method stub
		System.out.println("MercurialTeamProvider.getID()");
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub
		System.out.println("MercurialTeamProvider.deconfigure()");
	}

}
