package org.eclipse.e4.ui.css.swt.theme.xcss;

import org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngineManager;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.widgets.Display;

public class XcssThemeEngineManager extends ThemeEngineManager {
	private static final String KEY = "org.eclipse.e4.ui.css.swt.theme";
	
	public IThemeEngine getEngineForDisplay(Display display) {
		System.out.println("org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngineManager.getEngineForDisplay(Display)");
		IThemeEngine engine = (IThemeEngine) display.getData(KEY);
		
		if( engine == null ) {
			engine = new XcssThemeEngine(display);
			display.setData(KEY, engine);
		}
		
		return engine;
	}
}