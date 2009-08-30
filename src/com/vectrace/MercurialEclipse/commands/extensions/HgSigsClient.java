/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
public class HgSigsClient extends AbstractClient {

    private static HgException notAvailable;

    /**
     * Gets signed changesets
     *
     * @param repoFile
     * @return the identifiers of signed changesets (rev:node)
     * @throws HgException
     */
    public static List<Signature> getSigs(File repoFile) throws CoreException {
        try {
            HgRoot root = MercurialTeamProvider.getHgRoot(repoFile);
            List<Signature> nodes = new ArrayList<Signature>();
            File sigFile = new File(root, ".hgsigs"); //$NON-NLS-1$
            if (sigFile.exists()) {
                LineNumberReader reader = null;
                try {
                  reader = new LineNumberReader(new FileReader(sigFile));
                  String line = reader.readLine();
                  while (line != null) {
                      String nodeId = line.substring(0,line.indexOf(" 0 ")); //$NON-NLS-1$
                      Signature sig = new Signature(null,nodeId,root);
                      nodes.add(sig);
                      line = reader.readLine();
                  }
                } catch (IOException e) {
                    throw new HgException(e.getLocalizedMessage(), e);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
            return nodes;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @return may return null, if "sigcheck" command is not available
     * @throws HgException if command failed
     */
    public static String checkSig(File file, String nodeId) throws HgException {
        if(notAvailable != null){
            return null;
        }
        try {
            AbstractShellCommand c = new HgCommand("sigcheck", getWorkingDirectory(file), //$NON-NLS-1$
                    false);
            c.setUsePreferenceTimeout(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
            c.addOptions(nodeId);
            return c.executeToString();
        } catch (HgException e) {
            String message = e.getMessage();
            if(message != null && message.contains("unknown") && message.contains("'sigcheck'")){
                MercurialEclipsePlugin.logError(e);
                notAvailable = e;
                return null;
            }
            throw e;
        }
    }
}