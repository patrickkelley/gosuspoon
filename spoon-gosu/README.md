# Spoon Gosu

`spoon-gosu` is a Spoon front-end for the [Gosu](https://gosu-lang.org) programming language, backed by `gosu-core` 1.17.x. It enables static analysis, source code transformations, and program generation for Gosu source files (`.gs` and `.gsx`).

## Features

- **Full Language Coverage**:
  - Declarations: `class`, `interface`, `structure`, `enhancement`, `enum`, `annotation`.
  - Properties: `property get Name() : Type` and `property set Name(val : Type)`.
  - Generics: Type parameters and multi-bound constraints (`<T extends Comparable<T> & Serializable>`).
  - Annotations: Full support across types, fields, methods, parameters, and constructors.
  - Closures / Blocks: `\ x : int -> x + 1` mapped to `CtLambda`.
  - Collections & Literals: `{ 1, 2, 3 }`, `[ 1, 2, 3 ]`, map literals, range intervals `0..10`, `0..|10`.
  - Null safety: `?.` (null-safe access) and `*:` (null-safe expansion).
  - Resource management: `using (var r = ...) { ... }` mapped to `CtTryWithResource`.
  - Type operators: `typeis` (`CtBinaryOperator` with `INSTANCEOF`), `typeof`, `as` (`(expr as Type)`), and `eval`.
- **Imports**: `uses` declarations preserved as `CtImport` nodes.
- **Round-Trip Fixpoint**: Output produced by `GosuPrettyPrinter` re-parses identically across repeated compiler cycles.
- **Programmatic Synthesis**: Convenience methods in `GosuPrettyPrinter` for tagging enhancements, structures, and properties.

## Getting Started

### Maven Dependency

```xml
<dependency>
    <groupId>fr.inria.gforge.spoon</groupId>
    <artifactId>spoon-gosu</artifactId>
    <version>${spoon.version}</version>
</dependency>
```

### Basic Usage

```java
import spoon.Launcher;
import spoon.gosu.GosuEnvironment;
import spoon.gosu.GosuModelBuilder;
import spoon.gosu.GosuPrettyPrinter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GosuExample {
    public static void main(String[] args) {
        // 1. Initialize Gosu environment
        File srcDir = new File("src/main/gosu");
        GosuEnvironment gosu = GosuEnvironment.initialize(Collections.singletonList(srcDir));

        // 2. Build Spoon model
        Launcher launcher = new Launcher();
        Factory factory = launcher.getFactory();
        GosuModelBuilder builder = new GosuModelBuilder(factory, gosu);
        List<CtType<?>> types = builder.buildAll(srcDir);

        // 3. Pretty-print back to Gosu
        GosuPrettyPrinter printer = new GosuPrettyPrinter(factory.getEnvironment());
        for (CtType<?> type : types) {
            System.out.println(printer.printType(type));
        }
    }
}
```

### Command Line Interface

```bash
java -cp spoon-gosu/target/spoon-gosu-11.5.1-SNAPSHOT.jar:... spoon.gosu.GosuLauncher -s <sourceDir> [-o <outputDir>]
```

## Running Tests

```bash
mvn -f spoon-pom test -pl '../spoon-gosu'
```
