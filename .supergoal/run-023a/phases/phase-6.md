SUPERGOAL_PHASE_START
Phase: 6 of 6 — Polish, harden, document, e2e
Task: End-to-end verification with a real client, security/input hardening, docs, and final polish
Mandatory commands: mvn -q package -DskipTests; cd src && ant compile; mvn -pl src test -Dtest='Mcp*Test'
Acceptance criteria: 12
Evidence required: real MCP client transcript (initialize→tools/list→tools/call); malformed-input handling transcript; README rendered content; full test suite result; both-build tails
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests` (full reactor) → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest='Mcp*Test'` → exit 0
4. `mvn -pl src test` (full suite) → no new failures vs baseline

## Evidence required
- E2E transcript (raw HTTP or real client): initialize → tools/list → collections → analysis_list → create-folder → re-list verification
- 3 malformed-input responses (no stack frames in body)
- Path-escape + denied-action error responses
- Truncation + perf assertion outputs
- README tool-table count == catalog count test output
- Full `mvn -pl src test` surefire summary (numbers vs baseline)
- grep proof: 0 `System.out.println` in `src/biouml/plugins/mcp/**`

## Why
The mandatory Polish & Harden phase: "complete usage via MCP instead of UI" only counts if a real client (Claude Desktop / any MCP client) can drive it end-to-end, inputs are validated, errors are structured, and a developer can connect in 5 minutes from the README.
## Work
1. **End-to-end with a real MCP client**:
   - If an MCP CLI/client is available in the environment (e.g. `npx @modelcontextprotocol/inspector`, a Python `mcp` client, or the Claude Desktop config): launch the web app (`mvn -pl tomcat-embedded exec:java` with the fixture data dirs), connect a client to `http://localhost:8080/bioumlweb/mcp`, and capture: initialize, tools/list (full list), tools/call `biouml_repo_collections`, tools/call `biouml_analysis_list`, and one mutating call (create folder via `biouml_repo_run_action` on the test data dir) with verification via re-list.
   - If no client binary is installable offline: write `McpE2EServletTest` that scripts the same sequence with raw HTTP (JSON-RPC) — this test is mandatory either way — and mark the live-client attempt as best-effort evidence.
   - The web app needs MySQL per CLAUDE.md; if MySQL is unavailable, use the fixture/temp-repo path the tests use (embedded repository) and document that the e2e ran against the fixture, not the MySQL-backed repo.
2. **Input validation & error hardening**:
   - Every tool: validate argument types (path is string, ids are strings/ints, params are objects); reject unknown top-level args; catch all exceptions at the tool boundary → structured `{ok:false, code, error}` (never raw stack traces to the client; log them server-side).
   - Path traversal / escape: `DataElementPath` resolution must stay within registered repository roots — assert a path like `../../etc/passwd` or a root-escape returns a structured error, not a file read.
   - `biouml_repo_run_action` allowlist review: final pass over the action classification (interactive vs headless-safe); anything that can mutate outside the repo (e.g. Install-database admin actions, anything touching system state) is denied with a clear error.
   - Size limits: list/describe tools cap results (children ≤100, table rows ≤50, describe JSON ≤200KB) with a `truncated:true` flag; assert in tests.
3. **Performance sanity**: `tools/list` and `biouml_repo_collections` must respond < 500ms on the fixture (assert in test with a timing check); `biouml_analysis_list` < 2s.
4. **Logging & observability**: request log line per tool call (method, tool, duration_ms, ok/error, user) via the standard `java.util.logging` pattern used in `ru.biosoft.server`; no PII/secret logging.
5. **Documentation**:
   - `src/biouml/plugins/mcp/README.md` (and a copy at `plugconfig/biouml.plugins.mcp/README.md`): what it is, quick start (server URL, auth, example Claude Desktop / claude CLI config snippet pointing at `http://host:8080/bioumlweb/mcp`), full tool reference table (name, description, params, example) auto-derived from the tool catalog, analysis-running example (list→describe→run→poll→get_result), limitations (interactive-only actions, async semantics), security notes.
   - Tool reference table must be GENERATED (a small test or build step that renders the catalog to markdown) so it can't drift from code — the test asserts the README's tool table row count == live catalog count.
6. **Test suite**: run the FULL `mvn -pl src test` (not just Mcp*Test) to confirm no regressions in the broader suite (some tests are on the exclusion list — that's expected; report the surefire summary). Also confirm the excluded-test list wasn't touched.
## Acceptance criteria
1. `McpE2EServletTest` passes: raw-HTTP initialize → tools/list (≥10 tools) → tools/call collections → tools/call analysis_list → mutating create-folder verified by re-list.
2. Live-client attempt: either a captured transcript from a real MCP client, or a documented note that no client was installable and the raw-HTTP test is the e2e evidence.
3. Malformed input: 3 distinct bad calls (unknown tool, wrong arg type, missing required arg) all return structured errors with no stack trace in the response body (assert body contains no `at com.` / `at ru.` frames).
4. Path-escape attempt returns structured error; no file outside repo roots is read (assert).
5. Denied action (an interactive/admin action via run_action) returns a clear denial error (assert).
6. Truncation: listing a large fixture collection returns `truncated:true` when > cap (assert with a fixture of >100 children).
7. Perf: tools/list < 500ms and analysis_list < 2s on fixture (asserted in test).
8. README exists in both locations; tool table row count equals live catalog count (test asserts).
9. README contains: quick-start config snippet, ≥3 example calls, limitations section, security section.
10. Full `mvn -pl src test` surefire summary: no NEW failures vs baseline (report the numbers; pre-existing excluded/skipped tests unchanged).
11. `mvn -q package -DskipTests` and `cd src && ant compile` both exit 0.
12. No `System.out.println` / debug prints left in `src/biouml/plugins/mcp/**` production code (grep count 0; use java.util.logging only).
## Notes
- If the full reactor build surfaces a pre-existing failure unrelated to MCP, report it with evidence (git stash + rerun) rather than "fixing" unrelated code — scope creep is forbidden.
- The e2e test must be deterministic and headless; cap its runtime at 5 minutes.
- README quick-start must state the auth model (BioUML session) and how to get a session (same as the web UI login).
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END