# BioUML Agent Instructions

## Build & Run

```sh
# One-time setup: install private JARs not in Maven Central
./install_all_old_jars.sh

# Build (skip tests for speed)
mvn package -DskipTests

# Run web edition (requires MySQL via docker-compose.yaml)
mvn -pl tomcat-embedded exec:java

# Or forked launch (Java 17+)
mvn -pl tomcat-embedded exec:exec@run-forked
```

## Testing

```sh
# Requires R: sudo apt install r-base r-base-dev
mvn -pl src test

# Single test (matches simple class name)
mvn -pl src test -Dtest=DiagramXmlReaderTest
```

- Tests live in `**/_test/` packages next to production code (NOT `src/test/java`)
- JUnit 3.8.x (`junit.framework.TestCase` style)
- Many tests are excluded in `src/pom.xml` (broken, GUI-dependent, requires-display)
- Runs headless: `java.awt.headless=true`

## Architecture

- **OSGi-based** platform split between:
  - `src/` — Java source code
  - `plugconfig/` — OSGi bundle metadata (`MANIFEST.MF`, `plugin.xml`)
- Plugin code needs updates to BOTH directories
- Entry points: `biouml.launcher.BioUMLLauncher` (servlet), `ru.biosoft.server.tomcat.ConnectionServlet`

## Dependencies

- **Java 21** required (compiler target/release)
- **MySQL** for data repository (`docker-compose.yaml` brings up `bioumlsupport2`)
- No lint/typecheck tools configured

## Performance / profiling

- **Do not hand-unroll tight elementwise numeric loops** (e.g. `z[i] = a*x[i] + b[i]`, `x[i] *= c`). C2's auto-vectorizer (SuperWord) already SIMD-vectorizes them; a manual unroll-by-N *defeats* it and measured ~3× **slower** on JDK 21 (JVode `VectorUtils` loops — PR #39/#43/#44).
- A profiler sample count is **not** proof a change is faster. Benchmark loop "optimizations" with the JMH harness in `benchmarks/` (same JDK/CPU as the server, at real vector sizes) before claiming a win.