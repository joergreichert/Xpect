/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.scoping;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmAnnotationReference;
import org.eclipse.xtext.common.types.JvmAnnotationType;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportNormalizer;
import org.eclipse.xtext.scoping.impl.ImportScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.util.Strings;
import org.junit.Test;
import org.xpect.AbstractComponent;
import org.xpect.Component;
import org.xpect.XpectFile;
import org.xpect.XpectPackage;
import org.xpect.XpectTest;
import org.xpect.runner.Xpect;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class XpectScopeProvider extends AbstractScopeProvider {

	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider delegate;

	private String getAssignmentTargetFeatureName(JvmFeature feature) {
		if (feature instanceof JvmOperation) {
			JvmOperation op = (JvmOperation) feature;
			if (op.getParameters().size() == 1) {
				String fullname = op.getSimpleName();
				if (fullname.startsWith("set") || fullname.startsWith("add")) {
					String name = Strings.toFirstLower(fullname.substring(3));
					if (name.length() > 0)
						return name;
				}
			}
		}
		return null;
	}

	public IScope getScope(EObject context, EReference reference) {
		if (reference == XpectPackage.Literals.XPECT_INVOCATION__ELEMENT)
			return getScopeForXpectInvocationElement(EcoreUtil2.getContainerOfType(context, XpectFile.class));
		if (reference == XpectPackage.Literals.ASSIGNMENT__DECLARED_TARGET)
			return getScopeForAssignmentTarget(EcoreUtil2.getContainerOfType(context, AbstractComponent.class));
		if (reference == XpectPackage.Literals.COMPONENT__COMPONENT_CLASS) {
			return getScopeForInstanceType(EcoreUtil2.getContainerOfType(context, AbstractComponent.class), reference);
		}
		return delegate.getScope(context, reference);
	}

	private IScope getScopeForAssignmentTarget(AbstractComponent owner) {
		JvmDeclaredType componentClass = owner.getComponentClass();
		if (componentClass == null || componentClass.eIsProxy())
			return IScope.NULLSCOPE;
		JvmDeclaredType type = componentClass;
		List<IEObjectDescription> descs = Lists.newArrayList();
		for (JvmFeature feature : type.getAllFeatures()) {
			String name = getAssignmentTargetFeatureName(feature);
			if (name != null)
				descs.add(EObjectDescription.create(QualifiedName.create(name), feature));
		}
		return new SimpleScope(descs);
	}

	private IScope getScopeForInstanceType(AbstractComponent instance, EReference reference) {
		IScope scope = delegate.getScope(instance, reference);
		if (instance instanceof XpectTest)
			return scope;
		if (instance instanceof Component) {
			Set<String> packages = Sets.newLinkedHashSet();
			AbstractComponent current = ((Component) instance).getAssignment().getInstance();
			while (true) {
				if (current instanceof Component) {
					JvmDeclaredType componentClass = current.getComponentClass();
					if (componentClass != null && !componentClass.eIsProxy())
						packages.add(componentClass.getPackageName());
					current = ((Component) current).getAssignment().getInstance();
				} else if (current instanceof XpectTest) {
					JvmDeclaredType setup = ((XpectTest) current).getSetupClass();
					if (setup != null)
						packages.add(setup.getPackageName());
					break;
				} else
					break;
			}
			List<String> pkgs = Lists.newArrayList(packages);
			Collections.reverse(pkgs);
			for (String pkg : pkgs) {
				ImportNormalizer in = new ImportNormalizer(QualifiedName.create(pkg.split("\\.")), true, false);
				scope = new ImportScope(Collections.singletonList(in), scope, null, reference.getEReferenceType(), false);
			}
			return scope;
		}
		return IScope.NULLSCOPE;
	}

	private IScope getScopeForXpectInvocationElement(XpectFile file) {
		XpectTest test = file.getTest();
		if (test == null)
			return IScope.NULLSCOPE;
		JvmDeclaredType testClass = test.getTestClass();
		if (testClass == null || test.getTestClass().eIsProxy())
			return IScope.NULLSCOPE;
		JvmDeclaredType type = test.getTestClass();
		List<IEObjectDescription> descs = Lists.newArrayList();
		for (JvmFeature feature : type.getAllFeatures())
			if (isXpectInvocationOperation(feature))
				descs.add(EObjectDescription.create(QualifiedName.create(feature.getSimpleName()), feature));
		return new SimpleScope(descs);
	}

	private boolean isXpectInvocationOperation(JvmFeature feature) {
		if (feature instanceof JvmOperation) {
			for (JvmAnnotationReference ref : feature.getAnnotations()) {
				JvmAnnotationType annotation = ref.getAnnotation();
				if (annotation != null && !annotation.eIsProxy()) {
					if (annotation.getQualifiedName().equals(Test.class.getName()))
						return true;
					if (annotation.getQualifiedName().equals(Xpect.class.getName()))
						return true;
				}
			}
		}
		return false;
	}
}
