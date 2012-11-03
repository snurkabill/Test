/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;


/**
 * @author andrei
 */
public interface IHgResource extends IAdaptable {

	/**
	 * @return the hg root, never null
	 */
	HgRoot getHgRoot();

	/**
	 * The name of the file.
	 *
	 * @return never null
	 */
	String getName();

	/**
	 * The root relative path to the file
	 *
	 * @return relative path to hg root, never null
	 */
	IPath getIPath();

	/**
	 * @return True if this resource does not exist in the working copy
	 */
	boolean isReadOnly();

}