/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GosuLauncherTest {

	private static File srcDir;

	@BeforeAll
	static void setUp() throws Exception {
		srcDir = new File("target/test-launcher-src");
		deleteRecursively(srcDir);
		srcDir.mkdirs();

		File pkg = new File(srcDir, "launchdemo");
		pkg.mkdirs();
		Files.write(new File(pkg, "Sample.gs").toPath(),
				("package launchdemo\n"
				+ "uses java.util.List\n"
				+ "uses java.io.*\n"
				+ "class Sample {\n"
				+ "  var _name : String\n"
				+ "  construct(name : String) {\n"
				+ "    _name = name\n"
				+ "  }\n"
				+ "  function greet() : String {\n"
				+ "    return \"Hello \" + _name\n"
				+ "  }\n"
				+ "}\n").getBytes(StandardCharsets.UTF_8));

		Files.write(new File(pkg, "SampleEnhancement.gsx").toPath(),
				("package launchdemo\n"
				+ "enhancement SampleEnhancement : Sample {\n"
				+ "  function yell() : String {\n"
				+ "    return this.greet().toUpperCase()\n"
				+ "  }\n"
				+ "}\n").getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void testLauncherFileExport() throws Exception {
		File outDir = new File("target/test-launcher-out");
		deleteRecursively(outDir);

		GosuLauncher.main(new String[] { "-s", srcDir.getPath(), "-o", outDir.getPath() });

		File sampleFile = new File(outDir, "launchdemo/Sample.gs");
		File enhFile = new File(outDir, "launchdemo/SampleEnhancement.gsx");

		assertThat(sampleFile).exists();
		assertThat(enhFile).exists();

		String sampleContent = Files.readString(sampleFile.toPath());
		assertThat(sampleContent)
				.contains("package launchdemo")
				.contains("uses java.util.List")
				.contains("uses java.io.*")
				.contains("class Sample {")
				.contains("var _name : String")
				.contains("construct(name : String) {")
				.contains("function greet() : String {");

		String enhContent = Files.readString(enhFile.toPath());
		assertThat(enhContent)
				.contains("package launchdemo")
				.contains("enhancement SampleEnhancement : launchdemo.Sample {")
				.contains("function yell() : String {");
	}

	@Test
	void testLauncherStdout() throws Exception {
		PrintStream origOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
			GosuLauncher.main(new String[] { "-s", srcDir.getPath() });
		} finally {
			System.setOut(origOut);
		}

		String output = baos.toString(StandardCharsets.UTF_8);
		assertThat(output)
				.contains("package launchdemo")
				.contains("class Sample {")
				.contains("enhancement SampleEnhancement : launchdemo.Sample {");
	}

	@Test
	void testLauncherUsageOnInvalidArgs() throws Exception {
		PrintStream origOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
			GosuLauncher.main(new String[] { "--invalid" });
		} finally {
			System.setOut(origOut);
		}

		String output = baos.toString(StandardCharsets.UTF_8);
		assertThat(output).contains("usage: spoon.gosu.GosuLauncher -s <sourceDir> [-o <outputDir>]");
	}

	@Test
	void testLauncherUsageOnNonexistentDir() throws Exception {
		PrintStream origOut = System.out;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
			GosuLauncher.main(new String[] { "-s", "nonexistent/directory/path" });
		} finally {
			System.setOut(origOut);
		}

		String output = baos.toString(StandardCharsets.UTF_8);
		assertThat(output).contains("usage: spoon.gosu.GosuLauncher -s <sourceDir> [-o <outputDir>]");
	}

	@Test
	void testLauncherUsesOf() {
		Factory factory = new Launcher().getFactory();
		GosuEnvironment gosu = GosuEnvironment.initialize(List.of(srcDir));
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> types = builder.buildAll(srcDir);
		CtType<?> sample = types.stream()
				.filter(t -> t.getSimpleName().equals("Sample"))
				.findFirst()
				.orElseThrow();

		List<String> uses = GosuLauncher.usesOf(builder, sample);
		assertThat(uses).contains("java.util.List", "java.io.*");
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
