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

package org.eclipse.team.internal.ccvs.core.filehistory;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.*;
import org.eclipse.team.core.history.provider.FileHistoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.LogEntry;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSFileStore;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

public class CVSFileHistoryProvider extends FileHistoryProvider {

	/**
	 * see <code>org.eclipse.team.core.IFileHistoryProvider</code>
	 */
	public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor) {
		ICVSRemoteResource remoteResource;
		try {
			monitor.beginTask(null, 100);
			if ((flags == IFileHistoryProvider.SINGLE_REVISION) || ((flags == IFileHistoryProvider.SINGLE_LINE_OF_DESCENT))) {
				remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(resource);
				monitor.worked(40);
				CVSFileHistory remoteFile = null;
				if (remoteResource instanceof ICVSFile) {
					remoteFile = new CVSFileHistory((ICVSFile) remoteResource, flags);
				}
				return remoteFile;
			} else {
				// TODO need to complete the revision
				remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(resource);
				monitor.worked(40);
				CVSFileHistory remoteFile = null;
				if (remoteResource instanceof ICVSFile) {
					remoteFile = new CVSFileHistory((ICVSFile) remoteResource);
				}
				return remoteFile;
			}
		} catch (CVSException e) {
		} finally {
			monitor.done();
		}

		return null;
	}

	public IFileRevision getWorkspaceFileRevision(IResource resource) {
		
		ICVSRemoteResource remoteResource;
		try {
			remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(resource);
			if (remoteResource != null && 
				remoteResource instanceof RemoteFile){
				ResourceSyncInfo syncInfo = remoteResource.getSyncInfo();
				LogEntry cvsEntry = new LogEntry((RemoteFile) remoteResource, syncInfo.getRevision(), "", null,"","", new CVSTag[0]);  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return new CVSFileRevision(cvsEntry);
			}
		} catch (CVSException e) {
		}
		
		return null;
	}

	public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor) {
		if (store instanceof CVSFileStore) {
			
			CVSFileStore fileStore = (CVSFileStore) store;
			ICVSRemoteFile file = fileStore.getCVSURI().toFile();
			if (file != null){
				if ((flags == IFileHistoryProvider.SINGLE_REVISION) || ((flags == IFileHistoryProvider.SINGLE_LINE_OF_DESCENT))) {
					return new CVSFileHistory(file, flags);
				} else
					return new CVSFileHistory(file);
			}
		}
		return null;
	}

}
