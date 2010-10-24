/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.theme.xcss;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.swt.widgets.Display;

/**
 * Heavily adapted from the default implementation.
 * @see org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngineManager 
 * @author Sebastian Zarnekow
 */
public class XcssThemeEngineManager implements IThemeManager {
	
	private static final String KEY = "org.eclipse.e4.ui.css.swt.theme";
	
	public IThemeEngine getEngineForDisplay(Display display) {
		IThemeEngine engine = (IThemeEngine) display.getData(KEY);
		
		if( engine == null ) {
			engine = new XcssLiveThemeEngine(display);
			display.setData(KEY, engine);
		}
		
		return engine;
	}
}