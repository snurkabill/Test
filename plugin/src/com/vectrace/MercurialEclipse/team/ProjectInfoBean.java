/*******************************************************************************
 * Copyright (c) 2005-2017 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

/**
 * Bean used to collect information about projects so that the project label can be computed by the
 * resource decorator according to a user-defined syntax.
 *
 * @author Amenel
 *
 */
public class ProjectInfoBean {
	public boolean isNew = false;
	public String branch = "";
	public String author = "";
	public String heads = "";
	public String hex = "";
	public String index = "";
	public String node = "";
	public String outgoing = "";

	/**
	 * The logical name of a repo is always enclosed (when present) by MercurialEclipse between
	 * square brackets. If the user has specified "REPONAME", it will be reformatted as
	 * "[REPONAME]".
	 */
	public String repoLogicalName = "";

	public String tags = "";
	public String mergeMsg = "";
	public String rebaseMsg = "";
	public String bisectMsg = "";
}
