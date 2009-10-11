package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgLogClient extends AbstractParseChangesetClient {

    private static final Pattern GET_REVISIONS_PATTERN = Pattern
            .compile("^([0-9]+):([a-f0-9]+) ([^ ]+ [^ ]+ [^ ]+) ([^#]+)#(.*)\\*\\*#(.*)$"); //$NON-NLS-1$

    public static final String NOLIMIT = "999999999999";

    public static ChangeSet[] getHeads(IProject project) throws HgException {
        HgCommand command = new HgCommand("heads", project, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        return getRevisions(command);
    }

    /**
     * @param command
     *            a command with optionally its Files set
     */
    private static ChangeSet[] getRevisions(HgCommand command)
            throws HgException {
        command.addOptions("--template", //$NON-NLS-1$
                "{rev}:{node} {date|isodate} {author|person}#{branches}**#{desc|firstline}\n"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        String[] lines = null;
        try {
            lines = command.executeToString().split("\n"); //$NON-NLS-1$
        } catch (HgException e) {
            if (!e.getMessage()
                    .contains(
                            "abort: can only follow copies/renames for explicit file names")) { //$NON-NLS-1$
                throw new HgException(e);
            }
            return null;
        }
        int length = lines.length;
        ChangeSet[] changeSets = new ChangeSet[length];
        HgRoot root = command.getHgRoot();
        for (int i = 0; i < length; i++) {
            Matcher m = GET_REVISIONS_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                ChangeSet changeSet = new ChangeSet.Builder(
                        Integer.parseInt(m.group(1)), // revisions
                        m.group(2), // changeset
                        m.group(5), // branch
                        m.group(3), // date
                        m.group(4), // user
                        root).description(m.group(6)).build();

                changeSets[i] = changeSet;
            } else {
                throw new HgException(Messages.getString("HgLogClient.parseException") + lines[i] + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }

        }

        return changeSets;
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getCompleteProjectLog(
            IResource res, boolean withFiles) throws HgException {
        return getProjectLog(res, -1, -1, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getProjectLogBatch(
            IResource res, int batchSize, int startRev, boolean withFiles)
            throws HgException {
        return getProjectLog(res, batchSize, startRev, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getRecentProjectLog(
            IResource res, int limitNumber, boolean withFiles) throws HgException {
        return getProjectLogBatch(res, limitNumber, -1, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getProjectLog(IResource res,
            int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("log", getWorkingDirectory(res), //$NON-NLS-1$
                    false);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
            command.addOptions("--debug", "--style", //$NON-NLS-1$ //$NON-NLS-2$
                    AbstractParseChangesetClient.getStyleFile(withFiles)
                            .getCanonicalPath());

            addRange(command, startRev, limitNumber);

            if (res.getType() == IResource.FILE) {
                command.addOptions("-f"); //$NON-NLS-1$
            }

            if (res.getType() != IResource.PROJECT) {
                command.addOptions(res.getLocation().toOSString());
            }

            String result = command.executeToString();
            if (result.length() == 0) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, withFiles, Direction.LOCAL, null, null, new IFilePatch[0]);
            return revisions;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    public static Map<IPath, SortedSet<ChangeSet>> getPathLog(boolean isFile, File path,
            HgRoot root, int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("log", root, //$NON-NLS-1$
                    false);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
            command.addOptions("--debug", "--style", //$NON-NLS-1$ //$NON-NLS-2$
                    AbstractParseChangesetClient.getStyleFile(withFiles)
                    .getCanonicalPath());

            addRange(command, startRev, limitNumber);

            if (isFile) {
                command.addOptions("-f"); //$NON-NLS-1$
            }

            command.addOptions(root.toRelative(path));

            String result = command.executeToString();
            if (result.length() == 0) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    new Path(path.getAbsolutePath()),
                    result, Direction.LOCAL, null, null, new IFilePatch[0], root);
            return revisions;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    public static void addRange(AbstractShellCommand command, int startRev, int limitNumber) {
        if (startRev >= 0 && startRev != Integer.MAX_VALUE) {
            int last = Math.max(startRev - limitNumber, 0);
            command.addOptions("-r"); //$NON-NLS-1$
            command.addOptions(startRev + ":" + last); //$NON-NLS-1$
        }
        setLimit(command, limitNumber);
    }

    public static void setLimit(AbstractShellCommand command, int limitNumber) {
        command.addOptions("--limit", (limitNumber > 0) ? limitNumber + "" : NOLIMIT); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     *
     * @param rev non null
     * @param history non null
     * @param monitor non null
     * @return may return null
     */
    public static ChangeSet getLogWithBranchInfo(MercurialRevision rev,
            MercurialHistory history, IProgressMonitor monitor) throws HgException {
        ChangeSet changeSet = rev.getChangeSet();
        IResource resource = rev.getResource();
        int limitNumber = 1;
        Map<IPath, SortedSet<ChangeSet>> map = getProjectLog(resource, limitNumber, changeSet
                .getChangesetIndex(), true);
        IPath location = ResourceUtils.getPath(resource);
        if(map != null) {
            return map.get(location).first();
        }
        File possibleParent = rev.getParent();
        MercurialRevision next = rev;
        if(possibleParent == null && !monitor.isCanceled()){
            HgRoot hgRoot = changeSet.getHgRoot();
            File file = location.toFile();

            // go up one revision, looking for the fist time "branch" occurence
            while((next = history.getNext(next)) != null && !monitor.isCanceled()){
                if(next.getParent() == null) {
                    int revision = next.getRevision();
                    possibleParent = HgStatusClient.getPossibleSourcePath(hgRoot, file, revision);
                } else {
                    possibleParent = next.getParent();
                }
                if(possibleParent != null){
                    // validate if the possible parent IS the parent for this version
                    map = getPathLog(resource.getType() == IResource.FILE,
                            possibleParent, hgRoot, limitNumber, rev.getRevision(), true);
                    if(map != null) {
                        // bingo, log is not null
                        break;
                    }
                    // see issue 10302: file seems to be copied/renamed multiple times
                    // restart the search from the beginning with the newly obtained path
                    // if it is different to the original one
                    if(possibleParent.equals(location.toFile())){
                        // give up
                        possibleParent = null;
                        break;
                    }
                    // restart
                    next = rev;
                    file = possibleParent;
                }
            }

            if(monitor.isCanceled()){
                return null;
            }

            // remember parent for all visited versions
            if(possibleParent != null) {
                while((next = history.getPrev(next)) != rev){
                    if(next == null) {
                        break;
                    }
                    next.setParent(possibleParent);
                }
            }
        }

        if(possibleParent != null){
            rev.setParent(possibleParent);
            // TODO now one can check the changesets which may have exist for the *branched* file only.
            if(map == null && !monitor.isCanceled()) {
                map = getPathLog(resource.getType() == IResource.FILE,
                        possibleParent, MercurialTeamProvider.getHgRoot(resource),
                        limitNumber, rev.getRevision(), true);
            }
            if(map != null) {
                return map.get(new Path(possibleParent.getAbsolutePath())).first();
            }
        }
        return null;
    }

    public static ChangeSet getChangeset(IResource res, String nodeId,
            boolean withFiles) throws HgException {

        try {
            Assert.isNotNull(nodeId);

            AbstractShellCommand command = new HgCommand("log", res.getProject().getLocation().toFile(), //$NON-NLS-1$
                    false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
            command.addOptions("--debug", "--style", AbstractParseChangesetClient //$NON-NLS-1$ //$NON-NLS-2$
                    .getStyleFile(withFiles).getCanonicalPath());
            command.addOptions("--rev", nodeId); //$NON-NLS-1$
            String result = command.executeToString();

            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, withFiles, Direction.LOCAL, null, null, new IFilePatch[0]);
            SortedSet<ChangeSet> set = revisions.get(res.getLocation());
            if (set != null) {
                return set.first();
            }
            return null;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }
}
