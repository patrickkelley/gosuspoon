---
title: Gosu language support
tags: [gosu, parsing, metamodel, transformation]
keywords: gosu, spoon-gosu, enhancement, structure, properties, closures
---

The `spoon-gosu` submodule provides a parser and model builder for the [Gosu](https://gosu-lang.org) language, backed by the Gosu compiler (`gosu-core`). It constructs a standard Spoon `Ct` metamodel from `.gs` and `.gsx` source files and provides an idiomatic `GosuPrettyPrinter` to render transformed models back into valid Gosu source code.

### Installation

To use `spoon-gosu`, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>fr.inria.gforge.spoon</groupId>
    <artifactId>spoon-gosu</artifactId>
    <version>$currentVersion</version>
</dependency>
```

### Basic Usage

```java
File srcDir = new File("src/main/gosu");
GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(srcDir));

Launcher launcher = new Launcher();
Factory factory = launcher.getFactory();
GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
List<CtType<?>> types = builder.buildAll(srcDir);

GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
for (CtType<?> type : types) {
    String gosuCode = printer.printType(type);
    System.out.println(gosuCode);
}
```

### Metamodel Mapping Details

| Gosu Construct | Spoon Metamodel Element | Notes |
| :--- | :--- | :--- |
| `class Foo` | `CtClass` | Marked with `GosuKind.CLASS` |
| `interface Foo` | `CtInterface` | Standard interface |
| `structure Foo` | `CtInterface` | Marked with `GosuKind.STRUCTURE` |
| `enhancement Foo : Target` | `CtClass` | Marked with `GosuKind.ENHANCEMENT` & `GosuEnhancedType` |
| `enum Foo` | `CtEnum` | Constants as `CtEnumValue` |
| `annotation Foo` | `CtAnnotationType` | Gosu custom annotations |
| `property get/set Foo` | `CtMethod` | Marked with `GosuKind.PROPERTY_GET` / `PROPERTY_SET` |
| `\ x : int -> x + 1` | `CtLambda` | Preserves typed parameter declarations |
| `using (var r = ...) { }` | `CtTryWithResource` | Supports local-var and bare expression resources |
| `obj typeis Target` | `CtBinaryOperator` | Kind `INSTANCEOF` |
| `obj as Target` | `CtExpression` with type cast | Printed as `(obj as Target)` |
| `0..10`, `0..|10` | `CtExpression` | Interval expressions |
| `uses pkg.Type` | `CtImport` | Preserves single and wildcard imports |

### AST Synthesis and Transformation

`GosuPrettyPrinter` provides tagging helper methods to synthesize new Gosu language elements from scratch:

```java
// Tag an enhancement
GosuPrettyPrinter.tagEnhancement(myClass, "java.lang.String");

// Tag a structure
GosuPrettyPrinter.tagStructure(myInterface);

// Tag property getter and setter
GosuPrettyPrinter.tagPropertyGet(getterMethod);
GosuPrettyPrinter.tagPropertySet(setterMethod);
```
