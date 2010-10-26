/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.demo.contacts;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class BundleActivatorImpl implements BundleActivator {

	private static final String F_META_AREA = ".metadata"; //$NON-NLS-1$
	private static final String F_PLUGIN_DATA = ".plugins"; //$NON-NLS-1$

	private static BundleActivatorImpl instance;

	private BundleContext context;

	private ServiceTracker locationServiceTracker;

	private IPath stateLocation;

	public void start(BundleContext context) throws Exception {
		instance = this;
		this.context = context;
	}

	public void stop(BundleContext context) throws Exception {
		this.context = null;
		instance = null;
	}

	public IPath getStateLocation() {
		try {
			if (stateLocation == null) {
				Filter filter = context.createFilter(Location.INSTANCE_FILTER);
				if (locationServiceTracker == null) {
					locationServiceTracker = new ServiceTracker(context,
							filter, null);
					locationServiceTracker.open();
				}
				Location location = (Location) locationServiceTracker
						.getService();
				if (location != null) {
					IPath path = new Path(location.getURL().getPath());
					stateLocation = path.append(F_META_AREA).append(
							F_PLUGIN_DATA).append(
							context.getBundle().getSymbolicName());
					stateLocation.toFile().mkdirs();
				}
			}
		} catch (InvalidSyntaxException e) {
			// ignore this. It should never happen as we have tested the above
			// format.
		}
		return stateLocation;
	}

	public static BundleActivatorImpl getInstance() {
		return instance;
	}

}
