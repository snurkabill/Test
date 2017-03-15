/*******************************************************************************
 * Copyright (c) 2014 MercurialEclipse project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * nadirj	implementation
 *******************************************************************************/
package com.nexj.oss.MercurialEclipseMaven;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.spi.ScmHandler;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCloneClient;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * @author nadirj
 *
 */
@SuppressWarnings("restriction")
public class MercurialHandler extends ScmHandler{

	public static final String SCM_HG_PREFIX = "scm:hg:";
	
	public static final String ARTIFACT_ID="${project.artifactId}"; 
	
	public static final String HEAD="HEAD";

	/**
	 * @see org.eclipse.m2e.scm.spi.ScmHandler#checkoutProject(org.eclipse.m2e.scm.MavenProjectScmInfo, java.io.File, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void checkoutProject(MavenProjectScmInfo info, File dest, IProgressMonitor arg2)
			throws CoreException, InterruptedException {
		HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();

		String repositoryUrl = info.getRepositoryUrl();
		String hgURL = repositoryUrl.substring(SCM_HG_PREFIX.length());
		String resolvedHgURL;
		
		if (hgURL.contains(ARTIFACT_ID)){
			String artifactId= info.getModel().getArtifactId();
		    resolvedHgURL = hgURL.replace(ARTIFACT_ID,artifactId);
		} else {
			resolvedHgURL=hgURL;
		}
		
		
		IHgRepositoryLocation location = repoManager.getRepoLocation(resolvedHgURL);
		
		String revision = info.getRevision();
		
		if (revision.equals(HEAD)){
			revision=null;
		}
		
		HgCloneClient.clone(dest.getParentFile(), location, false, false, false,
				false, revision, dest.getName());
	}

}
