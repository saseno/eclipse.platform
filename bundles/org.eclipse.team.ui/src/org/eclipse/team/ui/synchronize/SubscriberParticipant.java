/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A synchronize participant that displays synchronization information for local resources that are 
 * managed via a {@link Subscriber}. It maintains a dynamic collection of all out-of-sync resources
 * by listening to workspace resource changes and remote changes thus creating a live view of
 * changes in the workspace.
 * <p>
 * The subscriber can be configured to be synchronized in the background based on a schedule. This
 * effectively refreshes the subscriber and updates the dynamic sync set.
 * </p><p>
 * Subclasses will typically want to override the following methods:
 * <ul>
 * <li>initializeConfiguration: participants can add toolbar actions, configure the context menu, decorator.
 * <li>saveState and init: persist settings between sessions.
 * </ul>
 * This class is intended to be subclassed. 
 * </p>
 * @since 3.0
 */
public abstract class SubscriberParticipant extends AbstractSynchronizeParticipant implements IPropertyChangeListener {
	
	/*
	 * Collects and maintains set of all out-of-sync resources of the subscriber
	 */
	private SubscriberSyncInfoCollector collector;
	
	/*
	 * Controls the automatic synchronization of this participant
	 */
	private SubscriberRefreshSchedule refreshSchedule;
	
	/*
	 * Provides the resource scope for this participant
	 */
	private ISynchronizeScope scope;
	
	/*
	 * Key for settings in memento
	 */
	private static final String CTX_SUBSCRIBER_PARTICIPANT_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBERSETTINGS"; //$NON-NLS-1$
	
	/*
	 * Key for schedule in memento
	 */
	private static final String CTX_SUBSCRIBER_SCHEDULE_SETTINGS = TeamUIPlugin.ID + ".TEAMSUBSRCIBER_REFRESHSCHEDULE"; //$NON-NLS-1$

	/**
	 * Constructor initializes the schedule. Subclasses must call this method.
	 */
	public SubscriberParticipant() {
		refreshSchedule = new SubscriberRefreshSchedule(this);
	}
	
