/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.provider;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * Test the cvs watch/edit functionality
 */
public class WatchEditTest extends EclipseTest {

	/**
	 * Constructor for CVSProviderTest
	 */
	public WatchEditTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public WatchEditTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(WatchEditTest.class);
		return new CVSTestSetup(suite);
		//return new CVSTestSetup(new WatchEditTest("testReadOnly"));
	}
	
	private void setReadOnly(boolean b) {
		CVSProviderPlugin.getPlugin().getPluginPreferences().setValue(CVSProviderPlugin.READ_ONLY, b);
	}
	
	/**
	 * Method assertAllFilesReadOnly.
	 * @param copy
	 */
	private void assertAllFilesReadOnly(IContainer folder) throws CoreException {
		folder.accept(new IResourceVisitor() {
			public boolean visit(IResource resource) throws CoreException {
				if (resource.getType() == IResource.FILE) {
					assertTrue(resource.isReadOnly());
				}
				return true;
			}
		});
	}
	
	public void testReadOnlyCheckout() throws CoreException, TeamException {
		// Create a project
		IProject project = createProject("testReadOnlyCheckout", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// XXX project will not be read-only at this point because "cvs add" followed by a "cvs commit" doesn't set the resource "read-only"
		IProject copy = checkoutCopy(project, "copy");
		assertReadOnly(new IResource[] {copy}, true /* isReadOnly */, true /* recurse */);
	}

	public void testEditUnedit() throws CoreException, TeamException, IOException {
		// Create a project
		IProject project = createProject("testEditUnedit", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// XXX project will not be read-only at this point because "cvs add" followed by a "cvs commit" doesn't set the resource "read-only"
		IProject copy = checkoutCopy(project, "copy");
		assertReadOnly(new IResource[] {copy}, true /* isReadOnly */, true /* recurse */);
		editResources(copy, new String[] {"changed.txt", "deleted.txt"});
		setContentsAndEnsureModified(copy.getFile("changed.txt"));
		deleteResources(copy, new String[] {"deleted.txt"}, false);
		uneditResources(copy, new String[] {"changed.txt", "deleted.txt"});
		assertEquals(project, copy);
	}
	
	public void testCommit() throws CoreException, TeamException, IOException {
		// Create a project
		IProject project = createProject("testCommit", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// XXX project will not be read-only at this point because "cvs add" followed by a "cvs commit" doesn't set the resource "read-only"
		IProject copy = checkoutCopy(project, "copy");
		editResources(copy, new String[] {"changed.txt"});
		setContentsAndEnsureModified(copy.getFile("changed.txt"));
		commitProject(copy);
		assertReadOnly(new IResource[] {copy.getFile("changed.txt")}, true /* isReadOnly */, true /* recurse */);
	}
	
	public void testEditMergeUnedit() throws CoreException, TeamException, IOException {
		// Create a project
		IProject project = createProject("testEditMergeUnedit", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// XXX project will not be read-only at this point because "cvs add" followed by a "cvs commit" doesn't set the resource "read-only"
		IProject copy = checkoutCopy(project, "copy");
		IProject copy2 = checkoutCopy(project, "copy2");
		// Modify the second copy and commit changes
		editResources(copy2, new String[] {"changed.txt"});
		setContentsAndEnsureModified(copy2.getFile("changed.txt"));
		commitProject(copy2);
		// Edit first copy, merge then unedit
		editResources(copy, new String[] {"changed.txt"});
		setContentsAndEnsureModified(copy.getFile("changed.txt"));
		updateProject(copy, CVSTag.DEFAULT, false);
		// XXX Update may or may not make the file read-only so it may need to be re-edited
		if (copy.getFile("changed.txt").isReadOnly()) {
			editResources(copy, new String[] {"changed.txt"});
		}
		uneditResources(copy, new String[] {"changed.txt", "deleted.txt"});
		IFile backup = copy.getFile(".#changed.txt.1.1");
		if (backup.exists()) backup.delete(true, false, null);
		assertEquals(project, copy);
	}
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		setReadOnly(true);
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		setReadOnly(false);
	}

}
