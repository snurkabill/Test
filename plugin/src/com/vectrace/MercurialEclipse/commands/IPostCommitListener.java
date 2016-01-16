/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

/**
 * Interface for listeners to be notified when a resource has been committed.
 *
 * TODO make it so that listeners are only notified for their own resource, not all resources.
 *
 * @author Amenel VOGLOZIN
 *
 */
public interface IPostCommitListener {

	/**
	 * Method to be called when the "post-commit"/"a commit has just finished" notification is sent.
	 */
	public void resourceCommitted();

}
