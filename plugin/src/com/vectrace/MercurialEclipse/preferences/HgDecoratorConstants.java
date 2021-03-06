/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian         - implementation
 * Amenel Voglozin - Support for user-specified syntax of project labels
 *******************************************************************************/
package com.vectrace.MercurialEclipse.preferences;

/**
 * @author bastian
 *
 */
public final class HgDecoratorConstants {

	private HgDecoratorConstants() {
		// hide constructor of utility class.
	}

	public static final String CHANGE_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.changedBackgroundColor"; //$NON-NLS-1$
	public static final String CHANGE_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.changedFont"; //$NON-NLS-1$
	public static final String CHANGE_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.changedForegroundColor"; //$NON-NLS-1$

	public static final String IGNORED_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.ignoredBackgroundColor"; //$NON-NLS-1$
	public static final String IGNORED_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.ignoredFont"; //$NON-NLS-1$
	public static final String IGNORED_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.ignoredForegroundColor"; //$NON-NLS-1$

	public static final String ADDED_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.addedBackgroundColor"; //$NON-NLS-1$
	public static final String ADDED_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.addedFont"; //$NON-NLS-1$
	public static final String ADDED_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.addedForegroundColor"; //$NON-NLS-1$

	public static final String REMOVED_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.removedBackgroundColor"; //$NON-NLS-1$
	public static final String REMOVED_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.removedFont"; //$NON-NLS-1$
	public static final String REMOVED_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.removedForegroundColor"; //$NON-NLS-1$

	public static final String CONFLICT_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.conflictBackgroundColor"; //$NON-NLS-1$
	public static final String CONFLICT_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.conflictFont"; //$NON-NLS-1$
	public static final String CONFLICT_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.conflictForegroundColor"; //$NON-NLS-1$

	public static final String UNKNOWN_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.unknownBackgroundColor"; //$NON-NLS-1$
	public static final String UNKNOWN_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.unknownForegroundColor"; //$NON-NLS-1$
	public static final String UNKNOWN_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.unknownFont"; //$NON-NLS-1$

	public static final String DELETED_BACKGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.deletedBackgroundColor"; //$NON-NLS-1$
	public static final String DELETED_FOREGROUND_COLOR = "com.vectrace.mercurialeclipse.ui.colorsandfonts.deletedForegroundColor"; //$NON-NLS-1$
	public static final String DELETED_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.deletedFont"; //$NON-NLS-1$

	//
	// Keywords supported in the decoration string of projects.
	//
	public static final String LEX_AUTHOR = "author"; //$NON-NLS-1$
	public static final String LEX_BRANCH = "branch"; //$NON-NLS-1$
	public static final String LEX_HEADS = "heads"; //$NON-NLS-1$
	public static final String LEX_INDEX = "index"; //$NON-NLS-1$
	public static final String LEX_NODE = "node"; //$NON-NLS-1$
	public static final String LEX_HEX = "hex"; //$NON-NLS-1$
	public static final String LEX_OUTGOING = "outgoing"; //$NON-NLS-1$
	public static final String LEX_REPO = "repo"; //$NON-NLS-1$
	public static final String LEX_TAGS = "tags"; //$NON-NLS-1$
	public static final String LEX_MERGING_STATUS = "merging"; //$NON-NLS-1$
	public static final String LEX_REBASING_STATUS = "rebasing"; //$NON-NLS-1$
	public static final String LEX_BISECTING_STATUS = "bisecting"; //$NON-NLS-1$

	/**
	 * Default string for user-definable project labels syntax. Defined in a manner to be as close
	 * as possible to the syntax that existed before the implementation of configurable project
	 * labels. Close but not quite identical.
	 */
	public static final String DEFAULT_PROJECT_SYNTAX = "{repo} {branch} {\u2191}{outgoing} {,}{heads} {t:}{tags} {index}:{hex} {merging} {rebasing} {bisecting}";

}
