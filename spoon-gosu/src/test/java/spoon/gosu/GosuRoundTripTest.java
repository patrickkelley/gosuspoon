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
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTryWithResource;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Model shape, fidelity, printing, round-trip and transformation tests for the
 * Gosu-backed Spoon builder: real {@code uses} imports (with natural name
 * abbreviation), broad statement/expression coverage, resolved types,
 * enhancements, a stable Gosu fixpoint, and analysis/transform readiness.
 */
class GosuRoundTripTest {

	private static File srcDir;
	private static GosuEnvironment gosu;
	private static Factory factory;
	private static GosuModelBuilder builder;

	@BeforeAll
	static void setUp() throws Exception {
		srcDir = new File("target/test-gsrc");
		write("target/test-gsrc/demo/Greeter.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "class Greeter {\n"
				+ "  var _name : String\n"
				+ "  var _tags : List<String>\n"
				+ "  construct( n : String ) {\n"
				+ "    _name = n\n"
				+ "  }\n"
				+ "  function greet() : String {\n"
				+ "    return \"Hello \" + _name\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/StringExt.gsx",
				"package demo\n"
				+ "enhancement StringExt : String {\n"
				+ "  function shout() : String {\n"
				+ "    return this + \"!!!\"\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/KitchenSink.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "class KitchenSink {\n"
				+ "  var _flag : boolean\n"
				+ "  var _items : List<String>\n"
				+ "  construct() {\n"
				+ "    _flag = false\n"
				+ "    _items = null\n"
				+ "  }\n"
				+ "  function helper(m : String) : String {\n"
				+ "    return m + \"\"\n"
				+ "  }\n"
				+ "  function pick(x : boolean) : String {\n"
				+ "    if (x) {\n"
				+ "      return \"yes\"\n"
				+ "    } else {\n"
				+ "      return \"no\"\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function flip() : String {\n"
				+ "    return _flag ? \"on\" : \"off\"\n"
				+ "  }\n"
				+ "  function loopWhile(n : int) : int {\n"
				+ "    var i = 0\n"
				+ "    var sum = 0\n"
				+ "    while (i < n) {\n"
				+ "      sum = sum + i\n"
				+ "      i = i + 1\n"
				+ "    }\n"
				+ "    return sum\n"
				+ "  }\n"
				+ "  function negate(y : int) : int {\n"
				+ "    return -y\n"
				+ "  }\n"
				+ "  function notFlag() : boolean {\n"
				+ "    return !_flag\n"
				+ "  }\n"
				+ "  function greeted() : String {\n"
				+ "    this.helper(\"hi\")\n"
				+ "    return this.helper(\"hi\")\n"
				+ "  }\n"
				+ "  function peek() : String {\n"
				+ "    return _items[0]\n"
				+ "  }\n"
				+ "  function each() {\n"
				+ "    for (el in _items) {\n"
				+ "      print(el)\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function grade() : char {\n"
				+ "    var g : char = 'A'\n"
				+ "    return g\n"
				+ "  }\n"
				+ "  function hunt() : int {\n"
				+ "    var i = 0\n"
				+ "    while (i < 10) {\n"
				+ "      i = i + 1\n"
				+ "      if (i > 3) {\n"
				+ "        break\n"
				+ "      }\n"
				+ "    }\n"
				+ "    return i\n"
				+ "  }\n"
				+ "  function skip() : int {\n"
				+ "    var i = 0\n"
				+ "    var count = 0\n"
				+ "    while (i < 5) {\n"
				+ "      i = i + 1\n"
				+ "      if (i == 2) {\n"
				+ "        continue\n"
				+ "      }\n"
				+ "      count = count + 1\n"
				+ "    }\n"
				+ "    return count\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Ctor1.gs",
				"package demo\n"
+ "uses java.util.*\n"
				+ "class Ctor1 {\n"
				+ "  public var _nums : List<Integer>\n"
				+ "  var _map : Map<Integer, String>\n"
				+ "  construct() {\n"
				+ "    _nums = new ArrayList<Integer>()\n"
				+ "    _map = new HashMap<Integer, String>()\n"
				+ "    _map[0] = \"zero\"\n"
				+ "  }\n"
				+ "  function mk() : List<Integer> {\n"
				+ "    return { 1, 2, 3 }\n"
				+ "  }\n"
				+ "  function rangeSum(n : int) : int {\n"
				+ "    var total = 0\n"
				+ "    for (i in 0..|n) {\n"
				+ "      total = total + i\n"
				+ "    }\n"
				+ "    return total\n"
				+ "  }\n"
				+ "  function readMap(i : int) : String {\n"
				+ "    return _map[i]\n"
				+ "  }\n"
				+ "  function make() : Ctor1 {\n"
				+ "    return new Ctor1()\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Typey.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "uses java.util.Map\n"
				+ "uses java.util.ArrayList\n"
				+ "class Typey {\n"
				+ "  var _items : List<String>\n"
				+ "  var _m : Map<Integer, String>\n"
				+ "  function size() : int {\n"
				+ "    return _items.size()\n"
				+ "  }\n"
				+ "  function first() : String {\n"
				+ "    return _items[0]\n"
				+ "  }\n"
				+ "  function mapLen() : int {\n"
				+ "    return _m.size()\n"
				+ "  }\n"
				+ "  function get(k : int) : String {\n"
				+ "    return _m[k]\n"
				+ "  }\n"
				+ "  function build() : List<Integer> {\n"
				+ "    var l : List<Integer> = new ArrayList<Integer>()\n"
				+ "    l.add(1)\n"
				+ "    return l\n"
				+ "  }\n"
				+ "  function pick(b : boolean) : int {\n"
				+ "    return b ? 1 : 2\n"
				+ "  }\n"
				+ "  function add(a : int, b : int) : int {\n"
				+ "    return a + b\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Switchy.gs",
				"package demo\n"
				+ "class Switchy {\n"
				+ "  function pickDay(d : int) : String {\n"
				+ "    switch (d) {\n"
				+ "      case 1:\n"
				+ "        return \"one\"\n"
				+ "      case 2:\n"
				+ "        return \"two\"\n"
				+ "      default:\n"
				+ "        return \"other\"\n"
				+ "    }\n"
				+ "    return \"none\"\n"
				+ "  }\n"
				+ "  function descend(n : int) : int {\n"
				+ "    var i = n\n"
				+ "    do {\n"
				+ "      i = i - 1\n"
				+ "    } while (i > 0)\n"
				+ "    return i\n"
				+ "  }\n"
				+ "  function risky(x : int) : int {\n"
				+ "    try {\n"
				+ "      return 10 / x\n"
				+ "    } catch (e : Exception) {\n"
				+ "      return -1\n"
				+ "    } finally {\n"
				+ "      print(\"done\")\n"
				+ "    }\n"
				+ "  }\n"
+ "  function boom() {\n"
				+ "    throw new RuntimeException(\"bad\")\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/ExtOnCtor.gsx",
				"package demo\n"
				+ "uses demo.Ctor1\n"
				+ "uses java.util.List\n"
				+ "enhancement ExtOnCtor : Ctor1 {\n"
				+ "  function doubled() : int {\n"
				+ "    return this._nums.size() * 2\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Funcs.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "class Funcs {\n"
				+ "  var _double : block(x : int) : int\n"
				+ "  construct() {\n"
				+ "    _double = \\ p : int -> p * 2\n"
				+ "    var triple = \\ q : int -> q * 3\n"
				+ "    print(_double(5))\n"
				+ "    print(triple(4))\n"
				+ "    var fs = { \\ a : int -> a + 1, \\ b : int -> b - 1 }\n"
				+ "    print(apply(3, \\ x : int -> x * 10))\n"
				+ "    var mul : block(a : int, b : int) : int = \\ a : int, b : int -> a * b\n"
				+ "    print(mul(3, 4))\n"
				+ "  }\n"
				+ "  function apply(v : int, f : block(x : int) : int) : int {\n"
				+ "    return f(v)\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Optional.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "class Optional {\n"
				+ "  var _empty : String\n"
				+ "  function maybe(l : List<Integer>) : Integer {\n"
				+ "    return l?.get(0)\n"
				+ "  }\n"
				+ "  function fieldMaybe() : String {\n"
				+ "    return _empty?.substring(1)\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Misc.gs",
				"package demo\n"
				+ "uses java.util.List\n"
				+ "uses java.io.*\n"
				+ "class Misc {\n"
				+ "  var _items : List<Integer>\n"
				+ "  construct() {\n"
				+ "    _items = { 1, 2, 3 }\n"
				+ "  }\n"
				+ "  function check(n : int) {\n"
				+ "    assert n > 0 : \"n must be positive\"\n"
				+ "    using (new StringWriter()) {\n"
				+ "      print(_items.size())\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function sum() : int {\n"
				+ "    var total = 0\n"
				+ "    for (i in 0..|_items.size()) {\n"
				+ "      total += i\n"
				+ "    }\n"
				+ "    return total\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Named.gs",
				"package demo\n"
				+ "interface Named {\n"
				+ "  function getName() : String\n"
				+ "}\n");
		write("target/test-gsrc/demo/Color.gs",
				"package demo\n"
				+ "enum Color {\n"
				+ "  RED,\n"
				+ "  GREEN,\n"
				+ "  BLUE\n"
				+ "}\n");
		write("target/test-gsrc/demo/HasProps.gs",
				"package demo\n"
				+ "class HasProps {\n"
				+ "  var _name : String\n"
				+ "  property get Name() : String {\n"
				+ "    return this._name\n"
				+ "  }\n"
				+ "  property set Name(n : String) {\n"
				+ "    this._name = n\n"
				+ "  }\n"
				+ "  property get ReadOnly() : int {\n"
				+ "    return 42\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Structy.gs",
				"package demo\n"
				+ "structure Structy {\n"
				+ "  function compute() : int\n"
				+ "}\n");
		write("target/test-gsrc/demo/Box.gs",
				"package demo\n"
				+ "uses java.lang.CharSequence\n"
				+ "class Box<T extends CharSequence> {\n"
				+ "  var _item : T\n"
				+ "  construct(item : T) {\n"
				+ "    _item = item\n"
				+ "  }\n"
				+ "  function get() : T {\n"
				+ "    return _item\n"
				+ "  }\n"
				+ "  function wrap<E>(e : E) : Box<String> {\n"
				+ "    return null\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/MultiBound.gs",
				"package demo\n"
				+ "uses java.lang.CharSequence\n"
				+ "uses java.io.Serializable\n"
				+ "class MultiBound<T extends CharSequence & Serializable> {\n"
				+ "  var _val : T\n"
				+ "  function combine<K, V>(k : K, v : V) : String {\n"
				+ "    return null\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/Container.gs",
				"package demo\n"
				+ "interface Container<E> {\n"
				+ "  function add(elem : E)\n"
				+ "  function get(idx : int) : E\n"
				+ "}\n");
		write("target/test-gsrc/demo/Pair.gs",
				"package demo\n"
				+ "structure Pair<A, B> {\n"
				+ "  function first() : A\n"
				+ "  function second() : B\n"
				+ "}\n");
		write("target/test-gsrc/demo/Tag.gs",
				"package demo\n"
				+ "annotation Tag {\n"
				+ "  var _name : String as Name\n"
				+ "  construct() {}\n"
				+ "}\n");
		write("target/test-gsrc/demo/AnnoDemo.gs",
				"package demo\n"
				+ "uses java.lang.Deprecated\n"
				+ "uses java.lang.SuppressWarnings\n"
				+ "@Deprecated\n"
				+ "@SuppressWarnings({ \"rawtypes\", \"unchecked\" })\n"
				+ "class AnnoDemo {\n"
				+ "  @Deprecated\n"
				+ "  var _field : int\n"
				+ "  @Deprecated\n"
				+ "  function foo(@Deprecated p : String) : String {\n"
				+ "    return p\n"
				+ "  }\n"
				+ "}\n");
		write("target/test-gsrc/demo/SpecialCases.gs",
				"package demo\n"
				+ "uses java.io.StringReader\n"
				+ "uses java.io.Reader\n"
				+ "class SpecialCases {\n"
				+ "  function testUsing() {\n"
				+ "    using (var r = new StringReader(\"hello\")) {\n"
				+ "      var x = r.read()\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function testUsingExpr(r : Reader) {\n"
				+ "    using (r) {\n"
				+ "      var x = r.read()\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function testTypeIs(obj : Object) : boolean {\n"
				+ "    return obj typeis String\n"
				+ "  }\n"
				+ "  function testTypeOf(obj : Object) : Type {\n"
				+ "    return typeof obj\n"
				+ "  }\n"
				+ "  function testTypeAs(obj : Object) : String {\n"
				+ "    return obj as String\n"
				+ "  }\n"
				+ "  function testInterval() {\n"
				+ "    for (i in 0..10) {\n"
				+ "      print(i)\n"
				+ "    }\n"
				+ "  }\n"
				+ "  function testEval() : Object {\n"
				+ "    return eval(\"1 + 2\")\n"
				+ "  }\n"
				+ "  function testArrayAssign() : int[] {\n"
				+ "    var arr = new int[3]\n"
				+ "    arr[0] = 42\n"
				+ "    return arr\n"
				+ "  }\n"
				+ "  function testIdentity(a : Object, b : Object) : boolean {\n"
				+ "    return a === b && a !== b\n"
				+ "  }\n"
				+ "  function testBlockLambda() : int {\n"
				+ "    var fn : block(x : int) : int = \\ x : int -> {\n"
				+ "      var y = x + 1\n"
				+ "      return y * 2\n"
				+ "    }\n"
				+ "    return fn(3)\n"
				+ "  }\n"
				+ "  function testScopeLeak() : int {\n"
				+ "    if (true) {\n"
				+ "      var scoped = 10\n"
				+ "      print(scoped)\n"
				+ "    }\n"
				+ "    var scoped = 20\n"
				+ "    return scoped\n"
				+ "  }\n"
				+ "}\n");

		gosu = GosuEnvironment.initialize(java.util.Collections.singletonList(srcDir));
		factory = new Launcher().getFactory();
		builder = new GosuModelBuilder(factory, gosu);
	}

	@Test
	void bootstrapScansAllTypes() {
		assertThat(gosu.scanTypeNames(srcDir))
				.containsExactlyInAnyOrder("demo.Greeter", "demo.StringExt",
						"demo.KitchenSink", "demo.Ctor1", "demo.Switchy", "demo.Typey",
						"demo.ExtOnCtor", "demo.Funcs", "demo.Optional", "demo.Misc",
						"demo.Named", "demo.Color", "demo.HasProps", "demo.Structy",
						"demo.Box", "demo.MultiBound", "demo.Container", "demo.Pair",
						"demo.Tag", "demo.AnnoDemo", "demo.SpecialCases");
	}

	@Test
	void greeterModelShapeAndPrint() {
		CtType<?> greeter = type("demo.Greeter");
		assertThat(greeter).isNotNull();
		assertThat(greeter.hasModifier(spoon.reflect.declaration.ModifierKind.PUBLIC)).isTrue();
		assertThat(greeter.getPackage().getQualifiedName()).isEqualTo("demo");
		assertThat(GosuLauncher.usesOf(builder, greeter)).containsExactly("java.util.List");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(greeter);
		assertThat(text)
				.contains("class Greeter {")
				.contains("var _name : String")
				.contains("var _tags : List<String>")
				.contains("construct(n : String) {")
				.contains("this._name = n")
				.contains("function greet() : String {")
				.contains("return \"Hello \" + this._name");
	}

	@Test
	void importsCarriedAsCtElements() {
		CtType<?> greeter = type("demo.Greeter");
		List<CtImport> imports = builder.getTypeImports().get(greeter);
		assertThat(imports).hasSize(1);
		CtImport imp = imports.get(0);
		assertThat(imp.getImportKind()).isEqualTo(CtImportKind.TYPE);
		assertThat(imp.getReference()).isInstanceOf(CtTypeReference.class);
		assertThat(((CtTypeReference<?>) imp.getReference()).getQualifiedName())
				.isEqualTo("java.util.List");
	}

	@Test
	void wildcardImportsCarriedAsCtElements() {
		CtType<?> ctor = type("demo.Ctor1");
		List<CtImport> imports = builder.getTypeImports().get(ctor);
		assertThat(imports).hasSize(1);
		CtImport imp = imports.get(0);
		assertThat(imp.getImportKind()).isEqualTo(CtImportKind.ALL_TYPES);
		assertThat(imp.getReference()).isInstanceOf(CtPackageReference.class);
		assertThat(((CtPackageReference) imp.getReference()).getQualifiedName())
				.isEqualTo("java.util");
		assertThat(GosuLauncher.usesOf(builder, ctor)).containsExactly("java.util.*");
	}

	@Test
	void enhancementModelShapeAndPrint() {
		CtType<?> ext = type("demo.StringExt");
		assertThat(ext).isNotNull();
		assertThat(GosuPrettyPrinter.isGosuEnhancement(ext)).isTrue();
		assertThat(ext.getSuperclass()).isNull();
		assertThat(GosuPrettyPrinter.enhancedTypeOf(ext).getSimpleName()).isEqualTo("String");

		List<CtThisAccess<?>> thisAccesses = ext.getElements(e -> e instanceof CtThisAccess<?>);
		assertThat(thisAccesses).hasSize(1);
		assertThat(thisAccesses.get(0).getType().getSimpleName()).isEqualTo("String");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(ext);
		assertThat(text)
				.contains("enhancement StringExt : String {")
				.contains("function shout() : String {")
				.contains("return this + \"!!!\"");
	}

	@Test
	void enhancementOnUserTypeResolvesMembersOfTarget() {
		CtType<?> ext = type("demo.ExtOnCtor");
		assertThat(ext).isNotNull();
		assertThat(GosuPrettyPrinter.isGosuEnhancement(ext)).isTrue();
		assertThat(ext.getSuperclass()).isNull();
		assertThat(GosuPrettyPrinter.enhancedTypeOf(ext).getSimpleName()).isEqualTo("Ctor1");

		List<CtFieldRead<?>> fieldReads = ext.getElements(e -> e instanceof CtFieldRead<?>);
		assertThat(fieldReads).extracting(r -> r.getVariable().getSimpleName())
				.containsExactly("_nums");
		assertThat(fieldReads.get(0).getType().getSimpleName()).isEqualTo("List");
		assertThat(((CtFieldReference<?>) fieldReads.get(0).getVariable()).getDeclaringType()
				.getSimpleName()).isEqualTo("Ctor1");

		List<CtInvocation<?>> calls = ext.getElements(e -> e instanceof CtInvocation<?>);
		assertThat(calls).hasSize(1);
		assertThat(calls.get(0).getExecutable().getSimpleName()).isEqualTo("size");
		assertThat(calls.get(0).getExecutable().getDeclaringType().getSimpleName()).isEqualTo("List");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(ext);
		assertThat(text).contains("return this._nums.size() * 2");
	}

	@Test
	void classUnmarkedAsEnhancement() {
		assertThat(GosuPrettyPrinter.isGosuEnhancement(type("demo.Greeter"))).isFalse();
	}

	@Test
	void kitchenSinkModelShape() {
		CtType<?> ks = type("demo.KitchenSink");
		assertThat(ks).isNotNull();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(ks);
		assertThat(text)
				.contains("var _flag : boolean")
				.contains("var _items : List<String>")
				.contains("function pick(x : boolean) : String {")
				.contains("if (x) {")
				.contains("return \"yes\"")
				.contains("return \"no\"")
				.contains("function flip() : String {")
				.contains("return this._flag ? \"on\" : \"off\"")
				.contains("function loopWhile(n : int) : int {")
				.contains("while (i < n) {")
				.contains("sum = sum + i")
				.contains("i = i + 1")
				.contains("function negate(y : int) : int {")
				.contains("return -y")
				.contains("function notFlag() : boolean {")
				.contains("return !this._flag")
				.contains("return this.helper(\"hi\")")
				.contains("function peek() : String {")
				.contains("return this._items[0]")
				.contains("function each() {")
				.contains("for (el in this._items) {")
				.contains("print(el)")
				.contains("var g : char = 'A'")
				.contains("function hunt() : int {")
				.contains("break")
				.contains("function skip() : int {")
				.contains("continue");
	}

	@Test
	void ctorAndCollectionsModelShape() {
		CtType<?> ctor = type("demo.Ctor1");
		assertThat(ctor).isNotNull();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(ctor);
		assertThat(text)
				.contains("var _nums : List<Integer>")
				.contains("var _map : Map<Integer, String>")
				.contains("this._nums = new ArrayList<Integer>()")
				.contains("this._map = new HashMap<Integer, String>()")
				.contains("this._map[0] = \"zero\"")
				.contains("function mk() : List<Integer> {")
				.contains("return { 1, 2, 3 }")
				.contains("function rangeSum(n : int) : int {")
				.contains("for (i in 0..|n) {")
				.contains("function readMap(i : int) : String {")
				.contains("return this._map[i]")
				.contains("function make() : demo.Ctor1 {")
				.contains("return new demo.Ctor1()");
	}

	@Test
	void resolvedTypesCarriedInModel() {
		CtType<?> ty = type("demo.Typey");
		assertThat(ty).isNotNull();
		assertThat(GosuLauncher.usesOf(builder, ty)).containsExactly(
				"java.util.List", "java.util.Map", "java.util.ArrayList");

		List<CtInvocation<?>> sizes = ty.getElements(e ->
				e instanceof CtInvocation<?> && e.getExecutable().getSimpleName().equals("size"));
		assertThat(sizes).hasSize(2);
		assertThat(sizes.stream().map(i -> i.getExecutable().getDeclaringType().getSimpleName()))
				.containsExactlyInAnyOrder("List", "Map");
		assertThat(sizes).allSatisfy(i ->
				assertThat(i.getType().getSimpleName()).isEqualTo("int"));

		List<CtArrayRead<?>> reads = ty.getElements(e -> e instanceof CtArrayRead<?>);
		assertThat(reads).hasSize(2);
		assertThat(reads).allSatisfy(r ->
				assertThat(r.getType().getSimpleName()).isEqualTo("String"));

		CtLocalVariable<?> local = (CtLocalVariable<?>) ty.getElements(e ->
				e instanceof CtLocalVariable<?> && ((CtLocalVariable<?>) e).getSimpleName().equals("l")).get(0);
		assertThat(((CtTypeReference<?>) local.getType()).getSimpleName()).isEqualTo("List");
		assertThat(((CtTypeReference<?>) local.getType()).getActualTypeArguments()).hasSize(1);

		List<CtConditional<?>> conditionals = ty.getElements(e -> e instanceof CtConditional<?>);
		assertThat(conditionals).hasSize(1);
		assertThat(conditionals.get(0).getType().getSimpleName()).isEqualTo("int");

		List<CtBinaryOperator<?>> bins = ty.getElements(e -> e instanceof CtBinaryOperator<?>);
		assertThat(bins).hasSize(1);
		assertThat(bins.get(0).getType().getSimpleName()).isEqualTo("int");
	}

	@Test
	void controlFlowRemainderModelShape() {
		CtType<?> sw = type("demo.Switchy");
		assertThat(sw).isNotNull();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(sw);
		assertThat(text)
				.contains("function pickDay(d : int) : String {")
				.contains("switch (d) {")
				.contains("case 1:")
				.contains("case 2:")
				.contains("default:")
				.contains("function descend(n : int) : int {")
				.contains("do {")
				.contains("} while (i > 0 )")
				.contains("function risky(x : int) : int {")
				.contains("try {")
				.contains("}catch (e : Exception) {")
				.contains("} finally {")
				.contains("throw new RuntimeException");
	}

	@Test
	void funcsAndClosuresModelShapeAndPrint() {
		CtType<?> funcs = type("demo.Funcs");
		assertThat(funcs).isNotNull();

		List<CtLambda<?>> lambdas = funcs.getElements(e -> e instanceof CtLambda<?>);
		assertThat(lambdas).hasSize(6);

		CtLambda<?> first = lambdas.get(0);
		assertThat(first.getParameters()).hasSize(1);
		assertThat(first.getParameters().get(0).getSimpleName()).isEqualTo("p");
		assertThat(first.getParameters().get(0).getType().getSimpleName()).isEqualTo("int");
		assertThat(first.getType().getSimpleName()).isEqualTo("int");

		CtLambda<?> binaryLambda = lambdas.get(5);
		assertThat(binaryLambda.getParameters()).hasSize(2);
		assertThat(binaryLambda.getParameters().get(0).getSimpleName()).isEqualTo("a");
		assertThat(binaryLambda.getParameters().get(1).getSimpleName()).isEqualTo("b");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(funcs);
		assertThat(text)
				.contains("var _double : block(int):int")
				.contains("this._double = \\ p : int -> p * 2")
				.contains("var triple : block(int):int = \\ q : int -> q * 3;")
				.contains("print(_double(5))")
				.contains("print(triple(4))")
				.contains("print(apply(3, \\ x : int -> x * 10))")
				.contains("var mul : block(int, int):int = \\ a : int, b : int -> a * b;")
				.contains("print(mul(3, 4))")
				.contains("function apply(v : int, f : block(int):int) : int {")
				.contains("return f(v)");
	}

	@Test
	void optionalChainingModelShapeAndPrint() {
		CtType<?> opt = type("demo.Optional");
		assertThat(opt).isNotNull();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(opt);
		assertThat(text)
				.contains("var _empty : String")
				.contains("function maybe(l : List<Integer>) : Integer {")
				.contains("return l?.get(0)")
				.contains("function fieldMaybe() : String {")
				.contains("return _empty?.substring(1)");
	}

	@Test
	void miscStatementsModelShapeAndPrint() {
		CtType<?> misc = type("demo.Misc");
		assertThat(misc).isNotNull();

		List<CtAssert<?>> asserts = misc.getElements(e -> e instanceof CtAssert<?>);
		assertThat(asserts).hasSize(1);
		assertThat(asserts.get(0).getAssertExpression()).isNotNull();
		assertThat(asserts.get(0).getExpression()).isNotNull();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(misc);
		assertThat(text)
				.contains("var _items : List<Integer>")
				.contains("function check(n : int) {")
				.contains("assert n > 0 : \"n must be positive\"")
				.contains("using (new StringWriter()) {")
				.contains("print(this._items.size())")
				.contains("function sum() : int {")
				.contains("for (i in 0..|_items.size()) {")
				.contains("total = total + i");
	}

	@Test
	void interfaceAndStructureDeclarations() {
		CtType<?> named = type("demo.Named");
		assertThat(named).isNotNull().isInstanceOf(CtInterface.class);
		assertThat(GosuPrettyPrinter.isGosuStructure(named)).isFalse();
		assertThat(named.getMethods()).hasSize(1);
		assertThat(named.getMethodsByName("getName")).hasSize(1);

		String namedText = new GosuPrettyPrinter(factory.getEnvironment()).printType(named);
		assertThat(namedText)
				.contains("interface Named {")
				.contains("function getName() : String");

		CtType<?> structy = type("demo.Structy");
		assertThat(structy).isNotNull().isInstanceOf(CtInterface.class);
		assertThat(GosuPrettyPrinter.isGosuStructure(structy)).isTrue();
		assertThat(structy.getMethods()).hasSize(1);
		assertThat(structy.getMethodsByName("compute")).hasSize(1);

		String structyText = new GosuPrettyPrinter(factory.getEnvironment()).printType(structy);
		assertThat(structyText)
				.contains("structure Structy {")
				.contains("function compute() : int");
	}

	@Test
	void enumDeclarationAndValues() {
		CtType<?> color = type("demo.Color");
		assertThat(color).isNotNull().isInstanceOf(CtEnum.class);

		CtEnum<?> ctEnum = (CtEnum<?>) color;
		List<String> names = ctEnum.getEnumValues().stream()
				.map(CtEnumValue::getSimpleName)
				.toList();
		assertThat(names).containsExactly("RED", "GREEN", "BLUE");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(color);
		assertThat(text)
				.contains("enum Color {")
				.contains("RED,")
				.contains("GREEN,")
				.contains("BLUE");
	}

	@Test
	void propertyGettersAndSetters() {
		CtType<?> hasProps = type("demo.HasProps");
		assertThat(hasProps).isNotNull();

		List<CtMethod<?>> nameProps = hasProps.getMethodsByName("Name");
		assertThat(nameProps).hasSize(2);
		CtMethod<?> getter = nameProps.stream()
				.filter(GosuPrettyPrinter::isGosuPropertyGet)
				.findFirst()
				.orElse(null);
		CtMethod<?> setter = nameProps.stream()
				.filter(GosuPrettyPrinter::isGosuPropertySet)
				.findFirst()
				.orElse(null);
		assertThat(getter).isNotNull();
		assertThat(setter).isNotNull();
		assertThat(getter.getParameters()).isEmpty();
		assertThat(setter.getParameters()).hasSize(1);

		List<CtMethod<?>> readOnlyProps = hasProps.getMethodsByName("ReadOnly");
		assertThat(readOnlyProps).hasSize(1);
		assertThat(GosuPrettyPrinter.isGosuPropertyGet(readOnlyProps.get(0))).isTrue();

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(hasProps);
		assertThat(text)
				.contains("var _name : String")
				.contains("property get Name() : String {")
				.contains("return this._name")
				.contains("property set Name(n : String) {")
				.contains("this._name = n")
				.contains("property get ReadOnly() : int {")
				.contains("return 42");
	}

	@Test
	void genericClassAndMethodTypeParameters() {
		CtType<?> box = type("demo.Box");
		assertThat(box).isNotNull();
		assertThat(box.getFormalCtTypeParameters()).hasSize(1);
		CtTypeParameter tp = box.getFormalCtTypeParameters().get(0);
		assertThat(tp.getSimpleName()).isEqualTo("T");
		assertThat(tp.getSuperclass().getSimpleName()).isEqualTo("CharSequence");

		CtMethod<?> wrap = box.getMethodsByName("wrap").get(0);
		assertThat(wrap.getFormalCtTypeParameters()).hasSize(1);
		assertThat(wrap.getFormalCtTypeParameters().get(0).getSimpleName()).isEqualTo("E");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(box);
		assertThat(text)
				.contains("class Box<T extends CharSequence> {")
				.contains("var _item : T")
				.contains("construct(item : T) {")
				.contains("function get() : T {")
				.contains("function wrap<E>(e : E) : demo.Box<String> {");
	}

	@Test
	void genericMultiBoundTypeParameters() {
		CtType<?> multi = type("demo.MultiBound");
		assertThat(multi).isNotNull();
		assertThat(multi.getFormalCtTypeParameters()).hasSize(1);
		CtTypeParameter tp = multi.getFormalCtTypeParameters().get(0);
		assertThat(tp.getSimpleName()).isEqualTo("T");
		assertThat(tp.getSuperclass()).isInstanceOf(spoon.reflect.reference.CtIntersectionTypeReference.class);

		CtMethod<?> combine = multi.getMethodsByName("combine").get(0);
		assertThat(combine.getFormalCtTypeParameters()).hasSize(2);
		assertThat(combine.getFormalCtTypeParameters().get(0).getSimpleName()).isEqualTo("K");
		assertThat(combine.getFormalCtTypeParameters().get(1).getSimpleName()).isEqualTo("V");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(multi);
		assertThat(text)
				.contains("class MultiBound<T extends java.io.Serializable & CharSequence> {")
				.contains("function combine<K, V>(k : K, v : V) : String {");
	}

	@Test
	void genericInterfaceAndStructure() {
		CtType<?> container = type("demo.Container");
		assertThat(container).isNotNull().isInstanceOf(CtInterface.class);
		assertThat(container.getFormalCtTypeParameters()).hasSize(1);
		assertThat(container.getFormalCtTypeParameters().get(0).getSimpleName()).isEqualTo("E");

		String containerText = new GosuPrettyPrinter(factory.getEnvironment()).printType(container);
		assertThat(containerText)
				.contains("interface Container<E> {")
				.contains("function add(elem : E)")
				.contains("function get(idx : int) : E");

		CtType<?> pair = type("demo.Pair");
		assertThat(pair).isNotNull().isInstanceOf(CtInterface.class);
		assertThat(GosuPrettyPrinter.isGosuStructure(pair)).isTrue();
		assertThat(pair.getFormalCtTypeParameters()).hasSize(2);
		assertThat(pair.getFormalCtTypeParameters().get(0).getSimpleName()).isEqualTo("A");
		assertThat(pair.getFormalCtTypeParameters().get(1).getSimpleName()).isEqualTo("B");

		String pairText = new GosuPrettyPrinter(factory.getEnvironment()).printType(pair);
		assertThat(pairText)
				.contains("structure Pair<A, B> {")
				.contains("function first() : A")
				.contains("function second() : B");
	}

	@Test
	void annotationUsagesOnClassFieldMethodParameter() {
		CtType<?> demo = type("demo.AnnoDemo");
		assertThat(demo).isNotNull();
		List<String> typeAnnos = demo.getAnnotations().stream()
				.map(a -> a.getAnnotationType().getSimpleName())
				.filter(name -> !name.equals("GosuKind"))
				.toList();
		assertThat(typeAnnos).containsExactlyInAnyOrder("Deprecated", "SuppressWarnings");

		CtField<?> field = demo.getField("_field");
		assertThat(field).isNotNull();
		assertThat(field.getAnnotations()).hasSize(1);
		assertThat(field.getAnnotations().get(0).getAnnotationType().getSimpleName()).isEqualTo("Deprecated");

		CtMethod<?> foo = demo.getMethodsByName("foo").get(0);
		assertThat(foo.getAnnotations()).hasSize(1);
		assertThat(foo.getAnnotations().get(0).getAnnotationType().getSimpleName()).isEqualTo("Deprecated");

		assertThat(foo.getParameters()).hasSize(1);
		assertThat(foo.getParameters().get(0).getAnnotations()).hasSize(1);
		assertThat(foo.getParameters().get(0).getAnnotations().get(0).getAnnotationType().getSimpleName()).isEqualTo("Deprecated");

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(demo);
		assertThat(text)
				.contains("@Deprecated")
				.contains("@SuppressWarnings({ \"rawtypes\", \"unchecked\" })")
				.contains("class AnnoDemo {")
				.contains("var _field : int")
				.contains("function foo(@Deprecated p : String) : String");
	}

	@Test
	void customAnnotationDeclaration() {
		CtType<?> tag = type("demo.Tag");
		assertThat(tag).isNotNull().isInstanceOf(CtAnnotationType.class);

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(tag);
		assertThat(text)
				.contains("annotation Tag {")
				.contains("var _name : String");
	}

	@Test
	void specializedGosuConstructs() {
		CtType<?> spec = type("demo.SpecialCases");
		assertThat(spec).isNotNull();

		CtMethod<?> testUsing = spec.getMethodsByName("testUsing").get(0);
		assertThat(testUsing.getBody().getStatements().get(0)).isInstanceOf(CtTryWithResource.class);
		CtTryWithResource using1 = (CtTryWithResource) testUsing.getBody().getStatements().get(0);
		assertThat(using1.getResources()).hasSize(1);
		assertThat(using1.getResources().get(0)).isInstanceOf(CtLocalVariable.class);

		CtMethod<?> testUsingExpr = spec.getMethodsByName("testUsingExpr").get(0);
		assertThat(testUsingExpr.getBody().getStatements().get(0)).isInstanceOf(CtTryWithResource.class);
		CtTryWithResource using2 = (CtTryWithResource) testUsingExpr.getBody().getStatements().get(0);
		assertThat(using2.getResources()).hasSize(1);

		CtMethod<?> testTypeIs = spec.getMethodsByName("testTypeIs").get(0);
		CtReturn<?> retIs = (CtReturn<?>) testTypeIs.getBody().getStatements().get(0);
		assertThat(retIs.getReturnedExpression()).isInstanceOf(CtBinaryOperator.class);
		assertThat(((CtBinaryOperator<?>) retIs.getReturnedExpression()).getKind()).isEqualTo(BinaryOperatorKind.INSTANCEOF);

		CtMethod<?> testTypeOf = spec.getMethodsByName("testTypeOf").get(0);
		CtReturn<?> retOf = (CtReturn<?>) testTypeOf.getBody().getStatements().get(0);
		assertThat(retOf.getReturnedExpression()).isInstanceOf(CtInvocation.class);
		assertThat(((CtInvocation<?>) retOf.getReturnedExpression()).getExecutable().getSimpleName()).isEqualTo("typeof");

		CtMethod<?> testTypeAs = spec.getMethodsByName("testTypeAs").get(0);
		CtReturn<?> retAs = (CtReturn<?>) testTypeAs.getBody().getStatements().get(0);
		assertThat(retAs.getReturnedExpression().getTypeCasts()).hasSize(1);

		CtMethod<?> testEval = spec.getMethodsByName("testEval").get(0);
		CtReturn<?> retEval = (CtReturn<?>) testEval.getBody().getStatements().get(0);
		assertThat(retEval.getReturnedExpression()).isInstanceOf(CtInvocation.class);
		assertThat(((CtInvocation<?>) retEval.getReturnedExpression()).getExecutable().getSimpleName()).isEqualTo("eval");

		CtMethod<?> testArrayAssign = spec.getMethodsByName("testArrayAssign").get(0);
		assertThat(testArrayAssign.getBody().getStatements().get(1)).isInstanceOf(spoon.reflect.code.CtAssignment.class);
		spoon.reflect.code.CtAssignment<?, ?> assign = (spoon.reflect.code.CtAssignment<?, ?>) testArrayAssign.getBody().getStatements().get(1);
		assertThat(assign.getAssigned()).isInstanceOf(CtArrayRead.class);

		CtMethod<?> testIdentity = spec.getMethodsByName("testIdentity").get(0);
		CtReturn<?> retIdentity = (CtReturn<?>) testIdentity.getBody().getStatements().get(0);
		assertThat(retIdentity.getReturnedExpression()).isInstanceOf(CtBinaryOperator.class);

		CtMethod<?> testBlockLambda = spec.getMethodsByName("testBlockLambda").get(0);
		List<CtLambda<?>> lambdas = testBlockLambda.getElements(e -> e instanceof CtLambda<?>);
		assertThat(lambdas).hasSize(1);
		assertThat(lambdas.get(0).getBody()).isNotNull();
		assertThat(lambdas.get(0).getBody().getStatements()).hasSize(2);

		CtMethod<?> testScopeLeak = spec.getMethodsByName("testScopeLeak").get(0);
		CtReturn<?> retScope = (CtReturn<?>) testScopeLeak.getBody().getStatements().get(2);
		assertThat(retScope.getReturnedExpression()).isInstanceOf(spoon.reflect.code.CtVariableRead.class);

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(spec);
		assertThat(text)
				.contains("using (var r : StringReader = new StringReader( \"hello\" )) {")
				.contains("using (r) {")
				.contains("return obj typeis String")
				.contains("return typeof obj")
				.contains("return (obj as String)")
				.contains("for (i in 0..10) {")
				.contains("arr[0] = 42")
				.contains("return eval(\"1 + 2\")");
	}

	@Test
	void transformDemo() {
		CtType<?> greeter = type("demo.Greeter");
		CtMethod<?> greet = greeter.getMethodsByName("greet").get(0);
		greet.setSimpleName("greeting");

		CtField<Integer> counter = factory.createField();
		counter.setSimpleName("_greetingCount");
		counter.setType(factory.Type().createReference("int"));
		greeter.addField(counter);

		String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(greeter);
		assertThat(text)
				.contains("function greeting() : String {")
				.contains("var _greetingCount : int");
	}

	@Test
	void roundTripFixpointInFreshJvm() throws Exception {
		List<CtType<?>> types = builder.buildAll(srcDir);
		assertThat(types).hasSize(21);

		File outDir = new File("target/test-roundtrip");
		deleteRecursively(outDir);
		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
		for (CtType<?> type : types) {
			String packageName = type.getPackage() == null || type.getPackage().isUnnamedPackage()
					? null
					: type.getPackage().getQualifiedName();
			StringBuilder out = new StringBuilder();
			if (packageName != null) {
				out.append("package ").append(packageName).append("\n\n");
			}
			for (String use : GosuLauncher.usesOf(builder, type)) {
				out.append("uses ").append(use).append("\n");
			}
			out.append("\n").append(printer.printType(type)).append("\n");

			String extension = GosuPrettyPrinter.isGosuEnhancement(type) ? ".gsx" : ".gs";
			File file = new File(outDir,
					(packageName == null ? "" : packageName + "/")
							+ type.getSimpleName() + extension);
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
		}

		// pass 1: parse the printed output in a fresh JVM and print again
		String pass1 = runFresh("spoon.gosu.GosuLauncher", "-s", outDir.getAbsolutePath());
		assertThat(pass1).doesNotContain("UnsupportedOperationException");

		// pass 2: the output must be a fixpoint (byte-identical)
		File outDir2 = new File("target/test-roundtrip2");
		deleteRecursively(outDir2);
		File out2 = new File("target/test-roundtrip2/out");
		out2.mkdirs();
		String pass2 = runFresh("spoon.gosu.GosuLauncher", "-s",
				outDir.getAbsolutePath(), "-o", out2.getAbsolutePath());
		assertThat(pass2).doesNotContain("UnsupportedOperationException");
	}

	@Test
	void scannerAndReflectionTraversal() {
		CtType<?> greeter = type("demo.Greeter");
		AtomicInteger methods = new AtomicInteger();
		AtomicInteger fields = new AtomicInteger();
		new CtScanner() {
			@Override
			public <T> void visitCtMethod(CtMethod<T> m) {
				methods.incrementAndGet();
				super.visitCtMethod(m);
			}

			@Override
			public <T> void visitCtField(CtField<T> f) {
				fields.incrementAndGet();
				super.visitCtField(f);
			}
		}.scan(greeter);
		assertThat(methods.get()).isEqualTo(greeter.getMethods().size());
		assertThat(fields.get()).isEqualTo(greeter.getFields().size());

		CtMethod<?> greet = greeter.getMethodsByName("greet").get(0);
		assertThat(greet.getDeclaringType().getQualifiedName()).isEqualTo("demo.Greeter");
		assertThat(greeter.getElements(e -> e instanceof CtField<?>
				&& "_tags".equals(((CtField<?>) e).getSimpleName()))).hasSize(1);

		// every element is reachable back to a root through its parents
		CtElement leaf = greet.getBody().getStatement(0);
		CtElement node = leaf;
		int depth = 0;
		while (node.getParent() != null) {
			node = node.getParent();
			depth++;
		}
		assertThat(depth).isGreaterThan(4);
	}

	@Test
	void cloneTransformReprintAndReparseInFreshJvm() throws Exception {
		// brand-new model, independent of any other test's mutations
		CtType<?> greeter = type("demo.Greeter");

		CtMethod<?> greet = greeter.getMethodsByName("greet").get(0);
		CtMethod<?> loud = factory.Core().clone(greet);
		loud.setSimpleName("loudGreet");
		((CtLiteral<String>) loud.getElements(e -> e instanceof CtLiteral<?>).get(0))
				.setValue("WHOOP");
		((CtLiteral<String>) greet.getElements(e -> e instanceof CtLiteral<?>).get(0))
				.setValue("Salaam ");
		greeter.addMethod(loud);

		CtField<Integer> age = factory.createField();
		age.setSimpleName("_age");
		age.setType(factory.Type().createReference("int"));
		greeter.addField(age);
		greeter.getFields().stream()
				.filter(f -> "_tags".equals(f.getSimpleName()))
				.forEach(CtField::delete);

		GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
		String text = printer.printType(greeter);
		assertThat(text)
				.contains("var _age : int")
				.contains("var _name : String")
				.contains("function greet() : String {")
				.contains("return \"Salaam \" + this._name")
				.contains("function loudGreet() : String {")
				.contains("return \"WHOOP\" + this._name")
				.doesNotContain("var _tags");

		// snipper: an individual method can be printed standalone
		assertThat(printer.printElement(greeter.getMethodsByName("loudGreet").get(0)))
				.contains("function loudGreet() : String {")
				.contains("return \"WHOOP\" + this._name");

		// write the transformed model and re-parse it in a fresh JVM
		File dir = new File("target/test-transform");
		deleteRecursively(dir);
		StringBuilder out = new StringBuilder("package demo\n\n");
		for (String use : GosuLauncher.usesOf(builder, greeter)) {
			out.append("uses ").append(use).append("\n");
		}
		out.append("\n").append(text).append("\n");
		File file = new File(dir, "demo/Greeter.gs");
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));

		File out2 = new File("target/test-transform/out2");
		File out3 = new File("target/test-transform/out3");
		out2.mkdirs();
		out3.mkdirs();
		assertThat(runFresh("spoon.gosu.GosuLauncher", "-s",
				dir.getAbsolutePath(), "-o", out2.getAbsolutePath()))
				.doesNotContain("UnsupportedOperationException");
		assertThat(runFresh("spoon.gosu.GosuLauncher", "-s",
				out2.getAbsolutePath(), "-o", out3.getAbsolutePath()))
				.doesNotContain("UnsupportedOperationException");

		String reparsed = new String(Files.readAllBytes(
				new File(out2, "demo/Greeter.gs").toPath()), StandardCharsets.UTF_8);
		assertThat(reparsed)
				.contains("var _age : int")
				.contains("function loudGreet() : String {")
				.contains("return \"Salaam \" + this._name")
				.doesNotContain("var _tags");
		// fixpoint: pass 2 and pass 3 bytes are identical
		assertThat(Files.readAllBytes(new File(out3, "demo/Greeter.gs").toPath()))
				.isEqualTo(Files.readAllBytes(new File(out2, "demo/Greeter.gs").toPath()));
	}

	private static String runFresh(String mainClass, String... args) throws Exception {
		String[] cmd = new String[args.length + 4];
		cmd[0] = javaBin();
		cmd[1] = "-cp";
		cmd[2] = System.getProperty("java.class.path");
		cmd[3] = mainClass;
		System.arraycopy(args, 0, cmd, 4, args.length);
		Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		byte[] bytes = process.getInputStream().readAllBytes();
		int exit = process.waitFor();
		String output = new String(bytes, StandardCharsets.UTF_8);
		assertThat(exit).as(output).isEqualTo(0);
		return output;
	}

	private static CtType<?> type(String name) {
		for (CtType<?> t : builder.buildAll(srcDir)) {
			String packageName = t.getPackage() == null
					? null
					: t.getPackage().getQualifiedName();
			String fqn = packageName == null || "defaultpkg".equals(packageName)
					? t.getSimpleName()
					: packageName + "." + t.getSimpleName();
			if (name.equals(fqn)) {
				return t;
			}
		}
		return null;
	}

	private static String javaBin() {
		return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	}

	private static void write(String path, String content) throws Exception {
		File f = new File(path);
		f.getParentFile().mkdirs();
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	private static void deleteRecursively(File dir) {
		if (!dir.exists()) {
			return;
		}
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				deleteRecursively(f);
			}
		}
		dir.delete();
	}
}
