/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.theme.xcss;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.e4.ui.css.core.util.resources.IResourceLocator;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.xtext.builder.builderState.IBuilderState;
import org.eclipse.xtext.example.css.ui.AccessibleXcssActivator;
import org.eclipse.xtext.example.css.ui.JdtAwareRenderingHelper;
import org.eclipse.xtext.example.css.xcss.StyleSheet;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.ui.shared.Access;
import org.w3c.dom.css.CSSStyleDeclaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * @author Sebastian Zarnekow
 */
public class XcssLiveThemeEngine implements IThemeEngine, IResourceDescription.Event.Listener {
	
	private static final Logger logger = Logger.getLogger(XcssLiveThemeEngine.class);
	
	private URI currentThemeId;
	private Display display;
	private IBuilderState resourceDescriptions;
	private Map<URI, ITheme> themes;
	private JdtAwareRenderingHelper renderer;

	public XcssLiveThemeEngine(Display display) {
		logger.debug("Instantiated XcssLiveThemeEngine for display: " + display.toString());
		this.display = display;
		this.themes = Maps.newHashMap();
		resourceDescriptions = Access.getIBuilderState().get();
		renderer = AccessibleXcssActivator.getInstance().getRenderer();
		for(IResourceDescription description: resourceDescriptions.getAllResourceDescriptions()) {
			if ("xcss".equals(description.getURI().fileExtension())) {
				registerTheme(description.getURI());
			}
		}
		resourceDescriptions.addListener(this);
	}

	protected void registerTheme(URI uri) {
		XcssTheme theme = new XcssTheme(uri);
		themes.put(uri, theme);
		if (currentThemeId == null)
			currentThemeId = uri;
	}

	public void descriptionsChanged(IResourceDescription.Event event) {
		logger.debug("Descriptions changed");
		boolean currentThemeChanged = false;
		for(IResourceDescription.Delta delta: event.getDeltas()) {
			if ("xcss".equals(delta.getUri().fileExtension())) {
				if (delta.getNew() == null)
					themes.remove(delta.getUri());
				else
					registerTheme(delta.getUri());
				if (delta.getUri().equals(currentThemeId))
					currentThemeChanged = true;
			}
		}
		if (currentThemeChanged) {
			applyStyles();
		}
	}

	protected void applyStyles() {
		StyleSheet styleSheet = renderer.getStyleSheet(currentThemeId);
		if (styleSheet != null) {
			applyStyles(styleSheet);
		}
	}
	
	protected void applyStyles(final StyleSheet styleSheet) {
		new XcssStyleApplier(display, styleSheet, renderer).applyStyles();
	}
	
	public synchronized ITheme registerTheme(String id, String label, String basestylesheetURI) throws IllegalArgumentException {
		return registerTheme(id, label, basestylesheetURI, "");
	}
	
	public synchronized ITheme registerTheme(String id, String label,
			String basestylesheetURI, String osVersion) throws IllegalArgumentException {
//		throw new UnsupportedOperationException(getClass() + "#registerTheme(..)");
		return null;
	}

	public synchronized void registerStylesheet(String uri, String... themes) {
		throw new UnsupportedOperationException(getClass() + "#registerStylesheet(..)");
	}

	public synchronized void registerResourceLocator(IResourceLocator locator, String... themes) {
		throw new UnsupportedOperationException(getClass() + "#registerResourceLocator(..)");
	}

	public void setTheme(String themeId, boolean restore) {
		ITheme theme = themes.get(URI.createURI(themeId));
		setTheme(theme, restore);
	}

	public void setTheme(ITheme theme, boolean restore) {
		if (theme == null) {
			if (currentThemeId != null)
				applyStyles();
			return;
		}
		URI newTheme = URI.createURI(theme.getId());
		if (!newTheme.equals(this.currentThemeId)) {
			this.currentThemeId = newTheme;
			applyStyles();
		}
	}

	public synchronized List<ITheme> getThemes() {
		return ImmutableList.copyOf(themes.values());
	}

	public void applyStyles(Widget widget, boolean applyStylesToChildNodes) {
		StyleSheet styleSheet = renderer.getStyleSheet(currentThemeId);
		if (styleSheet != null) {
			new XcssStyleApplier(display, styleSheet, renderer).applyStyles(widget, applyStylesToChildNodes);
		}
	}

//	// TODO may not be ideal??
//	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=312842
//	public CSSEngine getCSSEngine() {
//		return engine;
//	}

//	private String getPreferenceThemeId() {
//		return getPreferences().get(THEMEID_KEY, null);
//	}

//	private IEclipsePreferences getPreferences() {
//		return new InstanceScope().getNode(FrameworkUtil.getBundle(
//				XcssLiveThemeEngine.class).getSymbolicName());
//	}

	public void restore(String alternateTheme) {
		throw new UnsupportedOperationException(getClass().getName() + "#alternateTheme");
//		String prefThemeId = getPreferenceThemeId();
//		boolean flag = true;
//		if (prefThemeId != null) {
//			for (ITheme t : getThemes()) {
//				if (prefThemeId.equals(t.getId())) {
//					setTheme(t, false);
//					flag = false;
//					break;
//				}
//			}
//		}
//
//		if (alternateTheme != null && flag) {
//			setTheme(alternateTheme, false);
//		}
	}
	
	public ITheme getActiveTheme() {
		return themes.get(currentThemeId);
	}
	
	public CSSStyleDeclaration getStyle(Widget widget) {
		return null;
	}
}