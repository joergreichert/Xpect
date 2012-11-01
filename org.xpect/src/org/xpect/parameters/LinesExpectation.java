/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import org.eclipse.xtext.util.Pair;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.xpect.parameters.ActualCollection.ActualItem;
import org.xpect.parameters.ActualCollection.ToString;
import org.xpect.parameters.ExpectationCollection.ExpectationItem;
import org.xpect.parameters.LinesExpectation.LinesExpectationParser;
import org.xpect.runner.IXpectParameterProvider.IXpectSingleParameterProvider;
import org.xpect.runner.XpectSingleParameterProvider;
import org.xpect.runner.XpectTestRunner;
import org.xpect.util.IRegion;
import org.xpect.util.ITypedProvider;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@XpectSingleParameterProvider(LinesExpectationParser.class)
public @interface LinesExpectation {

	public class LinesExpectationImpl extends AbstractExpectation implements ILinesExpectation {

		private final LinesExpectation annotation;

		public LinesExpectationImpl(LinesExpectation annotation, String document, int offset, int lenght) {
			super(document, offset, lenght);
			this.annotation = annotation;
		}

		public void assertEquals(Iterable<?> actual) {
			assertEquals("", actual, false);
		}

		public void assertEquals(String message, Iterable<?> actual) {
			assertEquals(message, actual, false);
		}

		protected void assertEquals(String message, Iterable<?> actual, boolean forceFail) {
			Assert.assertNotNull(actual);
			String indentation = getIndentation();

			ExpectationCollection exp = new ExpectationCollection();
			exp.setCaseSensitive(annotation.caseSensitive());
			exp.setOrdered(annotation.ordered());
			exp.setQuoted(annotation.quoted());
			exp.setSeparator('\n');
			exp.setWhitespaceSensitive(annotation.whitespaceSensitive());
			exp.init(getExpectation(indentation));

			ActualCollection act = new ActualCollection();
			act.setCaseSensitive(annotation.caseSensitive());
			act.setOrdered(annotation.ordered());
			act.setQuoted(annotation.quoted());
			act.setSeparator('\n');
			act.setWhitespaceSensitive(annotation.whitespaceSensitive());
			act.init(actual, annotation.itemFormatter());

			if (!exp.matches(act)) {
				List<String> expString = Lists.newArrayList();
				List<String> actString = Lists.newArrayList();
				for (Pair<Collection<ExpectationItem>, ActualItem> pair : exp.map(act)) {
					if (pair.getFirst() != null && !pair.getFirst().isEmpty()) {
						if (pair.getSecond() != null)
							expString.add(pair.getSecond().getEscaped());
						else
							expString.add(pair.getFirst().iterator().next().getEscaped());
					}
					if (pair.getSecond() != null)
						actString.add(pair.getSecond().getEscaped());
				}
				String expDoc = replaceInDocument(indentation, Joiner.on('\n').join(expString));
				String actDoc = replaceInDocument(indentation, Joiner.on('\n').join(actString));
				throw new ComparisonFailure(message, expDoc, actDoc);
			}
		}

		public void fail(String message, Iterable<?> actual) {
			assertEquals(message, actual, true);
		}

		public LinesExpectation getAnnotation() {
			return annotation;
		}
	}

	public class LinesExpectationParser extends AbstractExpectationParser implements IXpectSingleParameterProvider {

		private final LinesExpectation annotation;

		public LinesExpectationParser(LinesExpectation cfg) {
			super();
			this.annotation = cfg;
		}

		public LinesExpectation getAnnotation() {
			return annotation;
		}

		public ITypedProvider parseRegion(XpectTestRunner invocation, int paramIndex, List<IClaimedRegion> claims) {
			IRegion region = claimRegion(invocation, paramIndex);
			if (region != null)
				return new LinesExpectationImpl(annotation, invocation.getDocument(), region.getOffset(), region.getLength());
			return null;
		}

	}

	boolean caseSensitive() default true;

	Class<? extends Function<Object, String>> itemFormatter() default ToString.class;

	boolean ordered() default false;

	boolean quoted() default false;

	boolean whitespaceSensitive() default false;

}
