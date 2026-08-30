package spoon.gosu.phase0;

import gw.lang.parser.GosuParserFactory;
import gw.lang.parser.IGosuParser;
import gw.lang.parser.IParsedElement;
import gw.lang.parser.ScriptPartId;
import gw.lang.parser.expressions.IBinaryExpression;
import gw.lang.parser.expressions.ILocalVarDeclaration;
import gw.lang.parser.expressions.INumericLiteralExpression;
import gw.lang.parser.expressions.IParameterDeclaration;
import gw.lang.parser.expressions.IStringLiteralExpression;
import gw.lang.parser.expressions.IProgram;
import gw.lang.parser.statements.IFunctionStatement;
import gw.lang.parser.IParsedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase-0 feasibility probe: (1) can the official Gosu compiler (gosu-core)
 * parse standalone Gosu source without a Guidewire environment, and (2) can a
 * minimal subset be mapped onto the Spoon Ct metamodel.
 */
public final class Phase0Probe {

    private int failures;

    public static void main(String[] args) {
        System.exit(new Phase0Probe().run());
    }

    private int run() {
        System.out.println("== Phase 0: Gosu parsing feasibility ==");
        java.io.File srcDir = writeGsFixtures();
        bootstrapGosu(java.util.Collections.singletonList(srcDir));
        probeProgramStatementList();
        probeGsFilesFromDisk();
        System.out.println(failures == 0 ? "\nRESULT: PASS" : "\nRESULT: FAIL (" + failures + ")");
        return failures == 0 ? 0 : 1;
    }

    // Standalone bootstrap of the Gosu type system, the way the Gosu CLI/REPL does it.
    static void bootstrapGosu(java.util.List<java.io.File> sourceDirs) {
        try {
            gw.lang.reflect.module.IExecutionEnvironment env =
                    gw.internal.gosu.parser.ExecutionEnvironment.instance();
            gw.lang.init.GosuInitialization init = gw.lang.init.GosuInitialization.instance(env);
            gw.fs.IDirectory cwd =
                    gw.config.CommonServices.getFileSystem().getIDirectory(new java.io.File("."));
            List<gw.lang.init.GosuPathEntry> entries = new ArrayList<>();
            for (java.io.File dir : sourceDirs) {
                List<gw.fs.IDirectory> sources = new ArrayList<>();
                sources.add(gw.config.CommonServices.getFileSystem().getIDirectory(dir));
                entries.add(new gw.lang.init.GosuPathEntry(cwd, sources));
            }
            if (entries.isEmpty()) {
                entries.add(new gw.lang.init.GosuPathEntry(cwd, java.util.Collections.emptyList()));
            }
            init.initializeRuntime(entries);
            System.out.println("  [ok] gosu runtime initialized: "
                    + init.isInitialized());
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            throw new IllegalStateException("gosu bootstrap failed", t);
        }
    }

