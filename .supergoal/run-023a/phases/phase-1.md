SUPERGOAL_PHASE_START
Phase: 1 of 6 — Scaffold MCP plugin + spike transport
Task: Create the biouml.plugins.mcp plugin skeleton (both build sides), create the mcp branch, and spike the MCP Java SDK transport choice
Acceptance criteria: 8
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest=McpPluginSmokeTest` → exit 0
4. `git branch --show-current` → `mcp`

## Evidence required
- `git branch --show-current` prints `mcp`
- `find src/biouml/plugins/mcp plugconfig/biouml.plugins.mcp -type f` listing (≥10 files)
- mvn + ant build log tails showing BUILD SUCCESS / exit 0
- Smoke test surefire output
- `cat .supergoal/run-023a/transport-decision.md` (chosen transport + rationale)

## Why
Everything else depends on (a) a plugin home that both build systems accept, and (b) a proven transport. This phase de-risks the single biggest unknown — the MCP Java SDK (`io.modelcontextprotocol.sdk:mcp`, Java 17+, fits our Java 21) availability in the offline Maven world and its integration with the embedded Tomcat servlet context.
## Work
1. `git checkout -b mcp` from main (working tree must be clean of unrelated changes).
2. Create the plugin on BOTH sides (CLAUDE.md rule — Maven alone won't catch missing OSGi wiring):
   - `src/biouml/plugins/mcp/` — Java package root. Add sub-packages `server` (MCP server impl), `tools` (tool catalog), `web` (servlet), `support` (shared).
   - `plugconfig/biouml.plugins.mcp/` — copy an existing simple plugin's structure (e.g. `plugconfig/biouml.plugins.test/` or similar): `pom.xml` (depends on `org.biouml:src`), `META-INF/MANIFEST.MF` (Bundle-SymbolicName biouml.plugins.mcp; Require-Bundle: org.biouml.src + equinox http; Import-Package as needed), empty `plugin.xml` with extension declarations to be filled in later phases.
3. Add a trivial `McpConstants` class + a `_test/` smoke test (`McpPluginSmokeTest`, JUnit 3 style) asserting the plugin class loads and constants resolve.
4. **Transport spike** (the critical decision):
   - Check local `~/.m2/repository/io/modelcontextprotocol/sdk/` and whether `mvn dependency:get -Dartifact=io.modelcontextprotocol.sdk:mcp:<latest>` resolves in this environment.
   - If resolvable: wire the dependency into the plugin pom; write a minimal in-process `McpServer` with ONE dummy tool (`mcp_spike_ping`) using `McpServerFeatures`/ToolCallback, with stdio transport; unit-test the initialize + tools/list + tools/call JSON-RPC cycle in-memory (no network).
   - If NOT resolvable (offline): implement the fallback — a small hand-rolled JSON-RPC 2.0 layer over the existing `ru.biosoft.server.AbstractJSONServlet` pattern (MCP streamable-HTTP = JSON-RPC over POST + optional SSE; protocol version "2025-03-26"; methods: `initialize`, `notifications/initialized`, `tools/list`, `tools/call`). Document the decision in the spike notes.
   - Either way: record the chosen transport + coordinates in `.supergoal/run-023a/transport-decision.md` and print the conclusion.
## Acceptance criteria (all must hold)
1. Branch `mcp` exists and HEAD is on it.
2. `src/biouml/plugins/mcp/` exists with ≥4 sub-packages and ≥3 Java files.
3. `plugconfig/biouml.plugins.mcp/` exists with `pom.xml`, `META-INF/MANIFEST.MF`, `plugin.xml`.
4. Root `pom.xml` lists the new module (if modules are explicit) OR module is auto-included — verified by `mvn -q validate` at root picking it up.
5. `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` exits 0.
6. `cd src && ant compile` exits 0.
7. `mvn -pl src test -Dtest=McpPluginSmokeTest` exits 0.
8. `transport-decision.md` exists stating chosen SDK version OR fallback rationale, and the spike test (in-process ping) passes.
## Notes
- JUnit is 3.8.x (junit.framework.TestCase), tests under `**/_test/`.
- Do NOT commit anything yet; branch creation + working tree is enough for this phase (commits happen in a later phase).
- The SDK spike test must run headless (no GUI, no real network).
- **Ant baseline:** pre-flight proved `cd src && ant compile` PASSES as-is under JDK 21 (the 8 pattern-matching errors only appear when Ant runs on the shell-default JDK 8). No wdl source fix is needed — the criterion is simply that Ant runs under JDK 21 (criterion 0 in Mandatory commands).
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END