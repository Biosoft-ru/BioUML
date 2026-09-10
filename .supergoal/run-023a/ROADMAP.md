# ROADMAP — BioUML MCP Service

**Goal:** An MCP (Model Context Protocol) service inside BioUML that lets AI agents (Claude, ChatGPT, custom) drive the complete BioUML platform — repository browsing, context-menu actions, analyses (BioUML + geneXplain-compatible), diagrams, and simulations — instead of manual UI work.

**Branch:** `mcp` (created in phase 1 from `main`)
**Transport:** Both — in-process MCP server (OSGi-embedded agents) + streamable-HTTP servlet at `/mcp` on the embedded Tomcat (external clients). Auth: existing BioUML web session.
**Stack:** Java 21, Maven multi-module + Ant (both must compile), OSGi (Equinox), JUnit 3.8.x under `**/_test/`.

## Key architectural anchors (from recon)
- Context-menu actions = `ru.biosoft.access.repository.PluginActions.getActions(DataElement)`; stable IDs = `ActionCommandKey`; item identity = `DataElementPath` via `CollectionFactory.getDataElement(path)`.
- Analyses = `ru.biosoft.analysiscore` (`AnalysisMethodInfo.createAnalysisMethod()` → params bean → `validateParameters()` → `TaskManager.addAnalysisTask(...)` async or `justAnalyzeAndPut()` sync); parameter schemas auto-generated from BeanInfo/`ComponentFactory`.
- geneXplain compatibility = go through `AnalysisMethodRegistry` abstraction (geneXplain's `ConfiguredAnalysisMethodRegistry` + `analyses/methods.dat` plug in unmodified).
- Web plumbing = `ru.biosoft.server` (ServletRegistry/AbstractJSONServlet pattern); diagrams = `biouml.model` + `biouml.plugins.simulation`.

## Phases

### Phase 1 — Scaffold MCP plugin + spike transport
- **Deliverables:** `mcp` branch; `src/biouml/plugins/mcp/` (packages `server`, `tools`, `web`, `support`); `plugconfig/biouml.plugins.mcp/` (pom, MANIFEST.MF, plugin.xml); `McpConstants`; `McpPluginSmokeTest`; `.supergoal/run-023a/transport-decision.md`; in-process ping spike test.
- **Acceptance criteria:** 8 (see phase-1.md) — branch exists, both build sides present, both builds green, smoke test green, transport decision recorded with passing spike.
- **Depends on:** none

### Phase 2 — Repository tool layer
- **Deliverables:** `McpRepositorySupport`, `McpActionInvoker`; tools `biouml_repo_collections|list|describe|search|get_actions|run_action|create_folder|remove|import|export`; `McpRepositoryToolsTest` with temp-repo fixture.
- **Acceptance criteria:** 9 (see phase-2.md) — actions list = UI menu (ActionCommandKeys), run_action mutates fixture safely, GUI-only actions refused with `requires_interactive_ui`, uniform JSON envelope.
- **Depends on:** 1

### Phase 3 — Analysis engine
- **Deliverables:** `McpAnalysisSupport`; tools `biouml_analysis_list|describe|run|repeat|get_result`, `biouml_task_status|cancel|list`; stub-analysis fixture; `McpAnalysisToolsTest`, `McpAnalysisRegistryCompatTest`.
- **Acceptance criteria:** 10 (see phase-3.md) — auto-generated parameter schemas; sync + async run paths; task lifecycle to done; structured validation errors; geneXplain-registry compatibility.
- **Depends on:** 2 (envelope + path-resolution conventions)

### Phase 4 — Diagram tools + simulation
- **Deliverables:** `McpDiagramSupport`; tools `biouml_diagram_create|describe|addNode|addEdge|removeElement|moveElement|renameElement|save|import|export`, `biouml_diagram_node_types`, `biouml_simulation_run|result|solvers`; dynamic-model editing; tests with temp-repo fixtures.
- **Acceptance criteria:** 9 (see phase-4.md) — create→edit→describe round-trip, DML save/import round-trip, solver list from extension point, minimal simulation run or documented fallback.
- **Depends on:** 2 (envelope + path conventions)

### Phase 5 — HTTP/SSE endpoint + in-process server + auth
- **Deliverables:** `BioUmlMcpServer` + `McpServerFactory`; `McpServlet` at `/mcp` (streamable-HTTP, protocol 2025-03-26); BioUML session auth (401 unauthenticated); war wiring; `McpServletTest`, `McpInProcessServerTest`.
- **Acceptance criteria:** 10 (see phase-5.md) — real-HTTP initialize/list/call cycle, 401 path, in-process parity with HTTP tool list, servlet present in `bioumlweb.war`, full reactor + ant green.
- **Depends on:** 1, 2, 3, 4

### Phase 6 — Polish, harden, document, e2e
- **Deliverables:** `McpE2EServletTest` (raw-HTTP or real-client e2e); input-validation + path-escape + allowlist hardening; truncation/perf caps; request logging; README (both plugin dirs) with generated tool table; full-suite regression check.
- **Acceptance criteria:** 12 (see phase-6.md) — e2e transcript, no stack traces in responses, escape/denial handling, truncation + perf asserted, README table == live catalog, no new full-suite failures, both builds green, no debug prints.
- **Depends on:** 5

## Verification (final)
- `mvn -q package -DskipTests` (full reactor) exit 0
- `cd src && ant compile` exit 0
- `mvn -pl src test` — no new failures vs baseline
- E2E: MCP client (real or raw-HTTP) completes initialize → tools/list (≥10 tools) → mutating call → verify
