/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import spoon.compiler.Environment;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.TokenWriter;
import spoon.reflect.visitor.printer.CommentOffset;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A Spoon pretty-printer that emits Gosu instead of Java: types are
 * {@code class}/{@code enhancement}, fields are {@code var name : type},
 * functions are {@code function name(..) : type} and statements carry no
 * trailing semicolons.
 */
public class GosuPrettyPrinter extends DefaultJavaPrettyPrinter {

	private final Deque<CtExpression<?>> gosuParenthesed = new ArrayDeque<>();

	public GosuPrettyPrinter(Environment env) {
		super(env);
	}

	/** Prints a single top-level type (no compilation-unit package header). */
	public String printType(CtType<?> type) {
		reset();
		type.accept(this);
		return getResult();
	}

	/** Gosu has no statement-terminating semicolons. */
	@Override
	protected void exitCtStatement(CtStatement statement) {
		// no-op
	}

	@Override
	public <T> void visitCtClass(CtClass<T> type) {
		getContext().pushCurrentThis(type);
		if (!type.isImplicit()) {
			printAnnotations(type, true);
			TokenWriter w = getPrinterTokenWriter();
			boolean enhancement = isGosuEnhancement(type);
			w.writeKeyword(enhancement ? "enhancement" : "class");
			w.writeSpace();
			w.writeIdentifier(stripLeadingDigits(type.getSimpleName()));
			if (!type.getFormalCtTypeParameters().isEmpty()) {
				printFormalTypeParameters(type.getFormalCtTypeParameters());
			}

			CtTypeReference<?> superclass = enhancement
					? enhancedTypeOf(type)
					: type.getSuperclass();
			if (superclass != null && !"java.lang.Object".equals(superclass.getQualifiedName())) {
				w.writeSeparator(" : ");
				scan(superclass);
			}
			w.writeSpace();
			w.writeSeparator("{");
			w.incTab();
		}
		getElementPrinterHelper().writeElementList(type.getTypeMembers());
		if (!type.isImplicit()) {
			getPrinterTokenWriter().decTab();
			getPrinterTokenWriter().writeSeparator("}");
		}
		getContext().popCurrentThis();
	}

	@Override
	public <T> void visitCtInterface(CtInterface<T> intrface) {
		getContext().pushCurrentThis(intrface);
		printAnnotations(intrface, true);
		TokenWriter w = getPrinterTokenWriter();
		boolean structure = isGosuStructure(intrface);
		w.writeKeyword(structure ? "structure" : "interface");
		w.writeSpace();
		w.writeIdentifier(stripLeadingDigits(intrface.getSimpleName()));
		if (!intrface.getFormalCtTypeParameters().isEmpty()) {
			printFormalTypeParameters(intrface.getFormalCtTypeParameters());
		}

		if (!intrface.getSuperInterfaces().isEmpty()) {
			w.writeSeparator(" : ");
			printCommaList(new java.util.ArrayList<>(intrface.getSuperInterfaces()));
		}
		w.writeSpace();
		w.writeSeparator("{");
		w.incTab();
		getElementPrinterHelper().writeElementList(intrface.getTypeMembers());
		getPrinterTokenWriter().decTab();
		getPrinterTokenWriter().writeSeparator("}");
		getContext().popCurrentThis();
	}

	@Override
	public <T extends Enum<?>> void visitCtEnum(CtEnum<T> ctEnum) {
		getContext().pushCurrentThis(ctEnum);
		printAnnotations(ctEnum, true);
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("enum");
		w.writeSpace();
		w.writeIdentifier(stripLeadingDigits(ctEnum.getSimpleName()));
		w.writeSpace();
		w.writeSeparator("{");
		w.incTab();
		List<CtEnumValue<?>> values = ctEnum.getEnumValues();
		for (int i = 0; i < values.size(); i++) {
			w.writeln();
			scan(values.get(i));
			if (i < values.size() - 1) {
				w.writeSeparator(",");
			}
		}
		getElementPrinterHelper().writeElementList(ctEnum.getTypeMembers());
		w.decTab();
		w.writeln();
		w.writeSeparator("}");
		getContext().popCurrentThis();
	}

