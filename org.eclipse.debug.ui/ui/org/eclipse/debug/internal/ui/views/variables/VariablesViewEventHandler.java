package org.eclipse.debug.internal.ui.views.variables;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.views.AbstractDebugEventHandler;
import org.eclipse.debug.ui.AbstractDebugView;

/**
 * Updates the variables view
 */
public class VariablesViewEventHandler extends AbstractDebugEventHandler {
	
	/**
	 * Constructs a new event handler on the given view
	 * 
	 * @param view variables view
	 */
	public VariablesViewEventHandler(AbstractDebugView view) {
		super(view);
	}
	
	/**
	 * @see AbstractDebugEventHandler#handleDebugEvents(DebugEvent[])
	 */
	protected void doHandleDebugEvents(DebugEvent[] events) {	
		for (int i = 0; i < events.length; i++) {	
			DebugEvent event = events[i];
			switch (event.getKind()) {
				case DebugEvent.SUSPEND:
					doHandleSuspendEvent(event);
					break;
				case DebugEvent.CHANGE:
					doHandleChangeEvent(event);
					break;
				case DebugEvent.RESUME:
					doHandleResumeEvent(event);
					break;
				case DebugEvent.TERMINATE:
					doHandleTerminateEvent(event);
					break;
			}
		}
	}

	/**
	 * Clear the variables immediately upon resume.
	 */
	protected void doHandleResumeEvent(DebugEvent event) {
		if (!event.isStepStart() && !event.isEvaluation()) {
			// Clear existing variables from the view
			getViewer().setInput(null);
			// clear the cache of expanded variables for the resumed thread/target
			// getVariablesView().clearExpandedVariables(event.getSource());
		}
	}

	/**
	 * Clear any cached variable expansion state for the
	 * terminated thread/target.
	 */
	protected void doHandleTerminateEvent(DebugEvent event) {
		getVariablesView().clearExpandedVariables(event.getSource());
	}
	
	/**
	 * Process a SUSPEND event
	 */
	protected void doHandleSuspendEvent(DebugEvent event) {
		if (event.getDetail() != DebugEvent.EVALUATION_IMPLICIT) {
			if (event.getSource() instanceof ISuspendResume) {
				if (!((ISuspendResume)event.getSource()).isSuspended()) {
					// no longer suspended
					return;
				}
			}						
			// Don't refresh everytime an implicit evaluation finishes
			refresh();
			if (event.getDetail() == DebugEvent.STEP_END) {
				getVariablesView().populateDetailPane();
			}
		}		
	}
	
	/**
	 * Process a CHANGE event
	 */
	protected void doHandleChangeEvent(DebugEvent event) {
		if (event.getDetail() == DebugEvent.STATE) {
			// only process variable state changes
			if (event.getSource() instanceof IVariable) {
				refresh(event.getSource());
			}
		} else {
			refresh();
		}	
	}	

	protected VariablesView getVariablesView() {
		return (VariablesView)getView();
	}
}

