/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.team.internal.ui.synchronize.StructuredViewerAdvisor;
import org.eclipse.ui.IViewPart;

/**
 * A view that displays synchronization participants that are registered with the
 * synchronize manager. This is essentially a generic container that allows
 * multiple {@link ISynchronizeParticipant} implementations to share the same
 * view. The only behavior provided by the view is a mechanism for switching 
 * between participants.
 * <p> 
 * Clients should not add viewActions to this view because they will be global
 * to all participants. Instead, add participant specific actions as described
 * in {@link StructuredViewerAdvisor}.
 * </p>
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * @see ISynchronizeManager#showSynchronizeViewInActivePage()
 * @since 3.0
 */
public interface ISynchronizeView extends IViewPart {
	/**
	 * The id for this view
	 */
	public static final String VIEW_ID = "org.eclipse.team.sync.views.SynchronizeView"; //$NON-NLS-1$
	
	/**
	 * This id is no longer used.
	 * @deprecated not used, please use {@link #VIEW_ID} instead.
	 */
	public static final String COMPARE_VIEW_ID = "org.eclipse.team.sync.views.CompareView"; //$NON-NLS-1$
	
	/**
	 * Displays the given synchronize participant in the Synchronize View. This
	 * has no effect if this participant is already being displayed.
	 * 
	 * @param participant participant to be displayed, cannot be <code>null</code>
	 */
	public void display(ISynchronizeParticipant participant);
	
	/**
	 * Returns the participant currently being displayed in the Synchronize View
	 * or <code>null</code> if none.
	 *  
	 * @return the participant currently being displayed in the Synchronize View
	 * or <code>null</code> if none
	 */
	public ISynchronizeParticipant getParticipant();
}
