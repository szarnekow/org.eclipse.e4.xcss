/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.e4.ui.css.swt.theme.xcss;

import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.emf.common.util.URI;

/**
 * @author Sebastian Zarnekow
 */
public class XcssTheme implements ITheme{

	private final URI uri;

	public XcssTheme(URI uri) {
		this.uri = uri;
	}
	
	public String getId() {
		return uri.toString();
	}
	
	public URI getIdAsURI() {
		return uri;
	}

	public String getLabel() {
		String fileName = uri.trimFileExtension().lastSegment();
		return fileName;
	}

}
