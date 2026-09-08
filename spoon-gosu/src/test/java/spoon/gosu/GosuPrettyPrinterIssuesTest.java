/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;

import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GosuPrettyPrinterIssuesTest {

	private GosuPrettyPrinter newPrinter(Factory factory) {
		return new GosuPrettyPrinter(factory.getEnvironment());
	}

	@Test
	void multiCatchUnionTypesArePrinted() {
		Factory factory = new Launcher().getFactory();
		CtCatch ctCatch = factory.Core().createCatch();
		CtCatchVariable<Throwable> cv = factory.Core().createCatchVariable();
		cv.setSimpleName("ex");
		cv.setType(factory.Type().createReference("java.lang.Exception"));
		cv.setMultiTypes(List.of(
				factory.Type().createReference("java.io.IOException"),
				factory.Type().createReference("java.sql.SQLException")));
		ctCatch.setParameter(cv);
		ctCatch.setBody(factory.Core().createBlock());

		String out = newPrinter(factory).printElement(ctCatch);
		assertThat(out).contains("java.io.IOException | java.sql.SQLException");
	}

	@Test
	void resetClearsParenthesedState() throws Exception {
		Factory factory = new Launcher().getFactory();
		GosuPrettyPrinter printer = newPrinter(factory);
		java.lang.reflect.Field field = GosuPrettyPrinter.class.getDeclaredField("gosuParenthesed");
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		Deque<Object> deque = (Deque<Object>) field.get(printer);
		deque.push(factory.createLiteral(1));

		CtClass<?> cls = factory.createClass("demo.Clean");
		printer.printType(cls);

		assertThat(deque).isEmpty();
	}

	@Test
	void annotationParamAttributeKeyIsPrinted() {
		Factory factory = new Launcher().getFactory();
		CtAnnotation<Annotation> anno = factory.createAnnotation();
		anno.setAnnotationType(factory.Type().createReference("com.example.Query"));
		anno.addValue("param", factory.createLiteral("user_id"));
		anno.addValue("limit", factory.createLiteral(10));
		CtClass<?> cls = factory.createClass("com.example.MyService");
		cls.addAnnotation(anno);

		String out = newPrinter(factory).printType(cls);
		assertThat(out).contains("param = \"user_id\"");
	}

	@Test
	void forEachLoopVariableTypeIsPrinted() {
		Factory factory = new Launcher().getFactory();
		CtForEach fe = factory.Core().createForEach();
		CtLocalVariable<String> lv = factory.createLocalVariable();
		lv.setSimpleName("item");
		lv.setType(factory.Type().createReference("String"));
		fe.setVariable(lv);
		fe.setExpression(factory.Code().createCodeSnippetExpression("items"));
		fe.setBody(factory.Core().createBlock());

		String out = newPrinter(factory).printElement(fe);
		assertThat(out).contains("item : String");
	}

	@Test
	void localVariableHasNoTrailingSemicolon() {
		Factory factory = new Launcher().getFactory();
		CtLocalVariable<Integer> lv = factory.createLocalVariable();
		lv.setSimpleName("x");
		lv.setType(factory.Type().integerPrimitiveType());
		lv.setDefaultExpression(factory.createLiteral(1));

		String out = newPrinter(factory).printElement(lv);
		assertThat(out).doesNotContain(";");
	}

	@Test
	void constructorOmitsInvalidModifiers() {
		Factory factory = new Launcher().getFactory();
		CtConstructor<?> ctor = factory.createConstructor();
		ctor.addModifier(ModifierKind.FINAL);

		String out = newPrinter(factory).printElement(ctor);
		assertThat(out).doesNotContain("final");
		assertThat(out).contains("construct");
	}

	@Test
	void enumSuperInterfacesArePrinted() {
		Factory factory = new Launcher().getFactory();
		CtEnum<?> ctEnum = factory.createEnum();
		ctEnum.setSimpleName("Priority");
		ctEnum.addSuperInterface(factory.Type().createReference("java.lang.Comparable"));

		String out = newPrinter(factory).printElement(ctEnum);
		assertThat(out).contains(" : java.lang.Comparable");
	}

	@Test
	void parameterVarArgsArePrinted() {
		Factory factory = new Launcher().getFactory();
		CtParameter<String[]> param = factory.createParameter();
		param.setSimpleName("args");
		param.setType(factory.Type().createArrayReference(factory.Type().stringType()));
		param.setVarArgs(true);

		String out = newPrinter(factory).printElement(param);
		assertThat(out).contains("...");
		assertThat(out).doesNotContain("[]");
	}
}
