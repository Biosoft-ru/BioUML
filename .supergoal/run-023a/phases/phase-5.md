SUPERGOAL_PHASE_START
Phase: 5 of 6 — HTTP/SSE endpoint + in-process server + auth
Task: Expose the MCP server over streamable-HTTP on the embedded Tomcat (with BioUML session auth) and as an in-process server for OSGi-embedded agents
Mandatory commands: mvn -q package -DskipTests; cd src && ant compile; mvn -pl src test -Dtest='Mcp*Test'
Acceptance criteria: 10
Evidence required: curl transcript of initialize → tools/list → tools/call against the running servlet; unauthenticated 401 transcript; in-process server test output; full mvn+ant build tails
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests` (full reactor) → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest='Mcp*Test'` → exit 0

## Evidence required
- HTTP transcript: POST initialize → 200 serverInfo; POST tools/list → ≥10 tools; POST tools/call repo_collections → ok envelope
- 401 unauthenticated transcript (status + body)
- In-process server test output (same tool set)
- War contents check: `unzip -l <bioumlweb.war> | grep -i mcp`
- Full reactor mvn + ant build tails
- Full `Mcp*Test` surefire output

## Why
User chose **Both transports**: external AI agents (ChatGPT/Claude Desktop) connect via HTTP; in-JVM agents (e.g. a future geneXplain assistant running inside OSGi) use the in-process server. Auth reuses the BioUML web session like existing `ru.biosoft.server` services.
## Work
1. **In-process server** (`biouml.plugins.mcp.server.BioUmlMcpServer`):
   - Assembles the full tool catalog (repo tools phase 2 + analysis tools phase 3 + diagram/simulation tools phase 4) into the MCP server created in phase 1 (SDK or hand-rolled, per `transport-decision.md`).
   - `initialize` response: serverInfo name `biouml`, version from the plugin, capabilities `{tools: {listChanged: false}}`, protocol version per the chosen transport.
   - All tool callbacks route through the support classes; every result is the JSON envelope; exceptions are caught and returned as MCP tool errors (not thrown off the transport).
   - A `McpServerFactory.createForSession(sessionContext)` method so both the servlet and in-process callers get a configured server. The session context carries: repository roots, current user (for auth-scoped data), server path.
2. **HTTP servlet** (`biouml.plugins.mcp.web.McpServlet`):
   - Extend the established `ru.biosoft.server` servlet pattern (`AbstractJSONServlet` / `ServletExtension` + `ServletRegistry` registration) OR a plain `HttpServlet` registered via the OSGi HTTP whiteboard — pick whichever the phase-1 transport decision requires; register at context path `/mcp` (i.e. `http://host:8080/bioumlweb/mcp`).
   - **Streamable HTTP transport** (protocol 2025-03-26): POST JSON-RPC messages; respond `application/json` for requests, `text/event-stream` (SSE) for responses that need streaming (tools/call with long output) — or plain JSON responses if the SDK/fallback supports non-SSE responses (MCP streamable-HTTP allows either). GET → 405 unless used for an SSE open (per spec).
   - **Auth**: require the BioUML session (same mechanism `DiagramService`/other web services use — session attribute / cookie; check `ru.biosoft.server` for the existing auth check and reuse it). Unauthenticated → HTTP 401 with a JSON-RPC-style error body. No new auth system.
   - Request logging: method, tool name, duration, status (no secrets/params bodies by default).
3. **web.xml / launcher wiring**: the servlet must be reachable in the embedded Tomcat web app (`tomcat-embedded` + `war-build`). Follow however `ru.biosoft.server` servlets are currently mapped (check `web.xml` in `src/META-INF`/war-build and `ServletRegistry` dynamic registration) — if dynamic registration via `ServletRegistry` is the pattern, use it; otherwise add the mapping to the war's web.xml. Verify the mapping appears in the built `bioumlweb.war`.
4. **Tests** (`_test/`):
   - `McpServletTest`: stand up a `McpServlet` on a lightweight embedded HTTP server (use whatever the repo already uses for servlet tests — check `ru.biosoft.server._test` or `biouml.plugins.server._test` for the harness; if none, use `com.sun.net.httpserver` or Jetty if available in the build): POST `initialize` → 200 + serverInfo; POST `tools/list` → contains `biouml_repo_collections`, `biouml_analysis_list`, `biouml_diagram_create`; POST `tools/call` `biouml_repo_collections` → ok envelope. Unauthenticated request (no session) → 401.
   - `McpInProcessServerTest`: create the in-process server directly (no HTTP), run the JSON-RPC initialize/tools/list/tools/call cycle, assert same tool list as the servlet test.
   - Both tests headless.
## Acceptance criteria
1. `McpServletTest` passes: initialize/tools/list/tools/call all return 200 with correct MCP responses over real HTTP.
2. Tool list from the servlet contains ≥10 tools spanning repo/analysis/diagram namespaces.
3. Unauthenticated request → 401 (assert status + error body present).
4. `McpInProcessServerTest` passes: in-process initialize + tools/list + tools/call cycle works.
5. In-process and HTTP tool lists are identical (assert set equality in a test).
6. Servlet is registered at `/mcp` and present in the built `bioumlweb.war` (verify war contents: `unzip -l ... | grep McpServlet` or the web.xml mapping).
7. `mvn -q package -DskipTests` (full reactor) exits 0.
8. `cd src && ant compile` exits 0.
9. All `Mcp*Test` pass.
10. No stack traces leak into tool responses (errors are structured) — assert on a deliberately-bad tool call (e.g. unknown tool → JSON-RPC error code -32601 or envelope error).
## Notes
- Protocol version: use the latest stable the transport supports (2025-03-26 for streamable-HTTP). If the hand-rolled fallback is in use, implement the spec's method set minimally: initialize, notifications/initialized, tools/list, tools/call, ping.
- Do NOT break existing web services — the new servlet is additive; run the existing `ru.biosoft.server` tests if present to confirm no regression.
- The war-build module assembles `bioumlweb.war`; make sure the new bundle/classes are included in the war (check how `biouml.plugins.server` gets in).
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END