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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GosuForEachTypeTest {

	@TempDir
	Path tempDir;

	private CtType<?> buildSingleType(String simpleName, String gosuSource) throws Exception {
		Path srcDir = tempDir.resolve("src-" + simpleName);
		Path pkgDir = srcDir.resolve("demo");
		Files.createDirectories(pkgDir);
		Files.writeString(pkgDir.resolve(simpleName + ".gs"), gosuSource, StandardCharsets.UTF_8);

		GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(srcDir.toFile()));
		Factory factory = new Launcher().getFactory();
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> types = builder.buildAll(srcDir.toFile());

		CtType<?> found = null;
		for (CtType<?> t : types) {
			String pkg = t.getPackage() == null ? null : t.getPackage().getQualifiedName();
			String fqn = pkg == null ? t.getSimpleName() : pkg + "." + t.getSimpleName();
			if (("demo." + simpleName).equals(fqn)) {
				found = t;
			}
		}
		assertThat(found).isNotNull();
		return found;
	}

	// Note: Gosu has no explicit loop variable type syntax (both
	// `for (item : String in items)` and `for (String item in items)` are
	// parse issues), so the builder always marks inferred loop types
	// implicit. Explicit-type printing is covered by
	// GosuPrettyPrinterIssuesTest#forEachLoopVariableTypeIsPrinted on a
	// hand-built model.

	@Test
	void inferredLoopVariableTypeIsOmitted() throws Exception {
		CtType<?> demo = buildSingleType("InferredForDemo",
				"package demo\nuses java.util.List\nclass InferredForDemo {\n"
				+ "  function test(items : List<String>) : void {\n"
				+ "    for (item in items) {\n"
				+ "      print(item)\n"
				+ "    }\n"
				+ "  }\n}\n");
		String text = new GosuPrettyPrinter(demo.getFactory().getEnvironment()).printType(demo);
		assertThat(text).contains("for (item in items) {");
	}
}
