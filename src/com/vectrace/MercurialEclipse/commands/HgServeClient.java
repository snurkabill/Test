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
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 * 
 */
public class HgServeClient {

    class HgServeJob extends SafeWorkspaceJob {
        private IResource hgRoot = null;
        private boolean ipv6;
        private String name;
        private String prefix;
        private int port;
        private String webdirConf;
        private boolean stdio;

        public HgServeJob(IResource hgRoot, int port, String prefix,
                String name, String webdirConf, boolean ipv6, boolean stdio) {
            super("Controller thread for serving repository "
                    + hgRoot.getName() + "...");
            this.hgRoot = hgRoot;
            this.port = port;
            this.prefix = prefix;
            this.name = name;
            this.webdirConf = webdirConf;
            this.ipv6 = ipv6;
            this.stdio = stdio;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
         */
        @Override
        protected IStatus runSafe(IProgressMonitor monitor) {
            try {
                final AbstractShellCommand command = HgServeClient.getCommand(
                        hgRoot, port, prefix, name, webdirConf, stdio, ipv6);

                SafeWorkspaceJob job = new SafeWorkspaceJob(
                        "Server thread for:" + command.getCommands().toString().replace(",", "")) {

                    /*
                     * (non-Javadoc)
                     * 
                     * @see com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org.eclipse.core.runtime.IProgressMonitor)
                     */
                    @Override
                    protected IStatus runSafe(IProgressMonitor m) {
                        try {
                            command.executeToBytes(Integer.MAX_VALUE, false);
                        } catch (HgException e) {
                            MercurialEclipsePlugin.logError(e);
                        }
                        return super.runSafe(m);
                    }
                };
                job.schedule();

                try {
                    while (!monitor.isCanceled()) {
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                }

                command.terminate();
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
            }
            return super.runSafe(monitor);
        }
    }

    public void serve(IResource hgRoot, int port, String prefixPath,
            String name, String webdirConf, boolean stdio, boolean ipv6)
            throws IOException {
        new HgServeJob(hgRoot, port, prefixPath, name, webdirConf, ipv6, stdio)
                .schedule();
    }

    /**
     * 
     * @param hgRoot
     * @param port
     * @param prefixPath
     * @param name
     * @param webdirConf
     * @param stdio
     * @param ipv6
     * @return the command
     * @throws HgException
     * @throws IOException
     */
    private static AbstractShellCommand getCommand(IResource hgRoot, int port,
            String prefixPath, String name, String webdirConf, boolean stdio,
            boolean ipv6) throws IOException {
        final AbstractShellCommand command = new HgCommand("serve", new File(
                hgRoot.getLocation().toOSString()), true);
        File pidFile = File.createTempFile("hgserve_" + hgRoot.getName(),
                ".pidfile");
        pidFile.deleteOnExit();
        command.addOptions("--port", String.valueOf(port));
        if (prefixPath != null && prefixPath.length() > 0) {
            command.addOptions("--prefix", prefixPath);
        }
        if (name != null && name.length() > 0) {
            command.addOptions("--name", name);
        }
        if (webdirConf != null && webdirConf.length() > 0) {
            command.addOptions("--webdir-conf", webdirConf);
        }
        if (stdio) {
            command.addOptions("--stdio");
        }
        if (ipv6) {
            command.addOptions("--ipv6");
        }
        // start daemon
        return command;
    }

}