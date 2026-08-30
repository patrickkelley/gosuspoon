/*
 * SPDX-License-Identifier: (MIT OR CECILL-C)
 *
 * Copyright (C) 2006-2023 INRIA and contributors
 *
 * Spoon is available either under the terms of the MIT License (see LICENSE-MIT.txt) or the Cecill-C License (see LICENSE-CECILL-C.txt). You as the user are entitled to choose the terms under which to adopt Spoon.
 */
package spoon.gosu;

import gw.fs.IDirectory;
import gw.lang.init.GosuInitialization;
import gw.lang.reflect.gs.GosuClassTypeLoader;
import gw.lang.reflect.gs.ICompilableType;
import gw.lang.reflect.module.IExecutionEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Bootstraps and owns the Gosu type system for a Spoon-based source directory.
*
* <p>Gosu may only be initialized once per JVM (the runtime raises an
* {@code IllegalStateException} on a second {@code initializeRuntime}); this
* class guards that with a process-wide one-shot flag. Successive
* {@link #initialize} calls reuse the already-initialized runtime.</p>
*/
public final class GosuEnvironment implements AutoCloseable {

	private static final Object LOCK = new Object();
	private static GosuEnvironment instance;
	private static boolean initialized;

	private final IExecutionEnvironment env;
	private final List<File> sourceDirs;

	private GosuEnvironment(IExecutionEnvironment env, List<File> sourceDirs) {
		this.env = env;
		this.sourceDirs = new ArrayList<>(sourceDirs);
	}

	/**
	 * Bootstraps Gosu against the given source directories, or reinitializes the
	 * runtime if the JVM already has an initialized environment.
	 */
	public static GosuEnvironment initialize(Collection<File> sourceDirs) {
		synchronized (LOCK) {
			IExecutionEnvironment env = gw.internal.gosu.parser.ExecutionEnvironment.instance();
			List<IDirectory> sources = new ArrayList<>();
			gw.config.CommonServices.getFileSystem();
			for (File dir : sourceDirs) {
				sources.add(gw.config.CommonServices.getFileSystem().getIDirectory(dir));
			}
			IDirectory cwd = gw.config.CommonServices.getFileSystem()
					.getIDirectory(new File("."));
			List<gw.lang.init.GosuPathEntry> entries = new ArrayList<>();
			entries.add(new gw.lang.init.GosuPathEntry(cwd, sources));
			GosuInitialization init = GosuInitialization.instance(env);
			if (instance != null || init.isInitialized()) {
				init.reinitializeRuntime(entries, new String[0]);
			} else {
				init.initializeRuntime(entries);
			}
			initialized = init.isInitialized();
			if (!initialized) {
				throw new IllegalStateException("Gosu runtime failed to initialize");
			}
			instance = new GosuEnvironment(env, new ArrayList<>(sourceDirs));
			return instance;
		}
	}

	public static boolean isInitialized() {
		synchronized (LOCK) {
			return initialized;
		}
	}

	public IExecutionEnvironment executionEnvironment() {
		return env;
	}

	public List<File> sourceDirs() {
		return new ArrayList<>(sourceDirs);
	}

	public GosuClassTypeLoader classTypeLoader() {
		return GosuClassTypeLoader.getDefaultClassLoader();
	}

	/**
	* Loads a named Gosu type (class or enhancement) through the module type
	* loader, the same entry point the compiler and IDE use.
	*/
	public ICompilableType loadType(String fullyQualifiedName) {
		return classTypeLoader().getType(fullyQualifiedName);
	}

	/**
	* Scans a directory for {@code .gs} / {@code .gsx} files and returns their
	* fully-qualified type names, derived from the file's {@code package}
	* clause (falling back to the path relative to the source root).
	*/
	public List<String> scanTypeNames(File sourceDir) {
		List<String> names = new ArrayList<>();
		collect(sourceDir, sourceDir, names);
		return names;
	}

	private static void collect(File root, File dir, List<String> out) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File f : files) {
			if (f.isDirectory()) {
				collect(root, f, out);
			} else if (f.getName().endsWith(".gs") || f.getName().endsWith(".gsx")) {
				out.add(fullyQualifiedName(root, f));
			}
		}
	}

	private static String fullyQualifiedName(File root, File file) {
		String pkg = readPackage(file);
		String simple = file.getName().replaceAll("\\.gsx?$", "");
		if (pkg == null || pkg.isEmpty()) {
			String rel = root.toPath().relativize(file.toPath()).toString()
					.replace(File.separatorChar, '.');
			return rel.replaceAll("\\.(gs|gsx)$", "");
		}
		return pkg + "." + simple;
	}

	private static String readPackage(File file) {
		try {
			List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
			Matcher m = Pattern.compile("^\\s*package\\s+([\\w.]+)").matcher("");
			for (String line : lines) {
				m.reset(line);
				if (m.find()) {
					return m.group(1);
				}
			}
		} catch (IOException e) {
			// fall through
		}
		return null;
	}

	/**
	* Uninitializing Gosu would prevent any later use in the same JVM, so this
	* is deliberately a no-op; see {@link #initialize}.
	*/
	@Override
	public void close() {
		// Gosu's runtime cannot be re-initialized in the same JVM.
	}
}
