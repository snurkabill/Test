/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * Calls hg strip
 * 
 * @author bastian
 * 
 */
public class HgStripClient {
    /**
     * strip a revision and all later revs on the same branch
     * 
     * @param proj
     * @param backup
     * @param changeset
     * @return
     * @throws HgException
     */
    public static String strip(IProject proj, boolean saveUnrelated,
            boolean backup, boolean stripHeads, ChangeSet changeset)
            throws HgException {
        HgCommand command = new HgCommand("strip", proj, true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.COMMIT_TIMEOUT);
        if (saveUnrelated) {
            command.addOptions("--backup");
        }
        if (!backup) {
            command.addOptions("--nobackup");
        }
        if (stripHeads) {
            command.addOptions("-f");
        }
        command.addOptions(changeset.getChangeset());
        return command.executeToString();
    }
}