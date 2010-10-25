package org.eclipse.e4.ui.css.swt.theme.xcss;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
//import org.eclipse.xtext.example.css.ui.rendering.XcssRendererHelper;
import org.eclipse.xtext.example.css.rendering.XcssRendererHelper;
import org.eclipse.xtext.example.css.xcss.StyleSheet;

/**
 * @author Sebastian Zarnekow
 */
public class XcssStyleApplier implements Runnable {
	
	private static final Logger logger = Logger.getLogger(XcssStyleApplier.class);
	
	private final StyleSheet styleSheet;
	private final Display display;
	private final XcssRendererHelper renderer;

	public XcssStyleApplier(Display display, StyleSheet styleSheet, XcssRendererHelper renderer) {
		this.display = display;
		this.styleSheet = styleSheet;
		this.renderer = renderer;
	}

	public void run() {
		for(Shell shell: display.getShells()) {
			applyStyles(shell, true);
		}
	}

	public void applyStyles() {
		display.syncExec(this);
	}
	
	public void applyStyles(Widget widget, boolean recurse) {
		if (widget instanceof Shell) {
			Shell shell = (Shell) widget;
			try {
				shell.setRedraw(false);
				shell.reskin(SWT.ALL);
				renderer.applyStyles(display, styleSheet, shell, recurse);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				shell.setRedraw(true);
			}
		} else {
			renderer.applyStyles(display, styleSheet, widget, recurse);
		}
	}

}