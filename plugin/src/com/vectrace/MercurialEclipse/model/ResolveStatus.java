/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import com.aragost.javahg.commands.ResolveStatusLine;
import com.aragost.javahg.merge.KeepDeleteConflict;

public class ResolveStatus implements IAdaptable {

	public static enum Type {
		RESOLVED( 'R' ), UNRESOLVED( 'U' ), REMOTE_DELETED('X'), LOCAL_DELETED('Y');

		private char ch;

		private Type( char ch ) {
			this.ch = ch;
		}
	}

	private final IResource adaptable;
	private final Type flag;

	protected Type typeForChar( char ch ) {
		for ( Type t : Type.values() ) {
			if ( t.ch == ch ) {
				return t;
			}
		}
		throw new IllegalArgumentException( "No match for " + ch );
	}

	public ResolveStatus(IResource adaptable, KeepDeleteConflict c) {
		this.adaptable = adaptable;
		boolean remoteDeleted = c.getMergeCtx().getLocal().getBranch().equals( c.getKeepParent().getBranch() );
		this.flag = remoteDeleted ? Type.REMOTE_DELETED : Type.LOCAL_DELETED;
	}

	public ResolveStatus(IResource adaptable, ResolveStatusLine.Type flag) {
		this.adaptable = adaptable;
		this.flag = flag == ResolveStatusLine.Type.RESOLVED ? Type.RESOLVED : Type.UNRESOLVED;
	}

	public IResource getResource() {
		return this.adaptable;
	}

	public Object getAdapter(Class adapter) {
		if (adaptable == null) {
			return null;
		}
		return adaptable.getAdapter(adapter);
	}

	public Type getFlag() {
		return this.flag;
	}

	public String getStatus() {
		if ( flag == Type.REMOTE_DELETED ) {
			return Messages.getString("FlaggedAdaptable.remoteDeleted");
		}
		else if ( flag == Type.LOCAL_DELETED ) {
			return Messages.getString("FlaggedAdaptable.localDeleted");
		}
		else if ( flag == Type.UNRESOLVED ) {
			return Messages.getString("FlaggedAdaptable.unresolvedStatus");
		}
		return Messages.getString("FlaggedAdaptable.resolvedStatus");
	}

	public boolean isUnresolved() {
		return flag != Type.RESOLVED;
	}

	public boolean isResolved() {
		return flag == Type.RESOLVED;
	}
}
