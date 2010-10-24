/*******************************************************************************
 * Copyright (c) 2010 Tom Schindl and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.internal.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.core.engine.CSSErrorHandler;
import org.eclipse.e4.ui.css.core.util.impl.resources.OSGiResourceLocator;
import org.eclipse.e4.ui.css.core.util.resources.IResourceLocator;
import org.eclipse.e4.ui.css.swt.engine.CSSSWTEngineImpl;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSStyleDeclaration;

public class ThemeEngine implements IThemeEngine {
	private List<Theme> themes = new ArrayList<Theme>();

	private CSSEngine engine;
	private ITheme currentTheme;
	private Display display;

	private List<String> globalStyles = new ArrayList<String>();
	private List<IResourceLocator> globalSourceLocators = new ArrayList<IResourceLocator>();

	private HashMap<String, List<String>> stylesheets = new HashMap<String, List<String>>();
	private HashMap<String, List<IResourceLocator>> sourceLocators = new HashMap<String, List<IResourceLocator>>();

	private static final String THEMEID_KEY = "themeid";

	public ThemeEngine(Display display) {
		this.engine = new CSSSWTEngineImpl(display, true);
		this.display = display;
		this.engine.setErrorHandler(new CSSErrorHandler() {

			public void error(Exception e) {
				// TODO Use the logger
				e.printStackTrace();
			}
		});
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint extPoint = registry
				.getExtensionPoint("org.eclipse.e4.ui.css.swt.theme");

		for (IExtension e : extPoint.getExtensions()) {
			for (IConfigurationElement ce : getPlatformMatches(e
					.getConfigurationElements())) {
				if (ce.getName().equals("theme")) {
					try {
						String version = ce.getAttribute("os_version");
						if (version == null) version ="";
						registerTheme(
								ce.getAttribute("id") + version,
								ce.getAttribute("label"),
								"platform:/plugin/" + ce.getContributor().getName()
										+ "/"
										+ ce.getAttribute("basestylesheeturi"), version);						
					} catch (IllegalArgumentException e1) {
						//TODO Can we somehow use logging?
						e1.printStackTrace();
					}
				}
			}
		}

		for (IExtension e : extPoint.getExtensions()) {
			for (IConfigurationElement ce : getPlatformMatches(e.getConfigurationElements())) {
				if (ce.getName().equals("stylesheet")) {
					IConfigurationElement[] cces = ce.getChildren("themeid");
					if (cces.length == 0) {
						registerStylesheet("platform:/plugin/"
								+ ce.getContributor().getName() + "/"
								+ ce.getAttribute("uri"));

						for (IConfigurationElement resourceEl : ce
								.getChildren("osgiresourcelocator")) {
							String uri = resourceEl.getAttribute("uri");
							if (uri != null) {
								registerResourceLocator(new OSGiResourceLocator(
										uri));
							}
						}
					} else {
						String[] themes = new String[cces.length];
						for (int i = 0; i < cces.length; i++) {
							themes[i] = cces[i].getAttribute("refid");
						}
						registerStylesheet(
								"platform:/plugin/"
										+ ce.getContributor().getName() + "/"
										+ ce.getAttribute("uri"), themes);

						for (IConfigurationElement resourceEl : ce
								.getChildren("osgiresourcelocator")) {
							String uri = resourceEl.getAttribute("uri");
							if (uri != null) {
								registerResourceLocator(new OSGiResourceLocator(
										uri));
							}
						}
					}
				}
			}
		}

		display.setData("org.eclipse.e4.ui.css.core.engine", engine);
	}

	public synchronized ITheme registerTheme(String id, String label,
			String basestylesheetURI) throws IllegalArgumentException {
		return  registerTheme(id, label, basestylesheetURI, "");
	}
	public synchronized ITheme registerTheme(String id, String label,
			String basestylesheetURI, String osVersion) throws IllegalArgumentException {
		for (Theme t : themes) {
			if (t.getId().equals(id)) {
				throw new IllegalArgumentException("A theme with the id '" + id
						+ "' is already registered");
			}
		}
		Theme theme = new Theme(id, label);
		if (osVersion != "") theme.setOsVersion(osVersion);
		themes.add(theme);
		registerStyle(id, basestylesheetURI);
		return theme;
	}

	public synchronized void registerStylesheet(String uri, String... themes) {
		Bundle bundle = FrameworkUtil.getBundle(ThemeEngine.class);
		String osname = bundle.getBundleContext().getProperty("osgi.os");
		String wsname = bundle.getBundleContext().getProperty("ogsi.ws");

		uri = uri.replaceAll("\\$os\\$", osname).replaceAll("\\$ws\\$", wsname);

		if (themes.length == 0) {
			globalStyles.add(uri);
		} else {
			for (String t : themes) {
				registerStyle(t, uri);
			}
		}
	}

	public synchronized void registerResourceLocator(IResourceLocator locator,
			String... themes) {
		if (themes.length == 0) {
			globalSourceLocators.add(locator);
		} else {
			for (String t : themes) {
				List<IResourceLocator> list = sourceLocators.get(t);
				if (list == null) {
					list = new ArrayList<IResourceLocator>();
					sourceLocators.put(t, list);
				}
				list.add(locator);
			}
		}
	}

	private void registerStyle(String id, String stylesheet) {
		List<String> s = stylesheets.get(id);
		if (s == null) {
			s = new ArrayList<String>();
			stylesheets.put(id, s);
		}
		s.add(stylesheet);
	}

	private List<String> getAllStyles(String id) {
		List<String> s = stylesheets.get(id);
		if (s == null) {
			s = Collections.emptyList();
		}

		s = new ArrayList<String>(s);
		s.addAll(globalStyles);
		return s;
	}

	private List<IResourceLocator> getResourceLocators(String id) {
		List<IResourceLocator> list = new ArrayList<IResourceLocator>(
				globalSourceLocators);
		List<IResourceLocator> s = sourceLocators.get(id);
		if (s != null) {
			list.addAll(s);
		}

		return list;
	}

	/**
	 * Get all elements that have os/ws attributes that best match the current
	 * platform.
	 * 
	 * @param elements
	 *            the elements to check
	 * @return the best matches, if any
	 */
	private IConfigurationElement[] getPlatformMatches(
			IConfigurationElement[] elements) {
		Bundle bundle = FrameworkUtil.getBundle(ThemeEngine.class);
		String osname = bundle.getBundleContext().getProperty("osgi.os");
		// TODO: Need to differentiate win32 versions
		String os_version = System.getProperty("os.version");
		String wsname = bundle.getBundleContext().getProperty("ogsi.ws");
		ArrayList<IConfigurationElement> matchingElements = new ArrayList<IConfigurationElement>();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			String elementOs = element.getAttribute("os");
			String elementWs = element.getAttribute("ws");
			String elementOsVersion = element.getAttribute("os_version");
			if (osname != null
					&& (elementOs == null || elementOs.contains(osname))) {
				if (os_version != null && os_version.equalsIgnoreCase(elementOsVersion)) {
					// best match
					matchingElements.add(element);
					continue;
				}
				matchingElements.add(element);
			} else if (wsname != null && wsname.equalsIgnoreCase(elementWs)) {
				matchingElements.add(element);
			}
		}
		return (IConfigurationElement[]) matchingElements
				.toArray(new IConfigurationElement[matchingElements.size()]);
	}

	public void setTheme(String themeId, boolean restore) {
		String osVersion = System.getProperty("os.version");
		if (osVersion != null) {
			boolean found = false;
			for (Theme t : themes) {
				String version = t.getOsVersion();
				if (version != null && osVersion.contains(version)) {
					String themeVersion = themeId + version;
					if (t.getId().equals(themeVersion)) {
						setTheme(t, restore);
						found = true;
						break;
					}
				}
			}
			if (found) return;
		}
		//try generic
		for (Theme t : themes) {
			if (t.getId().equals(themeId)) {
				setTheme(t, restore);
				break;
			}
		}
	}

	public void setTheme(ITheme theme, boolean restore) {
		Assert.isNotNull(theme, "The theme must not be null");

		if (this.currentTheme != theme) {
			if (currentTheme != null) {
				for (IResourceLocator l : getResourceLocators(currentTheme
						.getId())) {
					engine.getResourcesLocatorManager()
							.unregisterResourceLocator(l);
				}
			}

			this.currentTheme = theme;
			engine.reset();

			for (IResourceLocator l : getResourceLocators(theme.getId())) {
				engine.getResourcesLocatorManager().registerResourceLocator(l);
			}

			for (String stylesheet : getAllStyles(theme.getId())) {
				URL url;
				InputStream stream = null;
				try {
					url = FileLocator.resolve(new URL(stylesheet.toString()));
					stream = url.openStream();
					engine.parseStyleSheet(stream);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}

			Shell[] shells = display.getShells();
			for (Shell s : shells) {
				try {
					s.setRedraw(false);
					s.reskin(SWT.ALL);
					applyStyles(s, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					s.setRedraw(true);
				}
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
		return Collections.unmodifiableList(new ArrayList<ITheme>(themes));
	}

	public void applyStyles(Widget widget, boolean applyStylesToChildNodes) {
		engine.applyStyles(widget, applyStylesToChildNodes);
	}

	// TODO may not be ideal??
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=312842
	public CSSEngine getCSSEngine() {
		return engine;
	}

	private String getPreferenceThemeId() {
		return getPreferences().get(THEMEID_KEY, null);
	}

	private IEclipsePreferences getPreferences() {
		return new InstanceScope().getNode(FrameworkUtil.getBundle(
				ThemeEngine.class).getSymbolicName());
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
		return currentTheme;
	}
	
	public CSSStyleDeclaration getStyle(Widget widget) {
		Element e = engine.getCSSElementContext(widget).getElement();
		if( e == null ) {
			return null;
		}
		return engine.getViewCSS().getComputedStyle(e, null);
	}
}