	@Override
	public <T> void visitCtEnumValue(CtEnumValue<T> enumValue) {
		getPrinterTokenWriter().writeIdentifier(enumValue.getSimpleName());
	}

	@Override
	public <A extends Annotation> void visitCtAnnotationType(CtAnnotationType<A> annotationType) {
		getContext().pushCurrentThis(annotationType);
		TokenWriter w = getPrinterTokenWriter();
		printAnnotations(annotationType, true);
		w.writeKeyword("annotation");
		w.writeSpace();
		w.writeIdentifier(stripLeadingDigits(annotationType.getSimpleName()));
		w.writeSpace();
		w.writeSeparator("{");
		w.incTab();
		getElementPrinterHelper().writeElementList(annotationType.getTypeMembers());
		w.decTab();
		w.writeSeparator("}");
		getContext().popCurrentThis();
	}

	@Override
	public <T> void visitCtField(CtField<T> field) {
		printAnnotations(field, true);
		TokenWriter w = getPrinterTokenWriter();
		writeVisibility(field);
		if (field.hasModifier(ModifierKind.ABSTRACT)) {
			w.writeKeyword("abstract");
			w.writeSpace();
		}
		if (field.isStatic()) {
			w.writeKeyword("static");
			w.writeSpace();
		}
		if (field.hasModifier(ModifierKind.FINAL)) {
			w.writeKeyword("final");
			w.writeSpace();
		}
		w.writeKeyword("var");
		w.writeSpace();
		w.writeIdentifier(field.getSimpleName());
		w.writeSeparator(" : ");
		scan(field.getType());
		if (field.getDefaultExpression() != null) {
			w.writeSpace();
			w.writeOperator("=");
			w.writeSpace();
			scan(field.getDefaultExpression());
		}
	}

	@Override
	public <T> void visitCtMethod(CtMethod<T> method) {
		printAnnotations(method, true);
		TokenWriter w = getPrinterTokenWriter();
		writeVisibility(method);
		if (method.hasModifier(ModifierKind.ABSTRACT)) {
			w.writeKeyword("abstract");
			w.writeSpace();
		}
		if (method.hasModifier(ModifierKind.STATIC)) {
			w.writeKeyword("static");
			w.writeSpace();
		}
		if (method.hasModifier(ModifierKind.FINAL)) {
			w.writeKeyword("final");
			w.writeSpace();
		}
		boolean propGet = isGosuPropertyGet(method);
		boolean propSet = isGosuPropertySet(method);
		if (propGet) {
			w.writeKeyword("property");
			w.writeSpace();
			w.writeKeyword("get");
		} else if (propSet) {
			w.writeKeyword("property");
			w.writeSpace();
			w.writeKeyword("set");
		} else {
			w.writeKeyword("function");
		}
		w.writeSpace();
		w.writeIdentifier(stripLeadingDigits(method.getSimpleName()));
		if (!method.getFormalCtTypeParameters().isEmpty()) {
			printFormalTypeParameters(method.getFormalCtTypeParameters());
		}
		w.writeSeparator("(");
		printCommaList(method.getParameters());
		w.writeSeparator(")");
		if (!isVoid(method.getType())) {
			w.writeSeparator(" : ");
			scan(method.getType());
		}
		if (method.getBody() != null) {
			w.writeSpace();
			scan(method.getBody());
		}
	}

	@Override
	public <T> void visitCtConstructor(CtConstructor<T> constructor) {
		printAnnotations(constructor, true);
		TokenWriter w = getPrinterTokenWriter();
		writeVisibility(constructor);
		if (constructor.hasModifier(ModifierKind.ABSTRACT)) {
			w.writeKeyword("abstract");
			w.writeSpace();
		}
		if (constructor.hasModifier(ModifierKind.STATIC)) {
			w.writeKeyword("static");
			w.writeSpace();
		}
		if (constructor.hasModifier(ModifierKind.FINAL)) {
			w.writeKeyword("final");
			w.writeSpace();
		}
		w.writeKeyword("construct");
		w.writeSeparator("(");
		printCommaList(constructor.getParameters());
		w.writeSeparator(")");
		w.writeSpace();
		scan(constructor.getBody());
	}

