/*******************************************************************************
 * Copyright (c) 2010 Siemens AG and others.
 * 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Kai Tödter - initial implementation
 ******************************************************************************/

package org.eclipse.e4.demo.contacts.processors;

import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.emf.ecore.EObject;

public class ToolbarThemeProcessor extends AbstractThemeProcessor {

	@Inject
	@Named("toolbar:org.eclipse.ui.main.toolbar")
	private MToolBar toolbar;

	@Override
	protected boolean check() {
		return toolbar != null;
	}

	@Override
	protected void preprocess() {
		toolbar.getChildren().add(
				MMenuFactory.INSTANCE.createToolBarSeparator());
	}

	@Override
	protected void processTheme(String name, MCommand switchCommand,
			MParameter themeId, String iconURI) {
		MHandledToolItem toolItem = MMenuFactory.INSTANCE
				.createHandledToolItem();
		toolItem.setTooltip(name);
		toolItem.setCommand(switchCommand);
		toolItem.getParameters().add(themeId);
		if (iconURI != null) {
			toolItem.setIconURI(iconURI);
		}
		toolbar.getChildren().add(toolItem);
	}

	@Override
	protected void postprocess() {
	}

	@Override
	protected MApplication getApplication() {
		return (MApplication) (((EObject) toolbar).eContainer()).eContainer().eContainer();
	}
}
