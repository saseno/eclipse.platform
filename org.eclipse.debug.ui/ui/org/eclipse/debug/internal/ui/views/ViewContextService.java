/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.commands.contexts.ContextManagerEvent;
import org.eclipse.core.commands.contexts.IContextManagerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.internal.ui.contexts.DebugModelContextBindingManager;
import org.eclipse.debug.internal.ui.contexts.provisional.IDebugContextListener;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener4;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Performs view management for a window.
 * 
 * @since 3.2
 */
public class ViewContextService implements IDebugContextListener, IPerspectiveListener4, IPropertyChangeListener, IContextManagerListener {
	
	/**
	 * Maps the perspectives in this window to its last activated workbench context
	 */
	private Map fPerspectiveToActiveContext = new HashMap();
	
	/**
	 * Map of the perspectives to all workbench contexts activated in that perspective 
	 */
	private Map fPerspectiveToActivatedContexts = new HashMap();
	
	/**
	 * Map of context ids to context view ids that have bindings
	 */
	private Map fContextIdsToViewIds;
    
    /**
     * Map of view id to its info
     */
    private Map fViewBindings;
	
	/**
	 * List of perspectives that debugging is allowed in
	 */
	private List fEnabledPerspectives = new ArrayList();	
    
    /**
     * Whether to ignore perspective change callbacks (set to 
     * true when this class is modifying views). 
     */
    private boolean fIgnoreChanges = false;
	
	/**
	 * The window this service is working for
	 */
	private IWorkbenchWindow fWindow;
	
	private IContextService fContextService;
	
	// base debug context
	public static final String DEBUG_CONTEXT= "org.eclipse.debug.ui.debugging"; //$NON-NLS-1$
	
	// extension point
	private static final String ID_CONTEXT_VIEW_BINDINGS= "contextViewBindings"; //$NON-NLS-1$
	
	// extension attributes
	private static final String ATTR_CONTEXT_ID= "contextId"; //$NON-NLS-1$
	private static final String ATTR_VIEW_ID= "viewId"; //$NON-NLS-1$
	private static final String ATTR_AUTO_OPEN= "autoOpen"; //$NON-NLS-1$
	private static final String ATTR_AUTO_CLOSE= "autoClose"; //$NON-NLS-1$	
    
    // XML tags
    private static final String XML_ELEMENT_VIEW_BINDINGS ="viewBindings"; //$NON-NLS-1$
    private static final String XML_ELEMENT_PERSPECTIVE ="perspective"; //$NON-NLS-1$
    private static final String XML_ELEMENT_VIEW = "view"; //$NON-NLS-1$
    private static final String XML_ATTR_ID = "id"; //$NON-NLS-1$
    private static final String XML_ATTR_USER_ACTION = "userAction"; //$NON-NLS-1$
    private static final String XML_VALUE_OPENED = "opened"; //$NON-NLS-1$
    private static final String XML_VALUE_CLOSED = "closed"; //$NON-NLS-1$
    
    /**
     * Information for a view
     */
    private class ViewBinding {
        private IConfigurationElement fElement;
        /**
         * Set of perspectives this view was opened in by the user
         */
        private Set fUserOpened = new HashSet();
        /**
         * Set of perspectives this view was closed in by the user
         */
        private Set fUserClosed = new HashSet();
        
        public ViewBinding(IConfigurationElement element) {
            fElement = element;
        }
        
        /**
         * Returns the id of the view this binding pertains to.
         * 
         * @return
         */
        public String getViewId() {
            return fElement.getAttribute(ATTR_VIEW_ID);
        }
        
        /**
         * Returns whether this view binding is set for auto-open.
         * 
         * @return
         */
        public boolean isAutoOpen() {
            String autoopen = fElement.getAttribute(ATTR_AUTO_OPEN);
            return autoopen == null || "true".equals(autoopen); //$NON-NLS-1$
        }
        
