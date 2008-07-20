/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgRenameClient {

    public static void renameResource(IResource source, IResource dest,
            IProgressMonitor monitor) throws HgException {
        // FIXME what are we supposed to do if both resources are in a different
        // project?
        if (monitor != null) {
            monitor.subTask("Moving " + source.getName() + " to "
                    + dest.getName());
        }
        HgCommand command = new HgCommand("rename", source.getProject(), true);
        command.addOptions("--force");
        command.addFiles(source, dest);
        command.executeToBytes();
    }
}