package org.eclipse.update.core;

import java.io.File;
import java.net.URL;
import java.util.Date;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
/**
 * Installation configuration object.
 */
public interface IInstallConfiguration {
		
	/**
	 * Returns <code>true</code> is this is the current configuration
	 * 
	 * @return boolean
	 */
	public boolean isCurrent();
	
	/**
	 *  Change the 
	 * 
	 */
	//FIXME : javadoc
	void setCurrent(boolean isCurrent);
	
	
	/**
	 * Returns an array of features configured through this configuration.
	 * 
	 * @return IFeatureReference[] configured features. Returns an empty array
	 * if there are no configured features
	 */
	public IFeatureReference[] getConfiguredFeatures();
	
	/**
	 * Returns an array of unconfigured features  through this configuration.
	 * 
	 * unconfigured Features are accessible by the user but will not be executed.
	 * 
	 * @return IFeatureReference[] unconfigured features. Returns an empty array
	 * if there are no unconfigured features
	 */
	public IFeatureReference[] getUnconfiguredFeatures();
	
	
	/**
	 * Returns an array of local install sites that can be used as 
	 * targets for installing local features. The sites
	 * must be read-write accessible from the current client, otherwise
	 * subsequent installation attampts will fail.
	 * 
	 * @return ISite[] local install sites. Returns an empty array
	 * if there are no local install sites
	 */
	public ISite[] getInstallSites();
	
	/**
	 * Adds an additional local install site to this configuration.
	 * The site must be read-write accessible from the current
	 * client, otherwise subsequent installation
	 * attempts will fail. Note, that this method does not verify
	 * the site is read-write accessible.
	 * 
	 * @param site local install site
	 */
	public void addInstallSite(ISite site);
	
	/**
	 * Removes a local install site from this configuration.
	 * 
	 * @param site local install site
	 */
	public void removeInstallSite(ISite site);
	
	/**
	 * Returns an array of sites (generally read-only) used for accessing
	 * additional features
	 * 
	 * @return ISite[] array of linked sites. Returns an empty array
	 * if there are no linked sites
	 */
	public ISite[] getLinkedSites();
	
	/**
	 * Adds an additional linked site to this configuration
	 * 
	 * @param site linked site
	 */
	public void addLinkedSite(ISite site);
	
	/**
	 * Removes a linked site from this configuration
	 * 
	 * @param site linked site
	 */
	public void removeLinkedSite(ISite site);	
	
	void addInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener);
	void removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener);
	
	/**
	 * Export the configuration to a file
	 */
	void export(File exportFile);
	
	/**
	 * Returns the Activities that were performed to get this InstallConfiguration.
	 * 
	 * There is always at least one Activity
	 * 
	 * 
	 */
	IActivity[] getActivities();
	
	/**
	 * retruns the Date at which the Configuration was created
	 * The date is the local date from the machine that created the Configuration.
	 */
	Date getCreationDate();
	
	
	/**
	 * Configure the Feature to be available at next startup
	 */
	void configure(IFeature feature);
	
	/**
	 * Unconfigure the feature from the execution path
	 */
	void unconfigure(IFeature feature);
	
	/**
	 * returns the URL of where the configuration is declared
	 * The URL points to teh exact XML file
	 */
	URL getURL();
	
	/**
	 * returns the label of the configuration
	 */
	String getLabel();

}

