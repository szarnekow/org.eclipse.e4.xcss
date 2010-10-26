/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.theme.xcss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.core.util.resources.IResourceLocator;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.xtext.common.types.access.impl.ClasspathTypeProvider;
import org.eclipse.xtext.example.css.rendering.XcssRendererHelper;
import org.eclipse.xtext.example.css.xcss.StyleSheet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.css.CSSStyleDeclaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Adapted from the default implementation.
 * @see org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine
 * @author Sebastian Zarnekow
 */
public class XcssPollingThemeEngine implements IThemeEngine {
	
	private static final Logger logger = Logger.getLogger(XcssPollingThemeEngine.class);

	private URI currentTheme;
	private StyleSheet currentStyleSheet;
	private Display display;
	private Map<URI, ITheme> uriToTheme;
	private Map<String, ITheme> idToTheme;
	private XcssRendererHelper renderer;

	private static final String THEMEID_KEY = "themeid";

	public XcssPollingThemeEngine(Display display, XcssRendererHelper renderer) {
		this.display = display;
		this.uriToTheme = Maps.newHashMap();
		this.idToTheme = Maps.newHashMap();
		this.renderer = renderer;
		
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.e4.ui.css.swt.theme.xcss");

		for (IExtension extension : extensionPoint.getExtensions()) {
			for (IConfigurationElement element : extension.getConfigurationElements()) {
				if (element.getName().equals("theme")) {
					try {
						registerTheme(
								element.getAttribute("id"),
								element.getAttribute("label"),
								"platform:/plugin/" + element.getContributor().getName()
										+ "/"
										+ element.getAttribute("basestylesheeturi"));						
					} catch (IllegalArgumentException illegalArgument) {
						logger.error(illegalArgument.getMessage(), illegalArgument);
					}
				}
			}
		}
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				conditionalApplyTheme();
			}
		}, 0, 1000);
	}

	private int prevHash = -1;
	
	protected void conditionalApplyTheme() {
		URI currentTheme = this.currentTheme;
		if (currentTheme != null) {
			if (currentTheme.isPlatform()) {
				try {
					URL themeURL = new URL(currentTheme.toString());
					InputStream stream = FileLocator.resolve(themeURL).openStream();
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
						StringBuilder newState = new StringBuilder();
						String line = null;
						while((line = reader.readLine()) != null) {
							newState.append(line);
						}
						String newStateAsString = newState.toString();
						if (currentStyleSheet != null && prevHash != -1 && prevHash == newStateAsString.hashCode()) {
							return;
						}
						prevHash = newStateAsString.hashCode();
					} finally {
						stream.close();
					}
				} catch (MalformedURLException e) {
					logger.error(e.getMessage(), e);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				} 
			}
			StyleSheet styleSheet = getStyleSheet(currentTheme);
			if (styleSheet != null) {
				this.currentStyleSheet = styleSheet;
				applyStyles();
			}
		}
	}

	public StyleSheet getStyleSheet(URI currentTheme) {
		XtextResourceSet resourceSet = renderer.createResourceSet();
		resourceSet.setClasspathURIContext(getClass().getClassLoader());
		new ClasspathTypeProvider(getClass().getClassLoader(), resourceSet);
		Resource resource = resourceSet.createResource(currentTheme);
		try {
			URL url = new URL(currentTheme.toString());
			InputStream stream = FileLocator.resolve(url).openStream();
			resource.load(stream, null);
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		StyleSheet styleSheet = renderer.getValidStyleSheet(resource);
		return styleSheet;
	}

	public synchronized ITheme registerTheme(String id, String label,
			String stylesheetURI) throws IllegalArgumentException {
		URI uri = URI.createURI(stylesheetURI);
		if (uriToTheme.containsKey(uri))
			throw new IllegalArgumentException("A theme with the given uri '" + stylesheetURI
					+ "' is already registered");
		XcssTheme theme = new XcssTheme(uri, id, label);
		uriToTheme.put(uri, theme);
		idToTheme.put(id, theme);
		return theme;
	}

	public synchronized void registerStylesheet(String uri, String... themes) {
		throw new UnsupportedOperationException(getClass() + "#registerStylesheet");
	}

	public synchronized void registerResourceLocator(IResourceLocator locator,
			String... themes) {
		throw new UnsupportedOperationException(getClass() + "#registerResourceLocator");
	}

	public void setTheme(String themeId, boolean restore) {
		ITheme theme = idToTheme.get(themeId);
		if (theme != null) {
			setTheme(theme, restore);
		}
	}
	
	protected void applyStyles() {
		new XcssStyleApplier(display, currentStyleSheet, renderer).applyStyles();
	}

	public void setTheme(ITheme theme, boolean restore) {
		Assert.isNotNull(theme, "The theme must not be null");
		if (currentTheme == null || !currentTheme.toString().equals(theme.toString())) {
			XcssTheme newTheme = (XcssTheme) theme;
			this.currentTheme = newTheme.getURI();
			StyleSheet newStyleSheet = getStyleSheet(this.currentTheme);
			if (newStyleSheet != null) {
				currentStyleSheet = newStyleSheet;
				applyStyles();				
			}
		}
		
		if (restore) {
			IEclipsePreferences pref = getPreferences();
			pref.put(THEMEID_KEY, theme.getId());
			try {
				pref.flush();
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public synchronized List<ITheme> getThemes() {
		return ImmutableList.copyOf(uriToTheme.values());
	}

	public void applyStyles(Widget widget, boolean applyStylesToChildNodes) {
		new XcssStyleApplier(display, currentStyleSheet, renderer).applyStyles(widget, applyStylesToChildNodes);
	}

	private String getPreferenceThemeId() {
		return getPreferences().get(THEMEID_KEY, null);
	}

	private IEclipsePreferences getPreferences() {
		return new InstanceScope().getNode(FrameworkUtil.getBundle(
				XcssPollingThemeEngine.class).getSymbolicName());
	}

	public void restore(String alternateTheme) {
		String prefThemeId = getPreferenceThemeId();
		boolean flag = true;
		if (prefThemeId != null) {
			for (ITheme t : getThemes()) {
				if (prefThemeId.equals(t.getId())) {
					setTheme(t, false);
					flag = false;
					break;
				}
			}
		}

		if (alternateTheme != null && flag) {
			setTheme(alternateTheme, false);
		}
	}
	
	public ITheme getActiveTheme() {
		return uriToTheme.get(currentTheme);
	}
	
	public CSSStyleDeclaration getStyle(Widget widget) {
		return null;
	}
}