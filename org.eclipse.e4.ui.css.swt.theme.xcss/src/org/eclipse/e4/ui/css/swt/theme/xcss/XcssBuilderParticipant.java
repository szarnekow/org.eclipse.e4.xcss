/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.theme.xcss;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtext.builder.IXtextBuilderParticipant;
import org.eclipse.xtext.example.css.ui.AccessibleXcssActivator;
import org.eclipse.xtext.example.css.ui.JdtAwareRenderingHelper;
import org.eclipse.xtext.example.css.xcss.StyleSheet;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

/**
 * @author Sebastian Zarnekow
 */
public class XcssBuilderParticipant implements IXtextBuilderParticipant {

	private static final Logger logger = Logger.getLogger(XcssBuilderParticipant.class);
	
	private JdtAwareRenderingHelper renderer;

	private IResourceValidator validator;
	
	public XcssBuilderParticipant() {
		renderer = AccessibleXcssActivator.getInstance().getRenderer();
		validator = AccessibleXcssActivator.getInstance().getValidator();
	}
	
	public void build(IBuildContext context, IProgressMonitor monitor)
			throws CoreException {
		logger.debug("BuilderParticipant.build");
		
		final ITheme[] activeTheme = new ITheme[] {null};
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				IThemeManager themeEngineManager = XcssThemeEngineManager.getInstance();
				IThemeEngine themeEngine = themeEngineManager.getEngineForDisplay(Display.getDefault());
				activeTheme[0] = themeEngine.getActiveTheme();
			}
			
		});
		if (activeTheme[0] instanceof XcssTheme) {
			URI activeThemeID = ((XcssTheme) activeTheme[0]).getIdAsURI();
			for(IResourceDescription.Delta delta: context.getDeltas()) {
				if (activeThemeID.equals(delta.getUri())) {
					Resource res = context.getResourceSet().getResource(activeThemeID, true);
					if (res != null && !res.getContents().isEmpty()) {
						List<Issue> issues = validator.validate(res, CheckMode.NORMAL_AND_FAST, CancelIndicator.NullImpl);
						if (issues.isEmpty()) {
							StyleSheet styleSheet = (StyleSheet) res.getContents().get(0);
							new XcssStyleApplier(Display.getDefault(), styleSheet, renderer);
						}
					}
					return;
				}
			}
		}
	}

}
