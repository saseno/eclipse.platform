package org.eclipse.debug.internal.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;

public class RemoveExpressionAction extends AbstractRemoveAction {

	public RemoveExpressionAction(ISelectionProvider provider) {
		super(provider, "Remove", "Remove Selected Expressions");
	}

	/**
	 * @see IAction
	 */
	public void run() {
		IStructuredSelection selection= getStructuredSelection();
		IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
		Iterator itr= selection.iterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			IExpression exp = getExpression(element);
			if (exp != null) {
				manager.removeExpression(exp);
			}
		}
	}
	
	/**
	 * Returns the expression associated with the given
	 * element.
	 * 
	 * @param element an expression of child of an expression in
	 *  the expression view.
	 * @return associated expression
	 */
	protected IExpression getExpression(Object obj) {		
		ISelectionProvider sp = getSelectionProvider();
		if (sp instanceof TreeViewer) {
			ITreeContentProvider cp = (ITreeContentProvider)((TreeViewer)sp).getContentProvider();
			while (!(obj instanceof IExpression) && obj != null) {
				obj = cp.getParent(obj);
			}
			return (IExpression)obj;
		}
		return null;
	}

}

