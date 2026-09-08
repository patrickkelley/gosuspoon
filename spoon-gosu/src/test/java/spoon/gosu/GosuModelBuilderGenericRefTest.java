/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spoon.Launcher;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtTypeReference;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GosuModelBuilderGenericRefTest {

	@TempDir
	Path tempDir;

	private static CtTypeReference<?> invokeGenericRef(Factory factory, String name) {
		return GosuModelBuilder.createGenericRef(factory, name);
	}

	private static Factory newFactory() {
		return new Launcher().getFactory();
	}

	@Test
	void nullMapsToObject() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, null);
		assertThat(ref.getQualifiedName()).isEqualTo("java.lang.Object");
	}

	@Test
	void simpleTypesAreNotArrays() throws Exception {
		Factory factory = newFactory();
		assertThat(invokeGenericRef(factory, "String")).isNotInstanceOf(CtArrayTypeReference.class);
		assertThat(invokeGenericRef(factory, "java.lang.String").getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(invokeGenericRef(factory, "int").getQualifiedName()).isEqualTo("int");
	}

	@Test
	void nonGenericArraysStillWork() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> single = invokeGenericRef(factory, "String[]");
		assertThat(single).isInstanceOf(CtArrayTypeReference.class);
		assertThat(((CtArrayTypeReference<?>) single).getComponentType().getSimpleName()).isEqualTo("String");

		CtTypeReference<?> multi = invokeGenericRef(factory, "String[][]");
		assertThat(multi).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> outer = ((CtArrayTypeReference<?>) multi).getComponentType();
		assertThat(outer).isInstanceOf(CtArrayTypeReference.class);
		assertThat(((CtArrayTypeReference<?>) outer).getComponentType().getSimpleName()).isEqualTo("String");
	}

	@Test
	void simpleGenericsAreNotArrays() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> list = invokeGenericRef(factory, "List<String>");
		assertThat(list).isNotInstanceOf(CtArrayTypeReference.class);
		assertThat(list.getSimpleName()).isEqualTo("List");
		assertThat(list.getActualTypeArguments()).hasSize(1);

		CtTypeReference<?> map = invokeGenericRef(factory, "Map<String, List<Integer>>");
		assertThat(map).isNotInstanceOf(CtArrayTypeReference.class);
		assertThat(map.getSimpleName()).isEqualTo("Map");
		assertThat(map.getActualTypeArguments()).hasSize(2);
	}

	@Test
	void genericArrayKeepsSingleDimension() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, "List<String>[]");
		assertThat(ref).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> component = ((CtArrayTypeReference<?>) ref).getComponentType();
		assertThat(component.getSimpleName()).isEqualTo("List");
		assertThat(component.getActualTypeArguments()).hasSize(1);
		assertThat(component.getActualTypeArguments().get(0).getSimpleName()).isEqualTo("String");
	}

	@Test
	void genericArrayKeepsMultipleDimensions() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, "List<String>[][]");
		assertThat(ref).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> level1 = ((CtArrayTypeReference<?>) ref).getComponentType();
		assertThat(level1).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> component = ((CtArrayTypeReference<?>) level1).getComponentType();
		assertThat(component.getSimpleName()).isEqualTo("List");
		assertThat(component.getActualTypeArguments()).hasSize(1);
	}

	@Test
	void genericArrayWithSpaceStillCountsAsArray() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, "List<String> []");
		assertThat(ref).isInstanceOf(CtArrayTypeReference.class);
	}

	@Test
	void nestedGenericArrayKeepsOuterDimension() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, "Map<String, List<Integer>>[]");
		assertThat(ref).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> component = ((CtArrayTypeReference<?>) ref).getComponentType();
		assertThat(component.getSimpleName()).isEqualTo("Map");
		assertThat(component.getActualTypeArguments()).hasSize(2);
	}

	@Test
	void arrayTypeArgumentIsPreserved() throws Exception {
		Factory factory = newFactory();
		CtTypeReference<?> ref = invokeGenericRef(factory, "List<String[]>");
		assertThat(ref).isNotInstanceOf(CtArrayTypeReference.class);
		assertThat(ref.getActualTypeArguments()).hasSize(1);
		assertThat(ref.getActualTypeArguments().get(0)).isInstanceOf(CtArrayTypeReference.class);
	}

	@Test
	void genericArrayFieldIsModeledAsArrayType() throws Exception {
		Path srcDir = tempDir.resolve("src");
		Path pkgDir = srcDir.resolve("demo");
		Files.createDirectories(pkgDir);
		Files.writeString(pkgDir.resolve("GenericArrayDemo.gs"),
				"package demo\nuses java.util.List\nclass GenericArrayDemo {\n  var _items : List<String>[]\n}\n",
				StandardCharsets.UTF_8);

		GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(srcDir.toFile()));
		Factory factory = newFactory();
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> types = builder.buildAll(srcDir.toFile());

		CtType<?> demo = null;
		for (CtType<?> t : types) {
			String pkg = t.getPackage() == null ? null : t.getPackage().getQualifiedName();
			String fqn = pkg == null ? t.getSimpleName() : pkg + "." + t.getSimpleName();
			if ("demo.GenericArrayDemo".equals(fqn)) {
				demo = t;
			}
		}
		assertThat(demo).isNotNull();
		CtTypeReference<?> fieldType = demo.getField("_items").getType();
		assertThat(fieldType).isInstanceOf(CtArrayTypeReference.class);
		CtTypeReference<?> component = ((CtArrayTypeReference<?>) fieldType).getComponentType();
		assertThat(component.getSimpleName()).isEqualTo("List");
		assertThat(component.getActualTypeArguments()).hasSize(1);

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(demo);
		assertThat(text).contains("List<String>[]");
	}
}
