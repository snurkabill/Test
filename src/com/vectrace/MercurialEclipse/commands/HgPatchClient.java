import com.vectrace.MercurialEclipse.history.MercurialRevision;
	public static String getDiff(HgRoot hgRoot, MercurialRevision entry, MercurialRevision secondEntry) throws HgException {
		HgCommand command = new HgCommand("diff", hgRoot, true);
		if( secondEntry == null ){
			command.addOptions("-c", "" + entry.getChangeSet().getRevision().getChangeset());
		} else {
			command.addOptions("-r", ""+entry.getChangeSet().getRevision().getChangeset());
			command.addOptions("-r", ""+secondEntry.getChangeSet().getRevision().getChangeset());
		}
		command.addOptions("--git");
		return command.executeToString();
	}

	public static enum DiffLineType { HEADER, META, ADDED, REMOVED, CONTEXT }

	// TODO Check this against git diff specification
	public static DiffLineType getDiffLineType(String line) {
		if(line.startsWith("diff ")) {
			return DiffLineType.HEADER;
		} else if(line.startsWith("+++ ")) {
			return DiffLineType.META;
		} else if(line.startsWith("--- ")) {
			return DiffLineType.META;
		} else if(line.startsWith("@@ ")) {
			return DiffLineType.META;
			// TODO there are some more things
		} else if(line.startsWith("new file mode")) {
			return DiffLineType.META;
		} else if(line.startsWith("\\ ")) {
			return DiffLineType.META;
		} else if(line.startsWith("+")) {
			return DiffLineType.ADDED;
		} else if(line.startsWith("-")) {
			return DiffLineType.REMOVED;
		} else {
			return DiffLineType.CONTEXT;
		}
	}