        /**
         * Returns whether this view binding is set for auto-close.
         * 
         * @return
         */
        public boolean isAutoClose() {
            return "true".equals(fElement.getAttribute(ATTR_AUTO_CLOSE)); //$NON-NLS-1$
        }
        
        /**
         * Returns whether this view was opened by the user in the active perspective.
         * @return
         */
        public boolean isUserOpened() {
            return fUserOpened.contains(getActivePerspective().getId());
        }
        
        /**
         * Returns whether this view was closed by the user in the active perspective
         * @return
         */
        public boolean isUserClosed() {
            return fUserClosed.contains(getActivePerspective().getId());
        }
        
        protected void userOpened() {
            if (isTrackingViews()) {
                String id = getActivePerspective().getId();
                fUserOpened.add(id);
                fUserClosed.remove(id);
                saveViewBindings();
            }
        }
        
        protected void userClosed() {
            if (isTrackingViews()) {
                String id = getActivePerspective().getId();
                fUserClosed.add(id);
                fUserOpened.remove(id);
                saveViewBindings();
            }
        }
        
        /**
         * Returns whether the preference is set to track user view open/close.
         * 
         * @return
         */
        protected boolean isTrackingViews() {
            return DebugUITools.getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_TRACK_VIEWS);
        }
        
        /**
         * Context has been activated, open/show as required.
         * 
         * @param page
         */
        public void activated(IWorkbenchPage page) {
            if (!isUserClosed()) {
                if (isAutoOpen()) {
                    try {
                        fIgnoreChanges = true;
                        page.showView(getViewId(), null, IWorkbenchPage.VIEW_CREATE);
                    } catch (PartInitException e) {
                        DebugUIPlugin.log(e);
                    } finally {
                        fIgnoreChanges = false;
                    }
                    // TODO:
                    // bring to top if less relevant view is on top open
                }
            }
        }
        
        /**
         * Context has been deactivated, close as required.
         * 
         * @param page
         */
        public void deactivated(IWorkbenchPage page) {
            if (!isUserOpened()) {
                if (isAutoClose() || !IDebugUIConstants.ID_DEBUG_PERSPECTIVE.equals(getActivePerspective().getId())) {
                    IViewReference reference = page.findViewReference(getViewId());
                    if (reference != null) {
                        try {
                            fIgnoreChanges = true;
                            page.hideView(reference);
                        } finally {
                            fIgnoreChanges = false;
                        }
                    }
                }
            }
        }

        /**
         * Save view binding settings into XML doc.
         * 
         * @param document
         * @param root
         */
        public void saveBindings(Document document, Element root) {
            Element viewElement = document.createElement(XML_ELEMENT_VIEW);
            viewElement.setAttribute(XML_ATTR_ID, getViewId());
            appendPerspectives(document, viewElement, fUserOpened, XML_VALUE_OPENED);
            appendPerspectives(document, viewElement, fUserClosed, XML_VALUE_CLOSED);
            if (viewElement.hasChildNodes()) {
                root.appendChild(viewElement);
            }
        }
        
