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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GosuEnvironmentTest {

	@TempDir
	Path tempDir;

	@Test
	void testEnvironmentPropertiesAndLifecycle() throws Exception {
		Path srcPath = tempDir.resolve("src");
		Files.createDirectories(srcPath);

		Path pkgDir = srcPath.resolve("sample/env");
		Files.createDirectories(pkgDir);
		Files.writeString(pkgDir.resolve("EnvSample.gs"), "package sample.env\nclass EnvSample {\n}\n", StandardCharsets.UTF_8);

		try (GosuEnvironment env = GosuEnvironment.initialize(Collections.singletonList(srcPath.toFile()))) {
			assertThat(GosuEnvironment.isInitialized()).isTrue();
			assertThat(env.executionEnvironment()).isNotNull();
			assertThat(env.sourceDirs()).containsExactly(srcPath.toFile());
			assertThat(env.classTypeLoader()).isNotNull();
			assertThat(env.loadType("sample.env.EnvSample")).isNotNull();
		}
	}

	@Test
	void testScanTypeNamesWithPackageAndFallback() throws Exception {
		Path srcPath = tempDir.resolve("scan-src");
		Files.createDirectories(srcPath);

		Path subPkg = srcPath.resolve("org/demo");
		Files.createDirectories(subPkg);
		Files.writeString(subPkg.resolve("First.gs"), "package org.demo\nclass First {}\n", StandardCharsets.UTF_8);
		Files.writeString(subPkg.resolve("Enhance.gsx"), "package org.demo\nenhancement Enhance : String {}\n", StandardCharsets.UTF_8);

		// File without explicit package statement
		Path noPkgDir = srcPath.resolve("raw/data");
		Files.createDirectories(noPkgDir);
		Files.writeString(noPkgDir.resolve("Bare.gs"), "class Bare {}\n", StandardCharsets.UTF_8);

		// Non-Gosu files
		Files.writeString(subPkg.resolve("ignored.txt"), "ignored", StandardCharsets.UTF_8);
		Files.writeString(subPkg.resolve("Ignored.java"), "class Ignored {}", StandardCharsets.UTF_8);

		GosuEnvironment env = GosuEnvironment.initialize(Collections.singletonList(srcPath.toFile()));
		List<String> names = env.scanTypeNames(srcPath.toFile());

		assertThat(names).containsExactlyInAnyOrder(
				"org.demo.First",
				"org.demo.Enhance",
				"raw.data.Bare"
		);
	}

	@Test
	void testScanTypeNamesOnNonDirectory() throws Exception {
		Path filePath = tempDir.resolve("dummy.txt");
		Files.writeString(filePath, "dummy", StandardCharsets.UTF_8);

		GosuEnvironment env = GosuEnvironment.initialize(Collections.singletonList(tempDir.toFile()));
		List<String> names = env.scanTypeNames(filePath.toFile());

		assertThat(names).isEmpty();
	}
}
