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
	private final String label;
	private final String id;

	public XcssTheme(URI uri, String id, String label) {
		this.uri = uri;
		this.id = id;
		this.label = label;
	}
	
	public String getId() {
		return id;
	}
	
	public URI getURI() {
		return uri;
	}

	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return uri.toString();
	}

}