        private void appendPerspectives(Document document, Element parent, Set perpectives, String xmlValue) {
            String[] ids = (String[]) perpectives.toArray(new String[perpectives.size()]);
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                Element element = document.createElement(XML_ELEMENT_PERSPECTIVE);
                element.setAttribute(XML_ATTR_ID, id);
                element.setAttribute(XML_ATTR_USER_ACTION, xmlValue);
                parent.appendChild(element);
            }
        }
        
        public void applyUserSettings(Element viewElement) {
            NodeList list = viewElement.getChildNodes();
            int length = list.getLength();
            for (int i = 0; i < length; ++i) {
                Node node = list.item(i);
                short type = node.getNodeType();
                if (type == Node.ELEMENT_NODE) {
                    Element entry = (Element) node;
                    if(entry.getNodeName().equalsIgnoreCase(XML_ELEMENT_PERSPECTIVE)){
                        String id = entry.getAttribute(XML_ATTR_ID);
                        String setting = entry.getAttribute(XML_ATTR_USER_ACTION);
                        if (id != null) {
                            if (XML_VALUE_CLOSED.equals(setting)) {
                                fUserClosed.add(id);
                            } else if (XML_VALUE_OPENED.equals(setting)) {
                                fUserOpened.add(id);
                            }
                        }
                    }
                }
            }       
        }
    }
	
	/**
	 * Creates a service for the given window
	 * 
	 * @param window
	 */
	ViewContextService(IWorkbenchWindow window) {
		fWindow = window;
		loadContextToViewExtensions();
        applyUserViewBindings();
		loadPerspectives();
		window.addPerspectiveListener(this);
		DebugContextManager.getDefault().addDebugContextListener(this, window);
		DebugUIPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(this);
		fContextService = (IContextService) PlatformUI.getWorkbench().getAdapter(IContextService.class);
		fContextService.addContextManagerListener(this);
	}
	
	public void dispose() {
		fWindow.removePerspectiveListener(this);
		DebugContextManager.getDefault().removeDebugContextListener(this, fWindow);
		DebugUIPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(this);
		fContextService.removeContextManagerListener(this);
	}
	
	/**
	 * Loads extensions which map context ids to views.
	 */
	private void loadContextToViewExtensions() {
        fContextIdsToViewIds = new HashMap();
        fViewBindings = new HashMap();
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), ID_CONTEXT_VIEW_BINDINGS);
		IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
		for (int i = 0; i < configurationElements.length; i++) {
			IConfigurationElement element = configurationElements[i];
			String viewId = element.getAttribute(ATTR_VIEW_ID);
			String contextId = element.getAttribute(ATTR_CONTEXT_ID);
			if (contextId == null || viewId == null) {
				continue;
			}
            ViewBinding info = new ViewBinding(element);
			List ids= (List) fContextIdsToViewIds.get(contextId);
			if (ids == null) {
				ids= new ArrayList();
				fContextIdsToViewIds.put(contextId, ids);
			}
			ids.add(viewId);
            fViewBindings.put(viewId, info);
		}
	}
    
    /**
     * Applies user settings that modify view binding extensions.
     */
    private void applyUserViewBindings() {
        String xml = DebugUITools.getPreferenceStore().getString(IInternalDebugUIConstants.PREF_USER_VIEW_BINDINGS);
        if (xml.length() > 0) {
            try {
                Element root = DebugPlugin.parseDocument(xml);
                NodeList list = root.getChildNodes();
                int length = list.getLength();
                for (int i = 0; i < length; ++i) {
                    Node node = list.item(i);
                    short type = node.getNodeType();
                    if (type == Node.ELEMENT_NODE) {
                        Element entry = (Element) node;
                        if(entry.getNodeName().equalsIgnoreCase(XML_ELEMENT_VIEW)){
                            String id = entry.getAttribute(XML_ATTR_ID);
                            ViewBinding binding = (ViewBinding) fViewBindings.get(id);
                            if (binding != null) {
                                binding.applyUserSettings(entry);
                            }
                        }
                    }
                }                
            } catch (CoreException e) {
                DebugUIPlugin.log(e);
            }
        }
    }
	
	/**
	 * Load the collection of perspectives in which view management will occur from the preference store.
	 */
	private void loadPerspectives() {
		String prefString = DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES);
		fEnabledPerspectives = parseList(prefString);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
        if (!fIgnoreChanges) {
    		if (IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES.equals(event.getProperty())) {
    			loadPerspectives();
    		} else if (IInternalDebugUIConstants.PREF_USER_VIEW_BINDINGS.equals(event.getProperty())) {
    		    loadContextToViewExtensions();
                applyUserViewBindings();
                // clear activations to re-enable activation based on new settings
                fPerspectiveToActivatedContexts.clear();
                ISelection selection = DebugContextManager.getDefault().getActiveContext(fWindow);
                contextActivated(selection, null);
            }
        }
	}	
	
	/**
	 * Returns whether this service's window's active perspective supports view management.
	 * 
	 * @return whether this service's window's active perspective supports view management
	 */
	private boolean isEnabledPerspective() {
		IPerspectiveDescriptor perspective = getActivePerspective();
		if (perspective != null) {
			return fEnabledPerspectives.contains(perspective.getId());
		}
		return false;
	}	
	
	/**
	 * Returns the active perspective in this service's window, or <code>null</code>
	 * 
	 * @return active perspective or <code>null</code>
	 */
	private IPerspectiveDescriptor getActivePerspective() {
		IWorkbenchPage activePage = fWindow.getActivePage();
		if (activePage != null) {
			return activePage.getPerspective();
		}
		return null;
	}
	
	/**
	 * Parses the comma separated string into a list of strings
	 * 
	 * @return list
	 */
	public static List parseList(String listString) {
		List list = new ArrayList(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			list.add(token);
		}
		return list;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.contexts.IDebugContextListener#contextActivated(org.eclipse.jface.viewers.ISelection, org.eclipse.ui.IWorkbenchPart)
	 */
	public void contextActivated(ISelection selection, IWorkbenchPart part) {
		if (isEnabledPerspective()) {
			if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				Iterator iterator = ss.iterator();
				while (iterator.hasNext()) {
					Object target = iterator.next();
					ILaunch launch = DebugModelContextBindingManager.getLaunch(target);
					if (launch != null) {
						ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
						if (launchConfiguration != null) {
							try {
								ILaunchConfigurationType type = launchConfiguration.getType();
								// check if this perspective is enabled for the launch type
								if (fContextService.getActiveContextIds().contains(type.getIdentifier() + "." + getActivePerspective().getId())) { //$NON-NLS-1$
									List workbenchContexts = DebugModelContextBindingManager.getDefault().getWorkbenchContextsForDebugContext(target);
									// TODO: do we need to check if contexts are actually enabled in workbench first?
									if (!workbenchContexts.isEmpty()) {
										// if all contexts already activate and last context is already active context == done
										Iterator contexts = workbenchContexts.iterator();
										while (contexts.hasNext()) {
											String contextId = (String)contexts.next();
											if (!isActivated(contextId)) {
												activateChain(contextId);
											}
											// ensure last context gets top priority
											if (!contexts.hasNext()) {
												if (!isActiveContext(contextId)) {
													activate(contextId);
												}
											}
										}
									}
									
								}
							} catch (CoreException e) {
								DebugUIPlugin.log(e);
							}
						}												
					}						
				}
			}
		}
	}
	
	/**
	 * Returns whether the given context is the active context in the active perspective.
	 * 
	 * @param contextId
	 * @return
	 */
	private boolean isActiveContext(String contextId) {
		IPerspectiveDescriptor activePerspective = getActivePerspective();
		if (activePerspective != null) {
			String activeId = (String) fPerspectiveToActiveContext.get(activePerspective);
			return contextId.equals(activeId);
		}
		return false;
	}
	
	/**
	 * Returns whether the given context is activated in the active perspective. 
	 * 
	 * @param contextId
	 * @return
	 */
	private boolean isActivated(String contextId) {
		IPerspectiveDescriptor activePerspective = getActivePerspective();
		if (activePerspective != null) {
			Set contexts = (Set) fPerspectiveToActivatedContexts.get(activePerspective); 
			if (contexts != null) {
				return contexts.contains(contextId);
			}
		}
		return false;
	}
	
	private void addActivated(String contextId) {
		IPerspectiveDescriptor activePerspective = getActivePerspective();
		if (activePerspective != null) {
			Set contexts = (Set) fPerspectiveToActivatedContexts.get(activePerspective); 
			if (contexts == null) {
				contexts = new HashSet();
				fPerspectiveToActivatedContexts.put(activePerspective, contexts);
			}
			contexts.add(contextId);
		}
	}
	
	private void removeActivated(String contextId) {
		IPerspectiveDescriptor activePerspective = getActivePerspective();
		if (activePerspective != null) {
			Set contexts = (Set) fPerspectiveToActivatedContexts.get(activePerspective); 
			if (contexts != null) {
				contexts.remove(contextId);
			}
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.contexts.IDebugContextListener#contextChanged(org.eclipse.jface.viewers.ISelection, org.eclipse.ui.IWorkbenchPart)
	 */
	public void contextChanged(ISelection selection, IWorkbenchPart part) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener3#perspectiveOpened(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener3#perspectiveClosed(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectiveClosed(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener3#perspectiveDeactivated(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
	}
	
	/**
	 * Closes all auto-opened views.
	 * 
	 * @param page
	 * @param perspective
	 */
	private void clean(IPerspectiveDescriptor perspective) {
		Set contexts = (Set) fPerspectiveToActivatedContexts.remove(perspective);
		fPerspectiveToActiveContext.remove(perspective);
		if (contexts != null) {
			Iterator iterator = contexts.iterator();
			while (iterator.hasNext()) {
				String id = (String) iterator.next();
				deactivate(id);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener3#perspectiveSavedAs(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor oldPerspective, IPerspectiveDescriptor newPerspective) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener2#perspectiveChanged(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor, org.eclipse.ui.IWorkbenchPartReference, java.lang.String)
	 */
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
		if (!fIgnoreChanges && page.getWorkbenchWindow().equals(fWindow)) {
			if(partRef != null) {
	            ViewBinding info = (ViewBinding) fViewBindings.get(partRef.getId());
	            if (info != null) {
	                if (IWorkbenchPage.CHANGE_VIEW_SHOW == changeId) {
	                    info.userOpened();
	                } else if (IWorkbenchPage.CHANGE_VIEW_HIDE == changeId) {
	                    info.userClosed();
	                }
	            }
			}
        }	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener#perspectiveActivated(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		if (page.getWorkbenchWindow().equals(fWindow)) {
			ISelection activeContext = DebugContextManager.getDefault().getActiveContext(fWindow);
			if (activeContext != null) {
				contextActivated(activeContext, null);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener#perspectiveChanged(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor, java.lang.String)
	 */
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
	}
	
	/**
	 * Activates all parent contexts of the given context, top down.
	 * 
	 * @param contextId
	 */
	private void activateChain(String contextId) {
		String[] contextChain = getContextChain(contextId);
		if (contextChain != null) {
			for (int i = 0; i < contextChain.length; i++) {
				activate(contextChain[i]);
			}
		}
	}
	
	/**
	 * Returns the debug context for the given leaf context, top down.
	 * 
	 * @param contextId
	 * @return context chain or <code>null</code>
	 */
	private String[] getContextChain(String contextId) {
		List chain = new ArrayList();
		Context context = null;
		do {
			if (context != null) {
				try {
					contextId = context.getParentId();
				} catch (NotDefinedException e) {
					DebugUIPlugin.log(e);
					return null;
				}
			}
			context = fContextService.getContext(contextId);
			chain.add(0, contextId);
		} while (!contextId.equals(DEBUG_CONTEXT));
		return (String[]) chain.toArray(new String[chain.size()]);
	}
	
	/**
	 * Activates the given context in the active perspective.
	 * 
	 * @param contextId
	 */
	private void activate(String contextId) {
		IWorkbenchPage page = fWindow.getActivePage();
		if (page != null) {
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (perspective != null) {
				addActivated(contextId);
				setActive(perspective, contextId);
				List viewIds = (List) fContextIdsToViewIds.get(contextId);
				if (viewIds != null) {
					Iterator iterator = viewIds.iterator();
					while (iterator.hasNext()) {
						String id = (String) iterator.next();
                        ViewBinding info = (ViewBinding) fViewBindings.get(id);
                        info.activated(page);
					}
				}
			}
		}
	}
	
	/**
	 * Sets the active context in the given perspective, or removes
	 * when <code>null</code>.
	 * 
	 * @param perspective
	 * @param contextId
	 */
	private void setActive(IPerspectiveDescriptor perspective, String contextId) {
		if (contextId == null) {
			fPerspectiveToActiveContext.remove(perspective);
		} else {
			fPerspectiveToActiveContext.put(perspective, contextId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.contexts.IContextManagerListener#contextManagerChanged(org.eclipse.core.commands.contexts.ContextManagerEvent)
	 */
	public void contextManagerChanged(ContextManagerEvent event) {
		if (event.isActiveContextsChanged()) {
			Set disabledContexts = getDisabledContexts(event);
			if (!disabledContexts.isEmpty()) {
				Iterator contexts = disabledContexts.iterator();
				while (contexts.hasNext()) {
					String contextId = (String)contexts.next();
					if (isViewConetxt(contextId)) {
						if (isActivated(contextId)) {
							deactivate(contextId);
						}
					}
				}
			}
		}
	}
	
	private void deactivate(String contextId) {
		IWorkbenchPage page = fWindow.getActivePage();
		if (page != null) {
			IPerspectiveDescriptor perspective = page.getPerspective();
			if (perspective != null) {
				removeActivated(contextId);
				if (isActiveContext(contextId)) {
					setActive(perspective, null);
				}
                List viewIds = (List) fContextIdsToViewIds.get(contextId);
                if (viewIds != null) {
                    Iterator iterator = viewIds.iterator();
                    while (iterator.hasNext()) {
                        String id = (String) iterator.next();
                        ViewBinding info = (ViewBinding) fViewBindings.get(id);
                        info.deactivated(page);
                    }
                }
			}
		}		
	}
	
	/**
	 * Returns a set of contexts disabled in the given event, possibly empty.
	 * 
	 * @param event
	 * @return disabled context ids
	 */
	private Set getDisabledContexts(ContextManagerEvent event) {
		Set prev = new HashSet(event.getPreviouslyActiveContextIds());
		Set activeContextIds = event.getContextManager().getActiveContextIds();
		if (activeContextIds != null) {
			prev.removeAll(activeContextIds);
		}
		return prev;
	}	

	/**
	 * Returns whether the given context has view bindings.
	 * 
	 * @param id
	 * @return whether the given context has view bindings
	 */
	private boolean isViewConetxt(String id) {
		return fContextIdsToViewIds.containsKey(id);
	}
    
    /**
     * Save view binding settings that differ from extension settings
     */
    private void saveViewBindings() {
        try {
            Document document = DebugPlugin.newDocument();
            Element root = document.createElement(XML_ELEMENT_VIEW_BINDINGS);
            document.appendChild(root);
            Iterator bindings = fViewBindings.values().iterator();
            while (bindings.hasNext()) {
                ViewBinding binding = (ViewBinding) bindings.next();
                binding.saveBindings(document, root);
            }
            String prefValue = ""; //$NON-NLS-1$
            if (root.hasChildNodes()) {
            	prefValue = DebugPlugin.serializeDocument(document);
            }
            fIgnoreChanges = true;
            DebugUITools.getPreferenceStore().setValue(IInternalDebugUIConstants.PREF_USER_VIEW_BINDINGS, prefValue);
        } catch (CoreException e) {
            DebugUIPlugin.log(e);
        } finally {
            fIgnoreChanges = false;
        }

    }    
    
    /**
     * Returns the perspectives in which debugging is enabled.
     * 
     * @return
     */
    public String[] getEnabledPerspectives() {
    	return (String[]) fEnabledPerspectives.toArray(new String[fEnabledPerspectives.size()]);
    }
    
    /**
     * Show the view without effecting user preferences
     * 
     * @param viewId
     */
    public void showViewQuiet(String viewId) {
		IWorkbenchPage page = fWindow.getActivePage();
		if (page != null) {
			try {
				fIgnoreChanges = true;
				page.showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				DebugUIPlugin.log(e);
			} finally {
				fIgnoreChanges = false;
			}
		}    	
    }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveListener4#perspectivePreDeactivate(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
	 */
	public void perspectivePreDeactivate(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		if (page.getWorkbenchWindow().equals(fWindow)) {
			clean(perspective);
		}
	}
}
