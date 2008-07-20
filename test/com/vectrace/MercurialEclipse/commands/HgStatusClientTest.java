/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

import junit.framework.TestCase;

/**
 * @author Stefan
 *
 */
public class HgStatusClientTest extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        TestConfiguration cfg = new TestConfiguration();
        HgClients.initialize(cfg, cfg, cfg);
    }
    public void testGetStatus() throws Exception {
        HgStatusClient.getStatus(new File("."));
      //   System.out.println(status);
    }
}