/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ResourceLocator;
import org.osgi.framework.Bundle;

public class PluginIndex {
	private String pluginId;

	/**
	 * index path as defined in plugin.xml, e.g. "index"
	 */
	private String path;

	private SearchIndex targetIndex;

	/**
	 * path prefixes where index is found e.g. "", "nl/en/US", "ws/gtk"
	 */
	private List indexIDs;

	/**
	 * resolved directory paths (Strings) corresponding to indexes at given
	 * prefixes, e.g. //d/eclipse/...../os/linux/index,
	 */
	private List resolvedPaths;

	public PluginIndex(String pluginId, String path, SearchIndex targetIndex) {
		super();
		this.pluginId = pluginId;
		this.path = path;
		this.targetIndex = targetIndex;
	}

	private void resolve() {
		if (indexIDs != null) {
			// resolved
			return;
		}
		indexIDs = new ArrayList();
		resolvedPaths = new ArrayList();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle == null) {
			return;
		}
		boolean found = false;
		ArrayList availablePrefixes = ResourceLocator.getPathPrefix(targetIndex
				.getLocale());
		for (int i = 0; i < availablePrefixes.size(); i++) {
			String prefix = (String) availablePrefixes.get(i);
			IPath prefixedPath = new Path(prefix + path);
			// find index at this directory in plugin or fragments
			URL url = Platform.find(bundle, prefixedPath);
			if (url == null) {
				continue;
			}
			found = true;
			if (!isCompatible(bundle, prefixedPath)) {
				continue;
			}
			URL resolved;
			try {
				resolved = Platform.resolve(url);
			} catch (IOException ioe) {
				HelpBasePlugin.logError("Help index directory at " //$NON-NLS-1$
						+ prefixedPath + " for plugin " //$NON-NLS-1$
						+ bundle.getSymbolicName() + " cannot be resolved.", //$NON-NLS-1$
						ioe);
				continue;
			}
			if ("file".equals(resolved.getProtocol())) { //$NON-NLS-1$
				indexIDs.add(getIndexId(prefix));
				resolvedPaths.add(resolved.getFile());
			} else {
				try {
					// extract index from jarred bundles
					URL localURL = Platform.asLocalURL(url);
					if ("file".equals(localURL.getProtocol())) { //$NON-NLS-1$
						indexIDs.add(getIndexId(prefix));
						resolvedPaths.add(localURL.getFile());
					}
				} catch (IOException ioe) {
					HelpBasePlugin.logError(
							"Help index directory at " + prefixedPath //$NON-NLS-1$
									+ " for plugin " + bundle.getSymbolicName() //$NON-NLS-1$
									+ " cannot be resolved.", ioe); //$NON-NLS-1$
					continue;
				}
			}
		}
		if (!found) {
			HelpBasePlugin.logError(
					"Help index declared, but missing for plugin " //$NON-NLS-1$
							+ getPluginId() + ".", null); //$NON-NLS-1$

		}
	}

	private boolean isCompatible(Bundle bundle, IPath prefixedPath) {
		URL url = Platform.find(bundle, prefixedPath
				.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME));
		if (url == null) {
			HelpBasePlugin.logError(prefixedPath
					.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME)
					+ " file missing from help index \"" //$NON-NLS-1$
					+ path + "\" of plugin " + getPluginId(), null); //$NON-NLS-1$

			return false;
		}
		InputStream in = null;
		try {
			in = url.openStream();
			Properties prop = new Properties();
			prop.load(in);
			String lucene = prop
					.getProperty(SearchIndex.DEPENDENCIES_KEY_LUCENE);
			String analyzer = prop
					.getProperty(SearchIndex.DEPENDENCIES_KEY_ANALYZER);
			if (!targetIndex.isLuceneCompatible(lucene)
					|| !targetIndex.isAnalyzerCompatible(analyzer)) {
				if (HelpBasePlugin.DEBUG_SEARCH) {
					System.out
							.println("Ignoring prebuilt help search index for plugin " //$NON-NLS-1$
									+ getPluginId()
									+ ".  Index is not compatible with index in use."); //$NON-NLS-1$
				}
				return false;
			}
		} catch (MalformedURLException mue) {
			return false;
		} catch (IOException ioe) {
			HelpBasePlugin.logError(
					"IOException accessing prebuilt index.", ioe); //$NON-NLS-1$
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return true;
	}

	/**
	 * Creates id of prebuilt index
	 * 
	 * @param prefix
	 *            index directory prefix, e.g. "", "ws/gtk"
	 * @return indexId string, e.g. "/", "/ws/gtk"
	 */
	private String getIndexId(String prefix) {
		if (prefix.length() == 0) {
			// root
			return "/"; //$NON-NLS-1$
		}
		return "/" + prefix.substring(0, prefix.length() - 1); //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		return pluginId.equals(obj);
	}

	public int hashCode() {
		return pluginId.hashCode();
	}

	public String toString() {
		StringBuffer ret = new StringBuffer(pluginId);
		ret.append(":"); //$NON-NLS-1$
		ret.append(path);
		ret.append("="); //$NON-NLS-1$
		if (indexIDs == null) {
			ret.append("unresolved"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < indexIDs.size(); i++) {
				ret.append(indexIDs.get(i));
				ret.append("@"); //$NON-NLS-1$
				ret.append(resolvedPaths.get(i));
			}
		}
		return ret.toString();
	}

	public List getIDs() {
		resolve();
		return indexIDs;
	}

	/**
	 * @return list of paths (string) to an index directory. Paths are ordered
	 *         from
	 */
	public List getPaths() {
		resolve();
		return resolvedPaths;
	}

	public String getPluginId() {
		return pluginId;
	}

}
