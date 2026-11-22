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

## Persistent project memory

The project shares persistent memory with Claude Code.

Memory directory: `.claude-memory/`

At the beginning of each task:
1. Read `.claude-memory/MEMORY.md`.
2. Identify memory files relevant to the task.
3. Read those files before making decisions or modifying code.
4. Respect recorded architectural decisions, branch rules,
   server constraints, and previously measured performance results.
5. Verify historical notes against current source code.

When a task produces durable new knowledge:
- Update the appropriate thematic memory file.
- Add new files to `MEMORY.md` when necessary.
- Do not overwrite unrelated memory.
- Do not store secrets or credentials.
- Do not commit personal memory files to the repository.

Memory files are shared with Claude Code. Changes to them
will be visible to both agents.

## Build verification and architecture constraints

- Java 21 is required.
- Both Maven and Ant builds must compile successfully after
  relevant source changes.
- Verify using:
  `mvn package -DskipTests && (cd src && ant compile)`
- BioUML uses Eclipse Equinox OSGi at runtime.
- Maven compilation alone does not validate OSGi wiring.
- Check `MANIFEST.MF` and `plugin.xml` when changing plugin
  dependencies, exported packages, or extension declarations.
- Before changing test configuration, check exclusions in
  `src/pom.xml`.
- Use the existing JUnit 3.8.x conventions.

## Shared persistent memory (Claude Code + Codex)

The shared project memory directory is `.claude-memory/`.
It is the same directory used by Claude Code auto memory.

### Reading

- At the beginning of every task, read `.claude-memory/MEMORY.md`.
- Read relevant topic files before making decisions.
- Treat memory as historical context, not as a substitute
  for verifying current source code.
- Respect documented branch rules, architectural decisions,
  and measured performance results.

### Writing

Both Claude Code and Codex are authorized to update this memory.

During a task, when durable information is discovered:
- Save important decisions, constraints, user corrections,
  and verified findings in the appropriate topic file.
- Create a new topic file if no suitable file exists.
- Update `MEMORY.md` when adding or renaming topic files.
- Keep `MEMORY.md` concise, with links to topic files.
- Do not save secrets, credentials, or temporary debugging output.
- Do not duplicate information already present.
- Preserve existing knowledge unless it is demonstrably obsolete.
- Record uncertainty instead of presenting assumptions as facts.

Before finishing a task:
- Review whether any durable knowledge should be saved.
- Update memory if appropriate.
- Do not create memory entries merely to document routine work.

### Concurrent access

- Other agents may update these files concurrently.
- Re-read the target file immediately before editing.
- Preserve unrelated changes.
- Do not overwrite a whole memory file using a stale copy.
- If concurrent edits conflict, report the conflict rather
  than silently discarding either version.

### Git

- Shared memory is private and outside the repository.
- Never stage or commit `.claude-memory` or its contents.