	/**
	 * Constructor which should be called when creating a particpant whose resources
	 * are to be scoped.
	 * 
	 * @param scope a synchronize scope
	 */
	public SubscriberParticipant(ISynchronizeScope scope) {
		this();
		this.scope = scope;
		scope.addPropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeViewPage#createPage(org.eclipse.team.ui.sync.ISynchronizeView)
	 */
	public final IPageBookViewPage createPage(ISynchronizePageConfiguration configuration) {
		validateConfiguration(configuration);
		return new SubscriberParticipantPage(configuration, getSubscriberSyncInfoCollector());
	}
	
	/**
	 * Returns the resources supervised by this participant. It will
	 * either be the roots of the subscriber or the resource scope
	 * provided when the subscriber was set.
	 * 
	 * @return the resources supervised by this participant.
	 */
	public IResource[] getResources() {
		return collector.getRoots();
	}
	
	/*
	 * Set the resources supervised by this participant. If <code>null</code>,
	 * the participant will include all roots of its subscriber
	 * 
	 * @param roots the root resources to consider or <code>null</code>
	 * to consider all roots of the subscriber
	 */
	private void setResources(IResource[] roots) {
		collector.setRoots(roots);
	}
	
	/**
	 * Refresh this participants synchronization state and displays the result in a model dialog. 
	 * 
	 * @param resources
	 * @param taskName
	 * @param site
	 */
	public final void refreshInDialog(Shell shell, IResource[] resources, String jobName, String taskName, ISynchronizePageConfiguration configuration, IWorkbenchSite site) {
		IRefreshSubscriberListener listener =  new RefreshUserNotificationPolicyInModalDialog(shell, taskName, configuration, this);
		internalRefresh(resources, jobName, taskName, site, listener);
	}

	/**
	 * Refresh a participant in the background the result of the refresh are shown in the progress view. Refreshing 
	 * can also be considered synchronizing, or refreshing the synchronization state. Basically this is a long
	 * running operation that will update the participants sync info sets with new changes detected on the
	 * server. Eother or both of the <code>shortTaskName</code> and <code>longTaskName</code> can be <code>null</code>
	 * in whihc case, the default values for these are returned by the methods <code>getShortTaskName()</code> and
	 * <code>getLongTaskName(IResource[])</code> will be used.
	 * 
	 * @param resources the resources to be refreshed.
	 * @param shortTaskName the taskName of the background job that will run the synchronize or <code>null</code>
	 * if the default job name is desired.
	 * @param longTaskName the taskName of the progress monitor running the synchronize or <code>null</code>
	 * if the default job name is desired.
	 * @param site the workbench site the synchronize is running from. This can be used to notify the site
	 * that a job is running.
	 */
	public final void refresh(IResource[] resources, String shortTaskName, String longTaskName, IWorkbenchSite site) {
		IRefreshSubscriberListener listener = new RefreshUserNotificationPolicy(this);
		internalRefresh(resources, shortTaskName, longTaskName, site, listener);
	}

    /**
	 * Refresh a participant. The returned status describes the result of the refresh.
	 */
	public final IStatus refreshNow(IResource[] resources, String taskName, IProgressMonitor monitor) {
		Platform.getJobManager().cancel(this);
		RefreshSubscriberJob job = new RefreshSubscriberJob(this, taskName, taskName, resources, null);
		return job.run(monitor);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.AbstractSynchronizeViewPage#dispose()
	 */
	public void dispose() {
		Platform.getJobManager().cancel(this);
		refreshSchedule.dispose();				
		TeamUI.removePropertyChangeListener(this);
		collector.dispose();
		scope.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#getName()
	 */
	public String getName() {
		String name = super.getName();
		return Policy.bind("SubscriberParticipant.namePattern", name, scope.getName()); //$NON-NLS-1$
	}
	
	/**
	 * Return the name of the participant as specified in the plugin manifest file. 
	 * This method is provided to give access to this name since it is masked by
	 * the <code>getName()</code> method defined in this class.
	 * @return the name of the participant as specified in the plugin manifest file
	 * @since 3.1
	 */
	protected final String getShortName() {
	    return super.getName();
	}
	
	/**
	 * Returns the <code>SyncInfoTree</code> for this participant. This set
	 * contains the out-of-sync resources supervised by this participant. 
	 * 
	 * @return the sync info set that contains the out-of-sync resources
	 * for this participant.
	 */
	public SyncInfoTree getSyncInfoSet() {
		return getSubscriberSyncInfoCollector().getSyncInfoSet();
	}
	
	/**
	 * Return the <code>Subscriber</code> associated with this this participant. This
	 * method will only return <code>null</code> if the participant has not been initialized
	 * yet. 
	 * 
	 * @return the <code>Subscriber</code> associated with this this participant.
	 */
	public Subscriber getSubscriber() {
		if (collector == null) return null;
		return collector.getSubscriber();
	}
	
	/**
	 * Returns a participant that matches the given resource scoping
	 * 
	 * @param ID the type id of participants to match
	 * @param resources the resources to match in the scope
	 * @return  a participant that matches the given resource scoping
	 */
	public static SubscriberParticipant getMatchingParticipant(String ID, IResource[] resources) {
		ISynchronizeParticipantReference[] refs = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
			for (int i = 0; i < refs.length; i++) {
			ISynchronizeParticipantReference reference = refs[i];
			if(reference.getId().equals(ID)) {
					SubscriberParticipant p;
					try {
						p = (SubscriberParticipant)reference.getParticipant();
					} catch (TeamException e) {
						continue;
					}
					IResource[] roots = p.getResources();
					Arrays.sort(resources, Utils.resourceComparator);
					Arrays.sort(roots, Utils.resourceComparator);
					if (Arrays.equals(resources, roots)) {
						return p;
					}
			}
		}
		return null;
	}
		
	/* (non-Javadoc)
	 * @see IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(TeamUI.GLOBAL_IGNORES_CHANGED)) {
			collector.reset();
		}
		if (event.getProperty().equals(ISynchronizeScope.ROOTS)) {
			setResources(scope.getRoots());
		}
		if (event.getProperty().equals(ISynchronizeScope.NAME)) {
			// Force a name change event, which will cause this classes getName to be called
			// and updated with the correct working set name.
			firePropertyChange(this, IBasicPropertyConstants.P_TEXT, null, getName());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#init(org.eclipse.ui.IMemento)
	 */
	public void init(String secondaryId, IMemento memento) throws PartInitException {
		super.init(secondaryId, memento);
		if(memento != null) {
			IMemento settings = memento.getChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
			if(settings != null) {
				SubscriberRefreshSchedule schedule = SubscriberRefreshSchedule.init(settings.getChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS), this);
				setRefreshSchedule(schedule);
				this.scope = AbstractSynchronizeScope.createScope(settings);
				scope.addPropertyChangeListener(this);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento memento) {
		super.saveState(memento);
		IMemento settings = memento.createChild(CTX_SUBSCRIBER_PARTICIPANT_SETTINGS);
		refreshSchedule.saveState(settings.createChild(CTX_SUBSCRIBER_SCHEDULE_SETTINGS));
		AbstractSynchronizeScope.saveScope(scope, settings);
	}

	/**
	 * Reset the sync set of the particpant by repopulating it from scratch.
	 */
	public void reset() {
		getSubscriberSyncInfoCollector().reset();
	}
	
	/* (non-Javadoc)
	 * Return the <code>SubscriberSyncInfoCollector</code> for the participant.
	 * This collector maintains the set of all out-of-sync resources for the subscriber.
	 * 
	 * @return the <code>SubscriberSyncInfoCollector</code> for this participant
	 */
	public SubscriberSyncInfoCollector getSubscriberSyncInfoCollector() {
		return collector;
	}
	
	/*(non-Javadoc)
	 * Not to be called by clients.
	 */
	public void setRefreshSchedule(SubscriberRefreshSchedule schedule) {
		if (refreshSchedule == schedule) return;
		if (refreshSchedule != null) {
			refreshSchedule.dispose();
		}
		this.refreshSchedule = schedule;
	}
	
	/* (non-Javadoc)
	 * Not to be called by clients.
	 */
	public SubscriberRefreshSchedule getRefreshSchedule() {
		return refreshSchedule;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
	 */
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		configuration.setProperty(SynchronizePageConfiguration.P_PARTICIPANT_SYNC_INFO_SET, collector.getSyncInfoSet());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ISynchronizeParticipant#run(org.eclipse.ui.IWorkbenchPart)
	 */
	public void run(IWorkbenchPart part) {
		refresh(getResources(), null, null, part != null ? part.getSite() : null);
	}
	
	/**
	 * Returns the short task name (e.g. no more than 25 characters) to describe the behavior of the
	 * refresh operation to the user. This is typically shown in the status line when this subcriber is refreshed
	 * in the backgroud. When refreshed in the foreground, only the long task name is shown.
	 * 
	 * @return the short task name to show in the status line.
	 */
	protected String getShortTaskName() {
		return Policy.bind("Participant.synchronizing"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the long task name to describe the behavior of the
	 * refresh operation to the user. This is typically shown in the status line when this subcriber is refreshed
	 * in the background.
	 * 
	 * @return the long task name
	 * @deprecated use <code>getLongTaskName(IResource[]) instead</code>
	 */
	protected String getLongTaskName() {
		return Policy.bind("Participant.synchronizing"); //$NON-NLS-1$
	}
	
	/**
	 * Returns the long task name to describe the behavior of the
	 * refresh operation to the user. This is typically shown in the status line when this subcriber is refreshed
	 * in the backgroud.
     * @param resources
     * @return the long task name
     */
    protected String getLongTaskName(IResource[] resources) {
        int resourceCount = 0;
        if (getResources().length == resources.length) {
            // Assume that the resources are the same as the roots.
            // If we are wrong, the message may no mention the specific resources which is OK
            ISynchronizeScope scope = getScope();
	        if (scope instanceof ResourceScope) {
	            resourceCount = scope.getRoots().length;
	        }
        } else {
            resourceCount = resources.length;
        }
        if (resourceCount == 1) {
            return Policy.bind("Participant.synchronizingMoreDetails", getShortName(), resources[0].getFullPath().toString()); //$NON-NLS-1$
        } else if (resourceCount > 1) {
            return Policy.bind("Participant.synchronizingResources", getShortName(), Integer.toString(resourceCount)); //$NON-NLS-1$
        }
        // A resource count of zero means that it is a non-resource scope so we can print the scope name
        return Policy.bind("Participant.synchronizingDetails", getName()); //$NON-NLS-1$
    }

    /**
	 * This method is invoked before the given configuration is used to
	 * create the page (see <code>createPage(ISynchronizePageConfiguration)</code>).
	 * The configuration would have been initialized by 
	 * <code>initializeConfiguration(ISynchronizePageConfiguration)</code>
	 * but may have also been tailored further. This method gives the particpant 
	 * a chance to validate those changes before the page is created.
	 * 
	 * @param configuration the page configuration that is about to be used to create a page.
	 */
	protected void validateConfiguration(ISynchronizePageConfiguration configuration) {
		// Do nothing by default
	}
	
	/**
	 * Subclasses must call this method to initialize the participant. Typically this
	 * method is called in {@link #init(String, IMemento)}. This method will initialize
	 * the sync info collector.
	 * 
	 * @param subscriber the subscriber to associate with this participant.
	 */
	protected void setSubscriber(Subscriber subscriber) {
		if (scope == null) {
			scope = new WorkspaceScope();
		}
		collector = new SubscriberSyncInfoCollector(subscriber, scope.getRoots());
		
		// listen for global ignore changes
		TeamUI.addPropertyChangeListener(this);
		
		// Start collecting changes
		collector.start();
		
		// Start the refresh now that a subscriber has been added
		SubscriberRefreshSchedule schedule = getRefreshSchedule();
		if(schedule.isEnabled()) {
			getRefreshSchedule().startJob();
		}
	}
	
	/**
	 * Provide a filter that is used to filter the contents of the sync info set for the participant. Normally, all out-of-sync
	 * resources from the subscriber will be included in the participant's set. However, a filter can be used to exclude
	 * some of these out-of-sync resources, if desired.
	 * <p>
	 * Subsclasses can invoke this method any time after <code>setSubscriber</code> has been invoked.
	 * </p>
	 * @param filter a sync info filter
	 */
	protected void setSyncInfoFilter(SyncInfoFilter filter) {
		collector.setFilter(filter);
	}
	
	/*
	 * Create and schedule a subscriber refresh job. 
	 * 
	 * @param resources resources to be synchronized
	 * @param taskName the task name to be shown to the user
	 * @param site the site in which to run the refresh
	 * @param listener the listener to handle the refresh workflow
	 */
	private void internalRefresh(IResource[] resources, String jobName, String taskName, IWorkbenchSite site, IRefreshSubscriberListener listener) {
		if (jobName == null)
		    jobName = getShortTaskName();
		if (taskName == null)
		    taskName = getLongTaskName(resources);
		Platform.getJobManager().cancel(this);
		RefreshSubscriberJob job = new RefreshSubscriberJob(this, jobName, taskName, resources, listener);
		job.setUser(true);
		Utils.schedule(job, site);
		
		// Remember the last participant synchronized
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT, getId());
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID, getSecondaryId());
	}
	
	/**
	 * Return the scope that defines the resources displayed by this participant.
	 * 
	 * @return Returns the scope.
	 */
	public ISynchronizeScope getScope() {
		return scope;
	}
}