    private java.io.File writeGsFixtures() {
        java.io.File srcDir = new java.io.File("target/gsrc");
        java.io.File demoDir = new java.io.File(srcDir, "demo");
        demoDir.mkdirs();
        java.io.File staleGs = new java.io.File(demoDir, "StringExt.gs");
        staleGs.delete();
        java.io.File greeter = new java.io.File(demoDir, "Greeter.gs");
        java.io.File enh = new java.io.File(demoDir, "StringExt.gsx");
        try {
            java.nio.file.Files.write(greeter.toPath(),
                    ("package demo\n"
                     + "uses java.util.List\n"
                     + "class Greeter {\n"
                     + "  var _name : String\n"
                     + "  construct( n : String ) {\n"
                     + "    _name = n\n"
                     + "  }\n"
                     + "  function greet() : String {\n"
                     + "    return \"Hello \" + _name\n"
                     + "  }\n"
                     + "}\n").getBytes());
            java.nio.file.Files.write(enh.toPath(),
                    ("package demo\n"
                     + "enhancement StringExt : String {\n"
                     + "  function shout() : String {\n"
                     + "    return this + \"!\"\n"
                     + "  }\n"
                     + "}\n").getBytes());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return srcDir;
    }

    // Load the .gs fixtures via the Gosu type loader — the exact path a
    // file-based .gs project would use (class + enhancement).
    private void probeGsFilesFromDisk() {
        System.out.println("\n--- B) parse .gs class + enhancement files from disk ---");
        try {
            gw.lang.reflect.IType staticType = gw.lang.reflect.TypeSystem.getByFullName("demo.Greeter");
            System.out.println("Greeter -> " + staticType.getRelativeName());
            check("class type loaded", staticType != null);

            // Insert the .gs source dir onto the module source path and load types
            // the way the module/gosuc does: through the module's GosuClassTypeLoader.
            gw.lang.reflect.gs.GosuClassTypeLoader loader =
                    gw.lang.reflect.gs.GosuClassTypeLoader.getDefaultClassLoader();
            gw.lang.reflect.gs.IEnhancementIndex enhIndex = loader.getEnhancementIndex();
            enhIndex.maybeLoadEnhancementIndex();

            gw.lang.reflect.gs.ICompilableType ct = loader.getType("demo.StringExt");
            boolean ctEnh = ct instanceof gw.internal.gosu.parser.IGosuEnhancementInternal;
            String ctInfo = "(" + (ct == null ? "null" : ct.getClass().getSimpleName()) + ")";
            if (ctEnh) {
                gw.internal.gosu.parser.IGosuEnhancementInternal enh =
                        (gw.internal.gosu.parser.IGosuEnhancementInternal) ct;
                enh.compileDeclarationsIfNeeded();
                try {
                    enh.compileDefinitionsIfNeeded();
                } catch (Throwable ignored) {
                    // bodies best-effort for the probe
                }
                List<String> enhFuncs = new ArrayList<>();
                for (gw.internal.gosu.parser.DynamicFunctionSymbol dfs :
                        enh.getParseInfo().getMemberFunctions().values()) {
                    List<gw.lang.parser.statements.IReturnStatement> rets = new ArrayList<>();
                    gw.lang.parser.statements.IFunctionStatement decl = dfs.getDeclFunctionStmt();
                    String params = decl == null || decl.getParameters() == null
                            ? ""
                            : decl.getParameters().size() + " params";
                    if (decl != null) {
                        decl.getContainedParsedElementsByType(
                                gw.lang.parser.statements.IReturnStatement.class, rets);
                    }
                    enhFuncs.add(dfs.getDisplayName() + "(" + params
                            + ") retStmts=" + rets.size());
                }
                ctInfo += " enhancedType=" + enh.getEnhancedType()
                        + " funcs=" + enhFuncs;
            }
            System.out.println("compilable type demo.StringExt: " + ctInfo);
            check("enhancement compilable type loaded", ctEnh);
            check("enhancement flagged",
                    ctEnh && ctInfo.contains("enhancedType=java.lang.String"));

            gw.lang.reflect.IType enhancedStr =
                    gw.lang.reflect.TypeSystem.getByFullName("java.lang.String");
            List<? extends gw.lang.reflect.gs.IGosuEnhancement> enhs =
                    enhIndex.getEnhancementsForType(enhancedStr);
            System.out.println("String enhancements: "
                    + enhs.stream().map(e -> e.getName())
                            .sorted().collect(java.util.stream.Collectors.toList()));
            check("String enhanced by demo.StringExt", enhs.stream()
                    .anyMatch(e -> e.getName().equals("demo.StringExt")));

            if (staticType instanceof gw.lang.reflect.gs.IGosuClass) {
                gw.lang.reflect.gs.IGosuClass gsClass = (gw.lang.reflect.gs.IGosuClass) staticType;
                gw.lang.parser.statements.IClassStatement classStmt = gsClass.getClassStatement();
                gw.lang.parser.statements.IClassDeclaration classDecl = classStmt.getClassDeclaration();
                System.out.println("class AST: " + classDecl.getClass().getSimpleName());
                List<String> typeInfoMethods = gsClass.getTypeInfo().getMethods().stream()
                        .filter(m -> !m.getDisplayName().contains("$"))
                        .map(m -> m.getDisplayName())
                        .sorted().collect(java.util.stream.Collectors.toList());
                System.out.println("methods (type info): " + typeInfoMethods);

                gw.internal.gosu.parser.IGosuClassInternal gs = (gw.internal.gosu.parser.IGosuClassInternal) gsClass;
                gs.compileDeclarationsIfNeeded();
                try {
                    gs.compileDefinitionsIfNeeded();
                } catch (Throwable ignored) {
                    // bodies best-effort for the probe
                }
                java.util.Map<String, gw.internal.gosu.parser.DynamicFunctionSymbol> funcs =
                        gs.getParseInfo().getMemberFunctions();
                List<String> memberFuncs = new ArrayList<>();
                for (gw.internal.gosu.parser.DynamicFunctionSymbol dfs : funcs.values()) {
                    String sig = dfs.getDisplayName() + " -> " + dfs.getReturnType();
                    gw.lang.parser.statements.IFunctionStatement decl = dfs.getDeclFunctionStmt();
                    List<gw.lang.parser.statements.IReturnStatement> rets = new ArrayList<>();
                    if (decl != null) {
                        decl.getContainedParsedElementsByType(
                                gw.lang.parser.statements.IReturnStatement.class, rets);
                    }
                    memberFuncs.add(sig + "  retStmts=" + rets.size()
                            + "  body=" + (decl == null ? "null" : decl.toString().replace('\n', ' ')));
                }
                System.out.println("member functions (parse info): " + memberFuncs);
                check("class function found in AST", memberFuncs.stream()
                        .anyMatch(s -> s.startsWith("greet -> ") && s.contains("retStmts=1")));

                Number n = null;
                try {
                    n = (Number) gsClass.getTypeInfo().getProperty("greet") == null ? null : 1;
                } catch (Throwable ignored) {
                    // not needed
                }
                System.out.println("  (probe) class name=" + gsClass.getClass().getSimpleName()
                        + " isProxy=" + gs.isProxy());
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail("disk parse threw: " + t);
        }
    }

    // Parse a program made of statements: local vars, a function, and a call.
    private void probeProgramStatementList() {
        System.out.println("\n--- A) parse statements (var + function + call) ---");
        String source =
                "var x = 42\n"
                + "var s = \"hi\"\n"
                + "function add(a: int, b: int): int {\n"
                + "  return a + b\n"
                + "}\n"
                + "print(add(x, 8))\n";
        try {
            IGosuParser parser = GosuParserFactory.createParser(source);
            parser.setEditorParser(true);
            IProgram root = parser.parseProgram(new ScriptPartId("probe.vars", null));
            System.out.println("parsed root = " + root.getClass().getSimpleName()
                    + "  issues=" + root.getImmediateParseIssues().size());
            CompiledProbe analysis = scan(root);
            System.out.println("local vars: " + analysis.varNames);
            System.out.println("functions:  " + analysis.functionNames);
            System.out.println("literals:   " + analysis.literals);
            System.out.println("bin ops:    " + analysis.binOps);
            check("parsed without parse exceptions", !root.hasParseExceptions());
            check("saw local var declarations", !analysis.varNames.isEmpty());
            check("saw function declarations", !analysis.functionNames.isEmpty());

            MinimalCtModel model = new MinimalCtModel();
            model.build(analysis, "Probe");
            System.out.println("--- Ct model pretty-printed as (Java-ish) source ---");
            System.out.println(model.print());
            check("report method present", model.methodName() != null);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail("program parse threw: " + e);
        }
    }

    private CompiledProbe scan(IParsedElement root) {
        CompiledProbe out = new CompiledProbe();
        root.visit(pe -> {
            if (pe instanceof ILocalVarDeclaration) {
                ILocalVarDeclaration d = (ILocalVarDeclaration) pe;
                String type = d.getSymbol() != null
                        ? String.valueOf(d.getSymbol().getType()) : "<unknown>";
                out.varNames.add(d.getLocalVarName() + " : " + type);
            } else if (pe instanceof IFunctionStatement) {
                IFunctionStatement fs = (IFunctionStatement) pe;
                List<String> params = new ArrayList<>();
                for (IParameterDeclaration p : fs.getParameters()) {
                    params.add(String.valueOf(p.getLocalVarName())
                            + ":" + (p.getSymbol() != null ? p.getSymbol().getType() : "<unknown>"));
                }
                out.functionNames.add(fs.getDynamicFunctionSymbol().getDisplayName() + "(" + String.join(", ", params) + ")");
            } else if (pe instanceof INumericLiteralExpression) {
                out.literals.add(String.valueOf(((INumericLiteralExpression) pe).getValue()));
            } else if (pe instanceof IStringLiteralExpression) {
                out.literals.add("\"" + ((IStringLiteralExpression) pe).getValue() + "\"");
            } else if (pe instanceof IBinaryExpression) {
                IBinaryExpression b = (IBinaryExpression) pe;
                out.binOps.add(b.getOperator() + "(" + brief(b.getLHS()) + "," + brief(b.getRHS()) + ")");
            }
        });
        return out;
    }

    private static String brief(Object o) {
        if (o == null) {
            return "null";
        }
        String s = o.toString().replaceAll("\\s+", " ").trim();
        return s.length() > 40 ? s.substring(0, 40) + "..." : s;
    }

    // Counts IReturnStatement / IAssignmentStatement / IVarStatement nested under a
    // function declaration (i.e. body statements), without resolving nested blocks.
    static int countStatements(gw.lang.parser.IParsedElement root) {
        final int[] n = {0};
        root.visit(el -> {
            if (el instanceof gw.lang.parser.statements.IReturnStatement
                    || el instanceof gw.lang.parser.expressions.IInitializerAssignment
                    || el instanceof gw.lang.parser.expressions.IVarStatement) {
                n[0]++;
            }
        });
        return n[0];
    }

    private void check(String what, boolean ok) {
        System.out.println((ok ? "  [ok] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private void fail(String what) {
        System.out.println("  [EXCEPTION] " + what);
        failures++;
    }

    static final class CompiledProbe {
        final List<String> varNames = new ArrayList<>();
        final List<String> functionNames = new ArrayList<>();
        final List<String> literals = new ArrayList<>();
        final List<String> binOps = new ArrayList<>();
    }
}