	@Override
	public <T> void visitCtParameter(CtParameter<T> parameter) {
		printAnnotations(parameter, false);
		getPrinterTokenWriter().writeIdentifier(parameter.getSimpleName());
		getPrinterTokenWriter().writeSeparator(" : ");
		scan(parameter.getType());
	}

	@Override
	public void visitCtForEach(CtForEach foreach) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("for");
		w.writeSpace();
		w.writeSeparator("(");
		CtLocalVariable<?> variable = foreach.getVariable();
		if (variable != null) {
			w.writeIdentifier(variable.getSimpleName());
		}
		w.writeSpace();
		w.writeKeyword("in");
		w.writeSpace();
		if (foreach.getExpression() != null) {
			scan(foreach.getExpression());
		}
		w.writeSeparator(")");
		w.writeSpace();
		scan(foreach.getBody());
	}

	@Override
	public <T> void visitCtLocalVariable(CtLocalVariable<T> localVariable) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("var");
		w.writeSpace();
		w.writeIdentifier(localVariable.getSimpleName());
		if (localVariable.getType() != null) {
			w.writeSpace();
			w.writeSeparator(":");
			w.writeSpace();
			scan(localVariable.getType());
		}
		if (localVariable.getDefaultExpression() != null) {
			w.writeSpace();
			w.writeOperator("=");
			w.writeSpace();
			scan(localVariable.getDefaultExpression());
		}
		w.writeSeparator(";");
	}

	@Override
	public <T> void visitCtConstructorCall(CtConstructorCall<T> constructorCall) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("new");
		w.writeSpace();
		scan(constructorCall.getType());
		w.writeSeparator("(");
		java.util.List<CtExpression<?>> args = constructorCall.getArguments();
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) {
				w.writeSeparator(",");
			}
			w.writeSpace();
			scan(args.get(i));
		}
		if (!args.isEmpty()) {
			w.writeSpace();
		}
		w.writeSeparator(")");
	}

	@Override
	public <T> void visitCtNewArray(CtNewArray<T> newArray) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeSeparator("{");
		boolean first = true;
		for (CtExpression<?> element : newArray.getElements()) {
			if (!first) {
				w.writeSeparator(",");
			}
			w.writeSpace();
			scan(element);
			first = false;
		}
		if (!newArray.getElements().isEmpty()) {
			w.writeSpace();
		}
		w.writeSeparator("}");
	}

	/** Gosu catch clauses bind in {code name : type} order. */
	@Override
	public void visitCtCatch(CtCatch ctCatch) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("catch");
		w.writeSpace();
		w.writeSeparator("(");
		CtCatchVariable<?> parameter = ctCatch.getParameter();
		if (parameter != null) {
			w.writeIdentifier(parameter.getSimpleName());
			w.writeSeparator(" : ");
			if (parameter.getType() != null) {
				scan(parameter.getType());
			}
		}
		w.writeSeparator(")");
		w.writeSpace();
		scan(ctCatch.getBody());
	}

	@Override
	public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
		enterCtStatement(tryWithResource);
		TokenWriter w = getPrinterTokenWriter();
		w.writeKeyword("using");
		w.writeSpace();
		w.writeSeparator("(");
		for (int i = 0; i < tryWithResource.getResources().size(); i++) {
			printResource(tryWithResource.getResources().get(i));
			if (i < tryWithResource.getResources().size() - 1) {
				w.writeSeparator(", ");
			}
		}
		w.writeSeparator(")");
		w.writeSpace();
		scan(tryWithResource.getBody());
		exitCtStatement(tryWithResource);
	}

	private void printResource(CtElement res) {
		if (res instanceof CtLocalVariable<?>) {
			CtLocalVariable<?> lv = (CtLocalVariable<?>) res;
			if (lv.getSimpleName() == null || lv.getSimpleName().isEmpty() || lv.isImplicit()) {
				scan(lv.getDefaultExpression());
				return;
			}
			TokenWriter w = getPrinterTokenWriter();
			w.writeKeyword("var");
			w.writeSpace();
			w.writeIdentifier(lv.getSimpleName());
			if (lv.getType() != null) {
				w.writeSpace();
				w.writeSeparator(":");
				w.writeSpace();
				scan(lv.getType());
			}
			if (lv.getDefaultExpression() != null) {
				w.writeSpace();
				w.writeOperator("=");
				w.writeSpace();
				scan(lv.getDefaultExpression());
			}
		} else {
			scan(res);
		}
	}

	@Override
	public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
		if (operator.getKind() == BinaryOperatorKind.INSTANCEOF) {
			enterCtExpression(operator);
			scan(operator.getLeftHandOperand());
			TokenWriter w = getPrinterTokenWriter();
			w.writeSpace();
			w.writeKeyword("typeis");
			w.writeSpace();
			scan(operator.getRightHandOperand());
			exitCtExpression(operator);
			return;
		}
		super.visitCtBinaryOperator(operator);
	}

	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		if (invocation.getTarget() == null && invocation.getExecutable() != null) {
			String name = invocation.getExecutable().getSimpleName();
			if ("typeof".equals(name) && invocation.getArguments().size() == 1) {
				enterCtStatement(invocation);
				enterCtExpression(invocation);
				TokenWriter w = getPrinterTokenWriter();
				w.writeKeyword("typeof");
				w.writeSpace();
				scan(invocation.getArguments().get(0));
				exitCtExpression(invocation);
				exitCtStatement(invocation);
				return;
			}
		}
		super.visitCtInvocation(invocation);
	}

	@Override
	protected void enterCtExpression(CtExpression<?> e) {
		if (e.getTypeCasts().isEmpty()) {
			super.enterCtExpression(e);
			return;
		}
		if (!(e instanceof CtStatement)) {
			getElementPrinterHelper().writeComment(e, CommentOffset.BEFORE);
		}
		getPrinterTokenWriter().getPrinterHelper().mapLine(e, sourceCompilationUnit);
		gosuParenthesed.push(e);
		getPrinterTokenWriter().writeSeparator("(");
	}

	@Override
	protected void exitCtExpression(CtExpression<?> e) {
		if (e.getTypeCasts().isEmpty()) {
			super.exitCtExpression(e);
			return;
		}
		TokenWriter w = getPrinterTokenWriter();
		for (CtTypeReference<?> cast : e.getTypeCasts()) {
			w.writeSpace();
			w.writeKeyword("as");
			w.writeSpace();
			scan(cast);
		}
		if (!gosuParenthesed.isEmpty() && e == gosuParenthesed.peek()) {
			gosuParenthesed.pop();
			w.writeSeparator(")");
		}
		if (!(e instanceof CtStatement)) {
			getElementPrinterHelper().writeComment(e, CommentOffset.AFTER);
		}
	}

	/** Gosu blocks are written {@code \ x : int -> expr} (not Java lambdas). */
	@Override
	public <T> void visitCtLambda(CtLambda<T> lambda) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeSeparator("\\");
		w.writeSpace();
		List<CtParameter<?>> params = lambda.getParameters();
		for (int i = 0; i < params.size(); i++) {
			scan(params.get(i));
			if (i < params.size() - 1) {
				w.writeSeparator(",");
				w.writeSpace();
			}
		}
		w.writeSpace();
		w.writeOperator("->");
		w.writeSpace();
		if (lambda.getExpression() != null) {
			scan(lambda.getExpression());
		}
	}

	/** Gosu case labels are {@code case <expr>:} or {@code default:}. */
	@Override
	public <S> void visitCtCase(CtCase<S> ctCase) {
		TokenWriter w = getPrinterTokenWriter();
		List<CtExpression<S>> exprs = ctCase.getCaseExpressions();
		if (exprs != null && !exprs.isEmpty()) {
			w.writeKeyword("case");
			w.writeSpace();
			for (int i = 0; i < exprs.size(); i++) {
				if (i > 0) {
					w.writeSeparator(",");
					w.writeSpace();
				}
				scan(exprs.get(i));
			}
			w.writeSeparator(":");
		} else {
			w.writeKeyword("default");
			w.writeSeparator(":");
		}
		w.incTab();
		for (CtStatement statement : ctCase.getStatements()) {
			w.writeln();
			scan(statement);
		}
		w.decTab();
	}

	@Override
	public void visitCtTypeParameter(CtTypeParameter typeParameter) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeIdentifier(typeParameter.getSimpleName());
		CtTypeReference<?> superclass = typeParameter.getSuperclass();
		if (superclass != null && !"java.lang.Object".equals(superclass.getQualifiedName())) {
			w.writeSpace();
			w.writeKeyword("extends");
			w.writeSpace();
			scan(superclass);
		}
	}

	private void printFormalTypeParameters(List<CtTypeParameter> typeParameters) {
		TokenWriter w = getPrinterTokenWriter();
		w.writeSeparator("<");
		for (int i = 0; i < typeParameters.size(); i++) {
			scan(typeParameters.get(i));
			if (i < typeParameters.size() - 1) {
				w.writeSeparator(", ");
			}
		}
		w.writeSeparator(">");
	}

	@Override
	public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
		if (isInternalMarker(annotation)) {
			return;
		}
		TokenWriter w = getPrinterTokenWriter();
		w.writeSeparator("@");
		scan(annotation.getAnnotationType());
		if (!annotation.getValues().isEmpty()) {
			w.writeSeparator("(");
			if (annotation.getValues().size() == 1 && annotation.getValues().containsKey("value")) {
				scan(annotation.getValues().get("value"));
			} else {
				int i = 0;
				for (Map.Entry<String, ?> entry : annotation.getValues().entrySet()) {
					spoon.reflect.code.CtExpression<?> val =
							(spoon.reflect.code.CtExpression<?>) entry.getValue();
					if (entry.getKey().startsWith("param")) {
						scan(val);
					} else {
						w.writeIdentifier(entry.getKey());
						w.writeSpace();
						w.writeOperator("=");
						w.writeSpace();
						scan(val);
					}
					if (i < annotation.getValues().size() - 1) {
						w.writeSeparator(", ");
					}
					i++;
				}
			}
			w.writeSeparator(")");
		}
	}

	private void printAnnotations(CtElement element, boolean newline) {
		if (element == null || element.getAnnotations().isEmpty()) {
			return;
		}
		TokenWriter w = getPrinterTokenWriter();
		for (CtAnnotation<?> anno : element.getAnnotations()) {
			if (!isInternalMarker(anno)) {
				scan(anno);
				if (newline) {
					w.writeln();
				} else {
					w.writeSpace();
				}
			}
		}
	}

	private static boolean isInternalMarker(CtAnnotation<?> annotation) {
		if (annotation == null || annotation.getAnnotationType() == null) {
			return false;
		}
		String name = annotation.getAnnotationType().getQualifiedName();
		return GosuModelBuilder.GOSU_KIND_ANNOTATION.equals(name)
				|| GosuModelBuilder.GOSU_ENHANCED_TYPE_ANNOTATION.equals(name);
	}

	private void writeVisibility(spoon.reflect.declaration.CtModifiable modifiable) {
		TokenWriter w = getPrinterTokenWriter();
		if (modifiable.hasModifier(ModifierKind.PUBLIC)) {
			w.writeKeyword("public");
			w.writeSpace();
		} else if (modifiable.hasModifier(ModifierKind.PRIVATE)) {
			w.writeKeyword("private");
			w.writeSpace();
		} else if (modifiable.hasModifier(ModifierKind.PROTECTED)) {
			w.writeKeyword("protected");
			w.writeSpace();
		}
	}

	private void printCommaList(List<? extends CtElement> elements) {
		TokenWriter w = getPrinterTokenWriter();
		for (int i = 0; i < elements.size(); i++) {
			scan(elements.get(i));
			if (i < elements.size() - 1) {
				w.writeSeparator(", ");
			}
		}
	}

	/** True when the Ct type carries the Gosu enhancement marker annotation. */
	public static boolean isGosuEnhancement(CtType<?> type) {
		return GosuModelBuilder.GOSU_KIND_ENHANCEMENT.equals(getGosuKind(type));
	}

	/** True when the Ct type carries the Gosu structure marker annotation. */
	public static boolean isGosuStructure(CtType<?> type) {
		return GosuModelBuilder.GOSU_KIND_STRUCTURE.equals(getGosuKind(type));
	}

	/** True when the method is a Gosu property getter. */
	public static boolean isGosuPropertyGet(CtMethod<?> method) {
		return GosuModelBuilder.GOSU_KIND_PROPERTY_GET.equals(getGosuKind(method));
	}

	/** True when the method is a Gosu property setter. */
	public static boolean isGosuPropertySet(CtMethod<?> method) {
		return GosuModelBuilder.GOSU_KIND_PROPERTY_SET.equals(getGosuKind(method));
	}

	/** Tags a Ct class as a Gosu enhancement on the given target type. */
	public static void tagEnhancement(CtClass<?> type, String enhancedType) {
		tagGosuKind(type, GosuModelBuilder.GOSU_KIND_ENHANCEMENT);
		CtTypeReference<Annotation> ref =
				type.getFactory().Type().createReference(GosuModelBuilder.GOSU_ENHANCED_TYPE_ANNOTATION);
		CtAnnotation<Annotation> marker = type.getFactory().createAnnotation(ref);
		marker.addValue("value", enhancedType);
		type.addAnnotation(marker);
	}

	/** Tags a Ct interface as a Gosu structure. */
	public static void tagStructure(CtInterface<?> type) {
		tagGosuKind(type, GosuModelBuilder.GOSU_KIND_STRUCTURE);
	}

	/** Tags a Ct method as a Gosu property getter. */
	public static void tagPropertyGet(CtMethod<?> method) {
		tagGosuKind(method, GosuModelBuilder.GOSU_KIND_PROPERTY_GET);
	}

	/** Tags a Ct method as a Gosu property setter. */
	public static void tagPropertySet(CtMethod<?> method) {
		tagGosuKind(method, GosuModelBuilder.GOSU_KIND_PROPERTY_SET);
	}

	/** Tags an arbitrary Ct element with a GosuKind marker annotation. */
	public static void tagGosuKind(CtElement element, String kind) {
		CtTypeReference<Annotation> ref =
				element.getFactory().Type().createReference(GosuModelBuilder.GOSU_KIND_ANNOTATION);
		CtAnnotation<Annotation> marker = element.getFactory().createAnnotation(ref);
		marker.addValue("value", kind);
		element.addAnnotation(marker);
	}

	private static String getGosuKind(CtElement element) {
		if (element == null) {
			return null;
		}
		for (CtAnnotation<?> annotation : element.getAnnotations()) {
			if (GosuModelBuilder.GOSU_KIND_ANNOTATION.equals(
					annotation.getAnnotationType().getQualifiedName())) {
				Object value = annotation.getValue("value");
				if (value instanceof spoon.reflect.code.CtLiteral<?>) {
					value = ((spoon.reflect.code.CtLiteral<?>) value).getValue();
				}
				return String.valueOf(value);
			}
		}
		return null;
	}

	/** The enhanced type reference of an enhancement Ct type, or null. */
	public static CtTypeReference<?> enhancedTypeOf(CtType<?> type) {
		if (type == null) {
			return null;
		}
		for (CtAnnotation<?> annotation : type.getAnnotations()) {
			if (GosuModelBuilder.GOSU_ENHANCED_TYPE_ANNOTATION.equals(
					annotation.getAnnotationType().getQualifiedName())) {
				Object value = annotation.getValue("value");
				if (value instanceof spoon.reflect.code.CtLiteral<?>) {
					value = ((spoon.reflect.code.CtLiteral<?>) value).getValue();
				}
				if (value != null) {
					return type.getFactory().Type().createReference(String.valueOf(value));
				}
			}
		}
		return null;
	}

	private static boolean isVoid(CtTypeReference<?> type) {
		return type != null && "void".equals(type.getQualifiedName());
	}
}
