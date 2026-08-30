package spoon.gosu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtImportKind;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 tests: the Gosu-backed Spoon model carries real {@code uses}
 * imports (with natural name abbreviation), broad statement/expression
 * coverage, and pretty-printed output stays a stable Gosu fixpoint.
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
                + "  var _nums : List<Integer>\n"
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

        gosu = GosuEnvironment.initialize(java.util.Collections.singletonList(srcDir));
        factory = new Launcher().getFactory();
        builder = new GosuModelBuilder(factory, gosu);
    }

    @Test
    void bootstrapScansAllTypes() {
        assertThat(gosu.scanTypeNames(srcDir))
                .containsExactlyInAnyOrder("demo.Greeter", "demo.StringExt",
                        "demo.KitchenSink", "demo.Ctor1", "demo.Switchy");
    }

    @Test
    void greeterModelShapeAndPrint() {
        CtType<?> greeter = type("demo.Greeter");
        assertThat(greeter).isNotNull();
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

        String text = new GosuPrettyPrinter(factory.getEnvironment()).printType(ext);
        assertThat(text)
                .contains("enhancement StringExt : String {")
                .contains("function shout() : String {")
                .contains("return this + \"!!!\"");
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
        assertThat(types).hasSize(5);

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