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
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.aragost.javahg.ext.mq.Patch;
import com.aragost.javahg.ext.mq.QSeriesCommand;
import com.aragost.javahg.ext.mq.flags.QSeriesCommandFlags;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgQSeriesClient extends AbstractClient {
	public static List<Patch> getPatchesInSeries(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("qseries", //$NON-NLS-1$
				"Invoking qseries", root, true);

		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("-v"); //$NON-NLS-1$
		command.addOptions("--summary"); //$NON-NLS-1$
		return parse(command.executeToString());
	}

	protected static List<Patch> parse(String executeToString) {
		List<Patch> list = new ArrayList<Patch>();
		if (executeToString != null && executeToString.indexOf("\n") >= 0) { //$NON-NLS-1$
			String[] patches = executeToString.split("\n"); //$NON-NLS-1$
			int i = 1;

			for (String string : patches) {
				String[] components = string.split(":", 2); //$NON-NLS-1$
				String[] patchData = components[0].trim().split(" ", 3); //$NON-NLS-1$

				list.add(new Patch(patchData[2].trim(), "A".equals(patchData[1]), components[1]
						.trim(), i++));
			}
		}
		return list;
	}

	public static List<Patch> getPatchesNotInSeries(IResource resource) throws HgException {
		AbstractShellCommand command = new HgCommand("qseries", //$NON-NLS-1$
				"Invoking qseries", resource, true);
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		command.addOptions("--summary", "--missing"); //$NON-NLS-1$ //$NON-NLS-2$
		return parse(command.executeToString());
	}
}
