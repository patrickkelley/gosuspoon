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
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GosuTransformationTest {

	@Test
	@SuppressWarnings("unchecked")
	void synthesizeClassFromScratch() throws Exception {
		Factory factory = new Launcher().getFactory();

		// Create class
		CtClass<Object> ctClass = factory.createClass("synth.Calculator");
		ctClass.setModifiers(Set.of(ModifierKind.PUBLIC));

		// Add field: var _base : int = 100
		CtField<Integer> field = factory.createField();
		field.setSimpleName("_base");
		field.setType(factory.Type().integerPrimitiveType());
		field.setDefaultExpression(factory.Code().createLiteral(100));
		ctClass.addField(field);

		// Add constructor: construct(initial : int) { this._base = initial }
		CtConstructor<Object> ctor = factory.createConstructor();
		CtParameter<Integer> pInitial = factory.createParameter();
		pInitial.setSimpleName("initial");
		pInitial.setType(factory.Type().integerPrimitiveType());
		ctor.addParameter(pInitial);

		CtBlock<Void> ctorBody = factory.createBlock();
		ctorBody.addStatement(factory.createCodeSnippetStatement("this._base = initial"));
		ctor.setBody(ctorBody);
		ctClass.addConstructor(ctor);

		// Add method: function add(x : int) : int { return this._base + x }
		CtMethod<Integer> addMethod = factory.createMethod();
		addMethod.setSimpleName("add");
		addMethod.setType(factory.Type().integerPrimitiveType());
		CtParameter<Integer> pX = factory.createParameter();
		pX.setSimpleName("x");
		pX.setType(factory.Type().integerPrimitiveType());
		addMethod.addParameter(pX);

		CtBlock<Integer> methodBody = factory.createBlock();
		CtReturn<Integer> ret = factory.createReturn();
		CtBinaryOperator<Integer> plus = factory.createBinaryOperator();
		plus.setKind(BinaryOperatorKind.PLUS);
		plus.setLeftHandOperand(factory.createCodeSnippetExpression("this._base"));
		plus.setRightHandOperand(factory.Code().createVariableRead(pX.getReference(), false));
		ret.setReturnedExpression(plus);
		methodBody.addStatement(ret);
		addMethod.setBody(methodBody);
		ctClass.addMethod(addMethod);

		// Add property getter and setter
		CtMethod<Integer> getScore = factory.createMethod();
		getScore.setSimpleName("Score");
		getScore.setType(factory.Type().integerPrimitiveType());
		GosuPrettyPrinter.tagPropertyGet(getScore);
		CtBlock<Integer> getBody = factory.createBlock();
		CtReturn<Integer> getRet = factory.createReturn();
		getRet.setReturnedExpression(factory.createCodeSnippetExpression("this._base * 2"));
		getBody.addStatement(getRet);
		getScore.setBody(getBody);
		ctClass.addMethod(getScore);

		CtMethod<Void> setScore = factory.createMethod();
		setScore.setSimpleName("Score");
		setScore.setType(factory.Type().voidPrimitiveType());
		GosuPrettyPrinter.tagPropertySet(setScore);
		CtParameter<Integer> pVal = factory.createParameter();
		pVal.setSimpleName("val");
		pVal.setType(factory.Type().integerPrimitiveType());
		setScore.addParameter(pVal);
		CtBlock<Void> setBody = factory.createBlock();
		setBody.addStatement(factory.createCodeSnippetStatement("this._base = val / 2"));
		setScore.setBody(setBody);
		ctClass.addMethod(setScore);

		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
		String code = printer.printType(ctClass);

		assertThat(code)
				.contains("class Calculator {")
				.contains("var _base : int = 100")
				.contains("construct(initial : int) {")
				.contains("function add(x : int) : int {")
				.contains("property get Score() : int {")
				.contains("property set Score(val : int) {");

		// Verify round-trip compilation with Gosu
		File tmpDir = new File("target/test-synth-gsrc");
		deleteRecursively(tmpDir);
		File pkgDir = new File(tmpDir, "synth");
		pkgDir.mkdirs();
		Files.write(new File(pkgDir, "Calculator.gs").toPath(),
				("package synth\n\n" + code + "\n").getBytes(StandardCharsets.UTF_8));

		GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(tmpDir));
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> parsed = builder.buildAll(tmpDir);
		assertThat(parsed).hasSize(1);
		assertThat(parsed.get(0).getSimpleName()).isEqualTo("Calculator");
	}

	@Test
	@SuppressWarnings("unchecked")
	void synthesizeEnhancementAndStructureFromScratch() throws Exception {
		Factory factory = new Launcher().getFactory();

		// Create Enhancement
		CtClass<Object> enh = factory.createClass("synth.StringExt");
		GosuPrettyPrinter.tagEnhancement(enh, "java.lang.String");
		CtMethod<String> doubleMethod = factory.createMethod();
		doubleMethod.setSimpleName("doubleUp");
		doubleMethod.setType(factory.Type().createReference("String"));
		CtBlock<String> dBody = factory.createBlock();
		CtReturn<String> dRet = factory.createReturn();
		dRet.setReturnedExpression(factory.createCodeSnippetExpression("this + this"));
		dBody.addStatement(dRet);
		doubleMethod.setBody(dBody);
		enh.addMethod(doubleMethod);

		// Create Structure
		CtInterface<Object> struct = factory.createInterface("synth.Renderable");
		GosuPrettyPrinter.tagStructure(struct);
		CtMethod<Void> renderMethod = factory.createMethod();
		renderMethod.setSimpleName("render");
		renderMethod.setType(factory.Type().voidPrimitiveType());
		struct.addMethod(renderMethod);

		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
		String enhCode = printer.printType(enh);
		String structCode = printer.printType(struct);

		assertThat(enhCode)
				.contains("enhancement StringExt : java.lang.String {")
				.contains("function doubleUp() : String {");

		assertThat(structCode)
				.contains("structure Renderable {")
				.contains("function render()");

		File tmpDir = new File("target/test-synth2-gsrc");
		deleteRecursively(tmpDir);
		File pkgDir = new File(tmpDir, "synth");
		pkgDir.mkdirs();
		Files.write(new File(pkgDir, "StringExt.gsx").toPath(),
				("package synth\n\n" + enhCode + "\n").getBytes(StandardCharsets.UTF_8));
		Files.write(new File(pkgDir, "Renderable.gs").toPath(),
				("package synth\n\n" + structCode + "\n").getBytes(StandardCharsets.UTF_8));

		GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(tmpDir));
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> parsed = builder.buildAll(tmpDir);
		assertThat(parsed).hasSize(2);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void mutateExistingModelAndRoundTrip() throws Exception {
		File tmpDir = new File("target/test-mutate-gsrc");
		deleteRecursively(tmpDir);
		File pkgDir = new File(tmpDir, "mutate");
		pkgDir.mkdirs();
		Files.write(new File(pkgDir, "Service.gs").toPath(),
				("package mutate\n"
				+ "class Service {\n"
				+ "  function process(msg : String) : String {\n"
				+ "    return msg\n"
				+ "  }\n"
				+ "}\n").getBytes(StandardCharsets.UTF_8));

		Factory factory = new Launcher().getFactory();
		GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(tmpDir));
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> parsed = builder.buildAll(tmpDir);
		assertThat(parsed).hasSize(1);

		CtType<?> service = parsed.get(0);

		// Mutate: Add @Deprecated annotation
		CtAnnotation<Deprecated> dep = factory.createAnnotation();
		dep.setAnnotationType(factory.Type().createReference(Deprecated.class));
		service.addAnnotation(dep);

		// Mutate: Rename method and change body
		CtMethod<?> processMethod = service.getMethodsByName("process").get(0);
		processMethod.setSimpleName("processMessage");

		CtLocalVariable<String> prefixVar = factory.createLocalVariable();
		prefixVar.setSimpleName("prefix");
		prefixVar.setType(factory.Type().createReference("String"));
		prefixVar.setDefaultExpression(factory.Code().createLiteral("PROCESSED: "));
		processMethod.getBody().getStatements().add(0, prefixVar);

		CtReturn ret = (CtReturn) processMethod.getBody().getStatements().get(1);
		CtBinaryOperator concat = factory.createBinaryOperator();
		concat.setKind(BinaryOperatorKind.PLUS);
		concat.setLeftHandOperand(factory.Code().createVariableRead(prefixVar.getReference(), false));
		concat.setRightHandOperand((CtExpression) ret.getReturnedExpression());
		ret.setReturnedExpression(concat);

		// Print mutated class
		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
		String mutatedCode = printer.printType(service);

		assertThat(mutatedCode)
				.contains("@java.lang.Deprecated")
				.contains("class Service {")
				.contains("function processMessage(msg : String) : String {")
				.contains("var prefix : String = \"PROCESSED: \"")
				.contains("return prefix + msg");

		// Write mutated code and re-parse in fresh JVM/environment
		Files.write(new File(pkgDir, "Service.gs").toPath(),
				("package mutate\n\n" + mutatedCode + "\n").getBytes(StandardCharsets.UTF_8));

		GosuEnvironment gosu2 = GosuEnvironment.initialize(Collections.singletonList(tmpDir));
		GosuModelBuilder builder2 = new GosuModelBuilder(factory, gosu2);
		List<CtType<?>> reparsed = builder2.buildAll(tmpDir);
		assertThat(reparsed).hasSize(1);
		CtType<?> reparsedService = reparsed.get(0);
		assertThat(reparsedService.getMethodsByName("processMessage")).hasSize(1);
		assertThat(reparsedService.getAnnotations()).isNotEmpty();
	}

	private static void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File c : children) {
					deleteRecursively(c);
				}
			}
		}
		file.delete();
	}
}
