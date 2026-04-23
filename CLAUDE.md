# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Toolchain

- **Java 21** required (`maven.compiler.source/target` is 21).
- Build system: **Maven** multi-module (root `pom.xml`, ~100 modules under `plugconfig/`, plus `src/`, `tomcat-embedded/`, `war-build/`).
- Runtime is **Eclipse Equinox OSGi** — modules under `plugconfig/<bundle>/` are real OSGi bundles with `META-INF/MANIFEST.MF` (`Bundle-SymbolicName`, `Require-Bundle`, `Export-Package`) and `plugin.xml` extension declarations. The Maven build only packages them; bundle wiring at runtime is OSGi.
- The same modules are also declared as Eclipse projects (`.project`, `.classpath` at the root) so the workspace can be opened directly in Eclipse.

## Common commands

One-time setup — install JARs not in Maven Central into the local Maven repo:
```sh
./install_all_old_jars.sh
```

Build everything (skip tests for speed):
```sh
mvn package -DskipTests
```

Run all tests:
```sh
mvn -pl src test
```

Run a single test (pattern matches the simple class name):
```sh
mvn -pl src test -Dtest=DiagramXmlReaderTest
```
The CI workflow (`.github/workflows/run_tests_on_commit.yaml`) runs `mvn -pl src test` followed by an explicit `-Dtest=DiagramXmlReaderTest` pass.

Launch the web edition (embedded Tomcat at http://localhost:8080/bioumlweb/) — requires MySQL running first (see README; `docker-compose.yaml` brings up the `bioumlsupport2` database with the dump in `dumps/`):
```sh
mvn -pl tomcat-embedded exec:java
# or, forked JVM with --add-opens (port 8085):
mvn -pl tomcat-embedded exec:exec@run-forked
```

Tests that hit R need `r-base` / `r-base-dev` installed on the host.

## Test layout and exclusions

- All test sources live next to production code under `**/_test/` packages, **not** in `src/test/java`. The `src/pom.xml` rebinds `testSourceDirectory` to the same root and includes only `**/_test/*.java`. Production compilation explicitly **excludes** `**/_test/**`.
- Test resources are picked up only from `**/_test/resources/`, `**/simulation/resources/`, and `**/physicell/resources/`.
- A large block of `<exclude>` entries in `src/pom.xml` (under `maven-surefire-plugin`) lists tests that are intentionally skipped — broken, GUI-dependent, requires-display, or environment-dependent. **Before "fixing" a test that doesn't run, check whether it is on this exclusion list.** Categories are commented in the POM (outdated, GUI, broken, ChatGPT-generated, etc.).
- Surefire passes `--add-opens java.desktop/sun.java2d=ALL-UNNAMED --add-opens java.sql/java.sql=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED` and runs headless.
- JUnit version is **3.8.x** (`junit.framework.TestCase`-style), not JUnit 4/5.

## Architecture

BioUML is an OSGi-based platform for biological modeling, simulation, and pathway/data analysis. The `src/` module contains the core packages; everything else is a feature plugin.

### Package roots in `src/`
- **`biouml.model`** — core diagram model (`Diagram`, `DiagramElement`, `Node`, `Edge`, `Compartment`, `Module`, `SubDiagram`); `dynamics/` holds the mathematical model (variables, equations, events) used by simulators.
- **`biouml.standard`** — concrete diagram types (math, gene-network, metabolic-pathway, composite), editors, simulation utilities, state machines.
- **`biouml.workbench`** — desktop UI (Swing) host: application shell, perspectives, document/repository actions, diagram view parts.
- **`biouml.launcher.BioUMLLauncher`** — servlet entry point (`web.xml`); subclasses `ru.biosoft.server.tomcat.ConnectionServlet`, sets `biouml.server.path`, then delegates to `org.eclipse.equinox.launcher.Main` with `-application ru.biosoft.server.tomcat.empty` to bootstrap OSGi inside the servlet container.
- **`biouml.plugins.*`** — production code for each feature plugin. The matching `plugconfig/biouml.plugins.<name>/` directory holds its OSGi `MANIFEST.MF`, `plugin.xml`, `pom.xml`, and resources.
- **`ru.biosoft.*`** — supporting frameworks shared across features: `access` (data collections / repository), `analysis*` (analysis framework), `bsa` (biosequence analysis & tracks), `graph`, `graphics`, `math`, `server`, `table`, `templates`, `workbench`. These are also exposed as OSGi bundles via `plugconfig/ru.biosoft.*/`.
- **`com.developmentontheedge.*`** — third-party-style framework code (BeanInfo / property editors) developed in-house and required by most plugins.

### Plugin model (important)
- A "plugin" is split between **two directories**: source under `src/biouml/plugins/<name>/` and OSGi/Maven config under `plugconfig/biouml.plugins.<name>/`. Bundle metadata (dependencies, exported packages) lives in the latter's `META-INF/MANIFEST.MF`; runtime extensions (solvers, engines, document factories, view parts, repository actions) are declared in `plugin.xml` against extension points such as `biouml.plugins.simulation.solver`, `biouml.plugins.simulation.engine`, `biouml.workbench.diagramViewPart`, `ru.biosoft.access.repositoryActionsProvider`, `ru.biosoft.workbench.documentFactory`.
- When adding/renaming code, update **both** sides: the Java package under `src/` and the bundle's `MANIFEST.MF` (`Require-Bundle`, `Export-Package`) and `plugin.xml`. Maven alone won't catch missing OSGi wiring.
- Each plugin's `pom.xml` depends on the core `org.biouml:src` artifact and is otherwise nearly identical — copy an existing one when adding a plugin.

### Build / packaging modules
- **`src/`** — compiles all core + plugin Java into a single `src` jar (the OSGi split is logical, not physical, at build time).
- **`war-build/`** — assembles `biouml.war` and `bioumlweb.war`.
- **`tomcat-embedded/`** — standalone runner (`org.biouml.tomcat.Embedded`); `exec:java` runs in-process, `exec:exec@run-forked` runs a separate JVM with the required `--add-opens java.base/java.net=ALL-UNNAMED`.

### Data directories
The launcher (and `run.sh`) wires these as repository roots: `./data`, `./data_resources`, `./users`, `./history`, `./analyses`. MySQL connection settings for the support database default to those in `docker-compose.yaml` (`bioumlsupport2` / `bioumlsupport2`).
