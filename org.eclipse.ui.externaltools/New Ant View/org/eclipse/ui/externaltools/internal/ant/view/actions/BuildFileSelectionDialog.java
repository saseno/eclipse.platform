package org.eclipse.ui.externaltools.internal.ant.view.actions;

import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.externaltools.internal.ui.TreeAndListGroup;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Dialog for selecting an XML file in the workspace. Derived from org.eclipse.
 * ui. dialogs.
 */
public class BuildFileSelectionDialog extends MessageDialog {
	// the root element to populate the viewer with
	private IAdaptable					root;

	// the visual selection widget group
	private TreeAndListGroup	selectionGroup;
	// constants
	private final static int			SIZING_SELECTION_WIDGET_WIDTH = 400;
	private final static int			SIZING_SELECTION_WIDGET_HEIGHT = 300;
	/**
	 * The file selected by the user.
	 */
	private IFile result= null;
/**
 * Creates a resource selection dialog rooted at the given element.
 *
 * @param parentShell the parent shell
 * @param rootElement the root element to populate this dialog with
 * @param message the message to be displayed at the top of this dialog, or
 *    <code>null</code> to display a default message
 */
public BuildFileSelectionDialog(Shell parentShell, IAdaptable rootElement, String message) {
	super(parentShell, "Resource selection", null, message, MessageDialog.NONE, new String[] {"Ok", "Cancel"}, 0);
	root = rootElement;
	setShellStyle(getShellStyle() | SWT.RESIZE);
}
/* (non-Javadoc)
 * Method declared in Window.
 */
protected void configureShell(Shell shell) {
	super.configureShell(shell);
	//WorkbenchHelp.setHelp(shell, IHelpContextIds.RESOURCE_SELECTION_DIALOG);
}

protected void createButtonsForButtonBar(Composite parent) {
	super.createButtonsForButtonBar(parent);
	initializeDialog();
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected Control createDialogArea(Composite parent) {
	// page group
	Composite composite = (Composite) super.createDialogArea(parent);

	//create the input element, which has the root resource
	//as its only child

	createMessageArea(composite);
	selectionGroup =
		new TreeAndListGroup(
			composite,
			root,
			getResourceProvider(IResource.FOLDER | IResource.PROJECT | IResource.ROOT),
			new WorkbenchLabelProvider(),
			getResourceProvider(IResource.FILE),
			new WorkbenchLabelProvider(),
			SWT.NONE,
			// since this page has no other significantly-sized
			// widgets we need to hardcode the combined widget's
			// size, otherwise it will open too small
			SIZING_SELECTION_WIDGET_WIDTH,
			SIZING_SELECTION_WIDGET_HEIGHT);

	composite.addControlListener(new ControlListener() {
		public void controlMoved(ControlEvent e) {};
		public void controlResized(ControlEvent e) {
			//Also try and reset the size of the columns as appropriate
			TableColumn[] columns = selectionGroup.getListTable().getColumns();
			for (int i = 0; i < columns.length; i++) {
				columns[i].pack();
			}
		}
	});

	return composite;
}
/**
 * Returns a content provider for <code>IResource</code>s that returns
 * only children of the given resource type.
 */
private ITreeContentProvider getResourceProvider(final int resourceType) {
	return new WorkbenchContentProvider() {
		public Object[] getChildren(Object o) {
			if (o instanceof IContainer) {
				IResource[] members = null;
				try {
					members = ((IContainer)o).members();
				} catch (CoreException e) {
					//just return an empty set of children
					return new Object[0];
				}

				//filter out the desired resource types
				ArrayList results = new ArrayList();
				for (int i = 0; i < members.length; i++) {
					//And the test bits with the resource types to see if they are what we want
					if ((members[i].getType() & resourceType) > 0) {
						if (members[i].getType() == IResource.FILE && !"xml".equalsIgnoreCase(members[i].getFileExtension())) {
							continue;
						}
						results.add(members[i]);
					}
				}
				return results.toArray();
			} else {
				return new Object[0];
			}
		}
	};
}
/**
 * Initializes this dialog's controls.
 */
private void initializeDialog() {
	selectionGroup.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			getOkButton().setEnabled(!selectionGroup.getListTableSelection().isEmpty());
		}
	});

	getOkButton().setEnabled(false);
}

/**
 * Returns this dialog's OK button.
 */
protected Button getOkButton() {
	return getButton(0);
}
/**
 * Returns the file the user chose or <code>null</code> if none.
 */
public IFile getResult() {
	return result;
}

protected void buttonPressed(int buttonId) {
	if (buttonId == 0) {
		Object resource= selectionGroup.getListTableSelection().getFirstElement();
		if (resource instanceof IFile) {
			result = (IFile) resource;
		}
	}
	super.buttonPressed(buttonId);
}

}
