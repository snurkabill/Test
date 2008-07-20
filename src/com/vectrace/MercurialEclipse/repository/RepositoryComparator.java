/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.util.Comparator;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * This class allows to sort HgRepositoryLoction's alphabetically using the URL
 * or the label (if set). The case of the strings is ignored.
 */
public class RepositoryComparator implements Comparator<HgRepositoryLocation> {
    /**
     * @see java.util.Comparator#compare(Obejct, Object)
     */
    public int compare(HgRepositoryLocation o1, HgRepositoryLocation o2) {
        return o1.getUrl().compareToIgnoreCase(o2.getUrl());
    }
}