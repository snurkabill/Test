/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Amenel Voglozin	implementation (2016-08-13)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.lang.reflect.InvocationTargetException;

import com.vectrace.MercurialEclipse.model.GroupedUncommittedChangeSet;
import com.vectrace.MercurialEclipse.synchronize.actions.EditChangesetSynchronizeOperation;

/**
 * @author Amenel Voglozin
 *
 */
public class EditChangesetHandler extends SynchronizeChangesetHandler {

	@Override
	protected void run(GroupedUncommittedChangeSet cs)
			throws InvocationTargetException, InterruptedException {
		EditChangesetSynchronizeOperation operation = new EditChangesetSynchronizeOperation(null,
				null, cs);
		operation.run();
	}
}
