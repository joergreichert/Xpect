/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.ui.services;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.ui.resource.IResourceUIServiceProvider;
import org.xpect.ui.internal.XpectActivator;
import org.xpect.ui.util.XtInjectorSetupUtil;
import org.xpect.util.URIDelegationHandler;

import com.google.inject.Injector;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class XtResourceUIServiceProviderProvider implements IResourceServiceProvider.Provider {
	public IResourceServiceProvider get(URI uri, String contentType) {
		String ext = new URIDelegationHandler().getOriginalFileExtension(uri.lastSegment());
		if (ext != null) {
			Injector injector = XtInjectorSetupUtil.getWorkbenchInjector(uri, ext);
			if (injector != null)
				return injector.getInstance(IResourceUIServiceProvider.class);
		}
		return XpectActivator.getInstance().getInjector(XpectActivator.ORG_XPECT_XPECT).getInstance(IResourceUIServiceProvider.class);
	}
}
