/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSActionDelegateWrapper;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * Adviser used to add toolbar buttons to the last page of the sharing wizard.
 */
public class SharingWizardPageActionGroup extends SynchronizePageActionGroup {
	
	public static final String ACTION_GROUP = "cvs_sharing_page_actions"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SynchronizeViewerAdvisor#initializeActions(org.eclipse.jface.viewers.StructuredViewer)
	 */
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		configuration.addMenuGroup(ISynchronizePageConfiguration.P_TOOLBAR_MENU, ACTION_GROUP);
		
		appendToGroup(
				ISynchronizePageConfiguration.P_TOOLBAR_MENU, 
				ACTION_GROUP,
				new CVSActionDelegateWrapper(new IgnoreAction(), configuration, null /* no id to avoid conflict with context menu (bug 198319)*/){ 
					protected String getBundleKeyPrefix() {
						return "SharingWizardIgnore."; //$NON-NLS-1$
					}
				});
	}
}
