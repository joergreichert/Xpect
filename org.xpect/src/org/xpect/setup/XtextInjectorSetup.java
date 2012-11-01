/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.setup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface XtextInjectorSetup {
	public class NullModule implements Module {
		public void configure(Binder binder) {
		}
	}

	Class<? extends Module> pluginTestModule() default NullModule.class;

	Class<? extends Module> standaloneTestModule() default NullModule.class;

	Class<? extends Module> workbenchModule() default NullModule.class;
}
