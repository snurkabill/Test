/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class MercurialSynchronizeSubscriber extends Subscriber /*implements Observer*/ {


    private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache.getInstance();

    private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache.getInstance();

    private static final OutgoingChangesetCache OUTGOING_CACHE = OutgoingChangesetCache.getInstance();

    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

    private final boolean debug;
    private final ISynchronizationScope scope;
    private IResource[] myRoots;
    private IResourceVariantComparator comparator;
    private final Semaphore sema;

    public MercurialSynchronizeSubscriber(ISynchronizationScope synchronizationScope) {
        debug = MercurialEclipsePlugin.getDefault().isDebugging();
        scope = synchronizationScope;
        sema = new Semaphore(1, true);
    }

    @Override
    public String getName() {
        return Messages.getString("MercurialSynchronizeSubscriber.repoWatcher"); //$NON-NLS-1$
    }

    @Override
    public IResourceVariantComparator getResourceComparator() {
        if (comparator == null) {
            comparator = new MercurialResourceVariantComparator();
        }
        return comparator;
    }

    @Override
    public SyncInfo getSyncInfo(IResource resource) {
        if (!isInteresting(resource)) {
            return null;
        }
        HgRepositoryLocation repositoryLocation = getRepo(resource);

        try {
            if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
                // waiting didn't worked for us...
                return null;
            }
        } catch (InterruptedException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }

        ChangeSet csOutgoing;
        try {
            // this can trigger a refresh and a call to the remote server...
            csOutgoing = OUTGOING_CACHE.getNewestOutgoingChangeSet(resource, repositoryLocation);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        } finally {
            sema.release();
        }

        MercurialRevisionStorage outgoingIStorage;
        IResourceVariant outgoing;
        // determine outgoing revision
        if (csOutgoing != null) {
            outgoingIStorage = new MercurialRevisionStorage(resource,
                    csOutgoing.getRevision().getRevision(),
                    csOutgoing.getChangeset(), csOutgoing);

            outgoing = new MercurialResourceVariant(outgoingIStorage);
        } else {
            // if outgoing != null it's our base, else we gotta construct one
            boolean exists = resource.exists();
            if (exists && !STATUS_CACHE.isAdded(resource.getProject(), resource.getLocation())
                    || (!exists && STATUS_CACHE.isRemoved(resource))) {

                try {
                    // Find current working directory changeset (not head)
                    HgRoot root = AbstractClient.getHgRoot(resource);
                    String nodeId = HgIdentClient.getCurrentChangesetId(root);

                    // try to get from cache (without loading)
                    csOutgoing = LOCAL_CACHE.getChangeset(resource.getProject(), nodeId);

                    // okay, we gotta load the changeset via hg log
                    if (csOutgoing == null) {
                        csOutgoing = LOCAL_CACHE.getLocalChangeSet(resource, nodeId, true);
                    }
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return null;
                }

                // construct base revision
                outgoingIStorage = new MercurialRevisionStorage(resource,
                        csOutgoing.getChangesetIndex(), csOutgoing.getChangeset(), csOutgoing);

                outgoing = new MercurialResourceVariant(outgoingIStorage);
            } else {
                // new incoming file - no local available
                outgoingIStorage = null;
                outgoing = null;
            }
        }

        // determine incoming revision get newest incoming changeset
        try {
            if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
                // waiting didn't worked for us...
                return null;
            }
        } catch (InterruptedException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
        ChangeSet csIncoming;
        try {
            // this can trigger a refresh and a call to the remote server...
            csIncoming = INCOMING_CACHE.getNewestIncomingChangeSet(resource, repositoryLocation);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        } finally {
            sema.release();
        }

        MercurialRevisionStorage incomingIStorage;
        if (csIncoming != null) {
            boolean fileRemoved = csIncoming.isRemoved(resource);
            if(fileRemoved){
                incomingIStorage = null;
            } else {
                incomingIStorage = getIncomingIStorage(resource, csIncoming);
            }
        } else {
            // if no incoming revision, incoming = base/outgoing
            incomingIStorage = outgoingIStorage;
        }

        IResourceVariant incoming;
        if (incomingIStorage != null) {
            incoming = new MercurialResourceVariant(incomingIStorage);
        } else {
            // neither base nor outgoing nor incoming revision
            incoming = null;
        }

        // now create the sync info object. everything may be null,
        // but resource and comparator
        SyncInfo info = new MercurialSyncInfo(resource, outgoing, incoming, getResourceComparator());

        try {
            info.init();
            return info;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            return null;
        }
    }

    private boolean isInteresting(IResource resource) {
        return resource != null
                && RepositoryProvider.getProvider(resource.getProject(), MercurialTeamProvider.ID) != null
                && (isSupervised(resource) || (!resource.exists()));
    }

    private MercurialRevisionStorage getIncomingIStorage(IResource resource,
            ChangeSet csRemote) {
        MercurialRevisionStorage incomingIStorage = new MercurialRevisionStorage(
                resource, csRemote.getRevision().getRevision(), csRemote
                .getChangeset(), csRemote);
        return incomingIStorage;
    }

    @Override
    public boolean isSupervised(IResource resource) {
        return resource.getType() == IResource.FILE && MercurialUtilities.isPossiblySupervised(resource);
    }

    @Override
    public IResource[] members(IResource resource) throws TeamException {

        HgRepositoryLocation repositoryLocation = getRepo(resource);
        Set<IResource> members = new HashSet<IResource>();
        Set<IResource> localMembers = STATUS_CACHE.getLocalMembers(resource);
        if (localMembers.size() > 0) {
            members.addAll(localMembers);
        }

        Set<IResource> outgoingMembers;
        Set<IResource> incomingMembers;
        try {
            if(!sema.tryAcquire(60 * 5, TimeUnit.SECONDS)){
                // waiting didn't worked for us...
                return getAllWithoutGivenOne(resource, members);
            }
            if(resource instanceof IContainer && debug) {
                System.out.println("get members: " + resource);
            }
            // this can trigger a refresh and a call to the remote server...
            outgoingMembers = OUTGOING_CACHE.getOutgoingMembers(resource, repositoryLocation);
            incomingMembers = INCOMING_CACHE.getIncomingMembers(resource, repositoryLocation);
        } catch (InterruptedException e) {
            MercurialEclipsePlugin.logError(e);
            return getAllWithoutGivenOne(resource, members);
        } finally {
            sema.release();
        }

        if (outgoingMembers.size() > 0) {
            members.addAll(outgoingMembers);
        }
        if (incomingMembers.size() > 0) {
            members.addAll(incomingMembers);
        }

        // we don't want ourself or the project as our member
        return getAllWithoutGivenOne(resource, members);
    }

    private IResource[] getAllWithoutGivenOne(IResource resource, Set<IResource> members) {
        members.remove(resource.getProject());
        members.remove(resource);
        return members.toArray(new IResource[members.size()]);
    }

    /**
     * @param flag one of {@link HgSubscriberScopeManager} constants, if the value is negative,
     * otherwise some depth hints from the Team API (which are ignored here).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void refresh(IResource[] resources, int flag, IProgressMonitor monitor) throws TeamException {
        if (resources == null) {
            return;
        }

        Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(Arrays.asList(resources));
        Set<IProject> projects = byProject.keySet();
        if(projects.isEmpty()){
            return;
        }

        Set<IResource> resourcesToRefresh = new HashSet<IResource>();

        for (IProject project : projects) {
            HgRepositoryLocation repositoryLocation = getRepo(project);
            if (repositoryLocation == null) {
                continue;
            }

            // clear caches in any case, but refresh them only if project exists
            boolean forceRefresh = project.exists();

            try {
                sema.acquire();
                if(debug) {
                    System.out.println("going to refresh: " + project + ", depth: " + flag);
                }
                monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingIncoming")); //$NON-NLS-1$
                refreshIncoming(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
                monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingOutgoing")); //$NON-NLS-1$
                refreshOutgoing(flag, resourcesToRefresh, project, repositoryLocation, forceRefresh);
                monitor.worked(1);
                if (monitor.isCanceled()) {
                    return;
                }
            } catch (InterruptedException e) {
                MercurialEclipsePlugin.logError(e);
            } finally {
                sema.release();
            }

            monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.refreshingLocal")); //$NON-NLS-1$
            refreshLocal(flag, monitor, project, forceRefresh);
            monitor.worked(1);
            if (monitor.isCanceled()) {
                return;
            }
        }

        List<ISubscriberChangeEvent> changeEvents = createEvents(resources, resourcesToRefresh);
        monitor.worked(1);
        if (monitor.isCanceled()) {
            return;
        }

        monitor.subTask(Messages.getString("MercurialSynchronizeSubscriber.triggeringStatusCalc")); //$NON-NLS-1$
        fireTeamResourceChange(changeEvents.toArray(new ISubscriberChangeEvent[changeEvents.size()]));
        monitor.worked(1);
        // should not call it, we do not own the monitor...
        // monitor.done();
    }

    private List<ISubscriberChangeEvent> createEvents(IResource[] resources,
            Set<IResource> resourcesToRefresh) {
        for (IResource resource : resources) {
            if(resource.getType() == IResource.FILE) {
                resourcesToRefresh.add(resource);
            } else {
                Set<IResource> localMembers = STATUS_CACHE.getLocalMembers(resource);
                resourcesToRefresh.addAll(localMembers);
            }
        }
        List<ISubscriberChangeEvent> changeEvents = new ArrayList<ISubscriberChangeEvent>();
        for (IResource res : resourcesToRefresh) {
            changeEvents.add(new SubscriberChangeEvent(this, ISubscriberChangeEvent.SYNC_CHANGED, res));
        }
        return changeEvents;
    }

    private void refreshLocal(int flag, IProgressMonitor monitor, IProject project,
            boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.LOCAL || flag >= 0) {
            STATUS_CACHE.clear(project, false);
            if(forceRefresh) {
                STATUS_CACHE.refreshStatus(project, monitor);
            }
        }
    }

    private void refreshIncoming(int flag, Set<IResource> resourcesToRefresh, IProject project,
            HgRepositoryLocation repositoryLocation, boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.INCOMING || flag >= 0) {
            if(debug) {
                System.out.println("\nclear incoming: " + project + ", depth: " + flag);
            }
            INCOMING_CACHE.clear(repositoryLocation, project, false);
        }
        if(forceRefresh && flag != HgSubscriberScopeManager.OUTGOING){
            if(debug) {
                System.out.println("\nget incoming: " + project + ", depth: " + flag);
            }
            // this can trigger a refresh and a call to the remote server...
            Set<IResource> incomingMembers = INCOMING_CACHE.getIncomingMembers(project, repositoryLocation);
            resourcesToRefresh.addAll(incomingMembers);
        }
    }

    private void refreshOutgoing(int flag, Set<IResource> resourcesToRefresh, IProject project,
            HgRepositoryLocation repositoryLocation, boolean forceRefresh) throws HgException {
        if(flag == HgSubscriberScopeManager.OUTGOING || flag >= 0) {
            if(debug) {
                System.out.println("\nclear outgoing: " + project + ", depth: " + flag);
            }
            OUTGOING_CACHE.clear(repositoryLocation, project, false);
        }
        if(forceRefresh && flag != HgSubscriberScopeManager.INCOMING){
            if(debug) {
                System.out.println("\nget outgoing: " + project + ", depth: " + flag);
            }
            // this can trigger a refresh and a call to the remote server...
            Set<IResource> outgoingMembers = OUTGOING_CACHE.getOutgoingMembers(project, repositoryLocation);
            resourcesToRefresh.addAll(outgoingMembers);
        }
    }

    protected HgRepositoryLocation getRepo(IResource resource){
        if(scope instanceof RepositorySynchronizationScope){
            RepositorySynchronizationScope repoScope = (RepositorySynchronizationScope) scope;
            return repoScope.getRepositoryLocation();
        }
        return MercurialEclipsePlugin.getRepoManager().getDefaultProjectRepoLocation(resource.getProject());
    }

    @Override
    public IResource[] roots() {
        if (myRoots == null) {
            if (scope != null && scope.getRoots() != null) {
                myRoots = scope.getRoots();
            } else {
                myRoots = MercurialStatusCache.getInstance().getAllManagedProjects();
            }
        }
        return myRoots;
    }

    /**
     * Overriden to made it accessible from {@link HgSubscriberScopeManager#update(java.util.Observable, Object)}
     * {@inheritDoc}
     */
    @Override
    public void fireTeamResourceChange(ISubscriberChangeEvent[] deltas) {
        super.fireTeamResourceChange(deltas);
    }


}
