package com.vectrace.MercurialEclipse.commands;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgStatusClient {

	public static String getStatus(IContainer root) throws HgException {
		HgCommand command = new HgCommand("status", root, true);
		//modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduic");
		return command.executeToString();
	}
	
	public static String getStatus(IResource res) throws HgException {
		HgCommand command = new HgCommand("status", res.getProject(), true);
		//modified, added, removed, deleted, unknown, ignored, clean
		command.addOptions("-marduic");
		command.addOptions(res.getProjectRelativePath().toOSString());
		return command.executeToString();
	}
	
	public static String[] getUntrackedFiles(IContainer root) throws HgException {
		HgCommand command = new HgCommand("status", root, true);
		command.addOptions("-u", "-n");
		return command.executeToString().split("\n");
	}

	public static boolean isDirty(List<? extends IResource> resources) throws HgException {
		HgCommand command = new HgCommand("status", true);
		command.addOptions("-mard");//modified, added, removed, deleted
		command.addFiles(resources);
		return command.executeToBytes().length != 0;
	}
	
    public static boolean isDirty(IProject project) throws HgException {
        HgCommand command = new HgCommand("status", project, true);
        command.addOptions("-mard");//modified, added, removed, deleted
        return command.executeToBytes().length != 0;
    }
	
}
