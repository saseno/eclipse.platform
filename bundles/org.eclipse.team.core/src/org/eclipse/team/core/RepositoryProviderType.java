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
package org.eclipse.team.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.core.*;

/**
 * This class represents provisional API. A provider is not required to implement this API.
 * Implementers, and those who reference it, do so with the awareness that this class may be
 * removed or substantially changed at future times without warning.
 * <p>
 * This class represents things you can ask/do with a type of provider. This
 * is in the absence of a project, as opposed to RepositoryProvider which
 * requires a concrete project in order to be instantiated.
 * <p>
 * A repository provider type class is asscoaited with it's provider ID along with it's 
 * corresponding repository provider class. To add a
 * repository provider type and have it registered with the platform, a client
 * must minimally:
 * <ol>
 * 	<li>extend <code>RepositoryProviderType</code>
 * 	<li>add the typeClass field to the repository extension in <code>plugin.xml</code>. 
 *     Here is an example extension point definition:
 * 
 *  <code>
 *	<br>&lt;extension point="org.eclipse.team.core.repository"&gt;
 *  <br>&nbsp;&lt;repository
 *  <br>&nbsp;&nbsp;class="org.eclipse.myprovider.MyRepositoryProvider"
 *  <br>&nbsp;&nbsp;typeClass="org.eclipse.myprovider.MyRepositoryProviderType"
 *  <br>&nbsp;&nbsp;id="org.eclipse.myprovider.myProviderID"&gt;
 *  <br>&nbsp;&lt;/repository&gt;
 *	<br>&lt;/extension&gt;
 *  </code>
 * </ol></p>
 * 
 * <p>
 * Once a repository provider type is registered with Team, then you
 * can access the singleton instance of the class by invoking <code>RepositoryProviderType.getProviderType()</code>.
 * </p>
 * 
 * @see RepositoryProviderType#getProviderType(String)
 * 
 * @since 2.1
 */

public abstract class RepositoryProviderType {
	private static Map allProviderTypes = new HashMap();
	
	private String id;

	public RepositoryProviderType() {
	}

	/**
	 * Return the RepositoryProviderType for the given provider ID.
	 * 
	 * @param id the ID of the provider
	 * @return RepositoryProviderType
	 * 
	 * @see #getID()
	 */
	public static RepositoryProviderType getProviderType(String id) {
		RepositoryProviderType type = (RepositoryProviderType) allProviderTypes.get(id);

		if(type != null)
			return type;
			
		//If there isn't one in the table, we'll try to create one from the extension point
		//Its possible that newProviderType() will return null, but in that case it will have also logged the error	so just return the result
		return newProviderType(id);
	}
	
	private void setID(String id) {
		this.id = id;
	}
	
	private static RepositoryProviderType newProviderType(String id) {
		IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.REPOSITORY_EXTENSION);
		if (extension != null) {
			IExtension[] extensions =  extension.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
				for (int j = 0; j < configElements.length; j++) {
					String extensionId = configElements[j].getAttribute("id"); //$NON-NLS-1$
					
					if (extensionId != null && extensionId.equals(id)) {
						try {
							RepositoryProviderType providerType;
							//Its ok not to have a typeClass extension.  In this case, a default instance will be created.
							if(configElements[j].getAttribute("typeClass") == null) { //$NON-NLS-1$
								providerType = new DefaultRepositoryProviderType();
							} else {
								providerType = (RepositoryProviderType) configElements[j].createExecutableExtension("typeClass"); //$NON-NLS-1$
							}
							
							providerType.setID(id);
							allProviderTypes.put(id, providerType);
							return providerType;
						} catch (CoreException e) {
							TeamPlugin.log(e);
						} catch (ClassCastException e) {
							String className = configElements[j].getAttribute("typeClass"); //$NON-NLS-1$
							TeamPlugin.log(IStatus.ERROR, Policy.bind("RepositoryProviderType.invalidClass", id, className), e); //$NON-NLS-1$
						}
						return null;
					}
				}
			}
		}		
		return null;
	}	
	
	/**
	 * Answer the id of this provider type. The id will be the repository
	 * provider type's id as defined in the provider plugin's plugin.xml.
	 * 
	 * @return the id of this provider type
	 */
	public final String getID() {
		return this.id;
	}

	/**
	 * Answers an object for serializing and deserializing
	 * of references to projects.  Given a project, it can produce a
	 * UTF-8 encoded String which can be stored in a file.
	 * Given this String, it can load a project into the workspace.
	 * It also provides a mechanism
	 * by which repository providers can be notified when a project set is created and exported.
	 * If the provider doesn't wish to provide this
	 * feature, return null.
	 * <p>
	 * Subclasses should override this method to return the appropriate
	 * serializer for the associated repository type.
	 * It is recommended that serializers not have any references to UI classes
	 * so that they can be used in a headless environment.
	 * <p>
	 * At this time, the default implementation wrappers the <code>IProjectSetSerializer</code>
	 * interface if one exists, providing backward compatibility with existing code.
	 * At some time in the future, the <code>IProjectSetSerializer</code> interface will be removed
	 * and the default implementation will revert to having limited functionality.
	 * 
	 * @return the project set serializer (or <code>null</code>)
	 */
	public ProjectSetCapability getProjectSetCapability() {
		// Provide backward compatibility with the old IProjectSetSerializer interface
		IProjectSetSerializer oldSerializer = Team.getProjectSetSerializer(getID());
		if (oldSerializer != null) {
			ProjectSetCapability capability = new DefaultProjectSetCapability();
			capability.setSerializer(oldSerializer);
			return capability;
		}
		return null;
	}
	
	/**
	 * Callback from team when the meta-files for a repository type are detected in an
	 * unshared project. The meta-file paths ae provided as part of the <code>repository</code>
	 * entry in the plugin manifets file.
	 * <p>
	 * By default, nothing is done (except that the repository type's
	 * plugin will have been loaded. Subclass may wish to mark the met-data as team-private.
	 * This method is called from a resource delta so sublcasses may not obtain scheduling rules
	 * or in any way modify workspace resources (including auto-sharing the project). However,
	 * auto-sharing (or other modification) could be performed by a background job scheduled from
	 * this callback.
	 * @param project the project that contains the detected meta-files.
	 * @param containers the folders (possibly including the project folder) in which meta-files were found
	 */
	public void metaFilesDetected(IProject project, IContainer[] containers) {
		// Do nothing by default
	}
}
