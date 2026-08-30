/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import spoon.Launcher;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
* Command-line entry point for the Gosu flavor of Spoon:
*
* <pre>
*   java spoon.gosu.GosuLauncher -s srcDir [-o outDir]
* </pre>
*
* Parses every {@code .gs}/{@code .gsx} file under {@code srcDir} into a Ct
* model and pretty-prints it back as Gosu (to stdout unless {@code -o} is
* given, in which case files are written under the matching package
* directories).
*/
public final class GosuLauncher {

	private GosuLauncher() {
	}

	public static void main(String[] args) throws IOException {
		File sourceDir = null;
		File outputDir = null;
		for (int i = 0; i < args.length; i++) {
			if ("-s".equals(args[i]) && i + 1 < args.length) {
				sourceDir = new File(args[++i]);
			} else if ("-o".equals(args[i]) && i + 1 < args.length) {
				outputDir = new File(args[++i]);
			} else {
				usage();
				return;
			}
		}
		if (sourceDir == null || !sourceDir.isDirectory()) {
			usage();
			return;
		}

		GosuEnvironment gosu = GosuEnvironment.initialize(
				java.util.Collections.singletonList(sourceDir));
		Launcher launcher = new Launcher();
		Factory factory = launcher.getFactory();
		GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
		List<CtType<?>> types = builder.buildAll(sourceDir);
		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());

		for (CtType<?> type : types) {
			String packageName = type.getPackage() == null || type.getPackage().isUnnamedPackage()
					? null
					: type.getPackage().getQualifiedName();
			StringBuilder out = new StringBuilder();
			if (packageName != null) {
				out.append("package ").append(packageName).append("\n\n");
			}
			for (String use : usesOf(builder, type)) {
				out.append("uses ").append(use).append("\n");
			}
			out.append("\n");
			out.append(printer.printType(type)).append("\n");

			if (outputDir == null) {
				System.out.println(out);
			} else {
				String extension = GosuPrettyPrinter.isGosuEnhancement(type) ? ".gsx" : ".gs";
				String packagePath = packageName == null
						? ""
						: packageName.replace('.', File.separatorChar);
				File target = new File(outputDir, new File(packagePath,
						type.getSimpleName() + extension).getPath());
				target.getParentFile().mkdirs();
				Files.write(target.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
				System.out.println("wrote " + target);
			}
		}
	}

	/** Reconstructs the {@code uses} clauses from the model's Ct imports. */
	public static List<String> usesOf(GosuModelBuilder builder, CtType<?> type) {
		List<String> uses = new ArrayList<>();
		for (CtImport imp : builder.getTypeImports().getOrDefault(type, List.of())) {
			if (imp.getImportKind() == CtImportKind.ALL_TYPES
					&& imp.getReference() instanceof CtPackageReference) {
				uses.add(((CtPackageReference) imp.getReference()).getQualifiedName() + ".*");
			} else if (imp.getReference() instanceof CtTypeReference) {
				uses.add(((CtTypeReference<?>) imp.getReference()).getQualifiedName());
			}
		}
		return uses;
	}

	private static void usage() {
		System.out.println("usage: spoon.gosu.GosuLauncher -s <sourceDir> [-o <outputDir>]");
	}
}
