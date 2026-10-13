# THINKING — BioUML MCP Service

## Goals
Provide a Model Context Protocol (MCP) service inside BioUML so AI agents (Claude, ChatGPT, custom) can use the full BioUML platform programmatically — everything the user does via repository context menus, diagram editing, and running analyses — without a human driving the UI.

## User decisions (from Stage 1)
1. **Transport: Both** — in-process MCP server (OSGi-usable) AND HTTP/SSE servlet on embedded Tomcat for external ChatGPT/Claude Desktop.
2. **Scope: Full** — repository context-menu actions (databases, data, analyses/tasks) + diagram/document CRUD + running analyses.
3. **Ecosystem: BioUML + geneXplain** — expose BioUML-native analyses; extension point shaped so geneXplain `biouml2_based` analyses plug in via the same interface.
4. **Auth: reuse BioUML session** — MCP HTTP endpoint authenticates like existing web services.

## Competitive context (smarts.bio, 2026-09-10)
SMARts is an AI-driven bioinformatics platform: central agent searches databases/literature and executes multi-step pipelines; natural-language bioCHAT; 80+ tools (GATK, STAR...); FASTA/BAM/VCF/PDB viewers with AI inline analysis; integrated via VS Code/Jupyter/CLI; REST API + Python/Node SDKs; **MCP support for ChatGPT/Claude/Gemini/Cursor**. Takeaway: the market-standard shape is exactly what the user asked for — an MCP layer over a bioinformatics backend with data upload, tool execution, results-as-figures. Our MCP tool surface should mirror: `upload_data`, `list_databases`, `describe_analysis`, `run_analysis`, `get_result`, `create_diagram`, `search_repository`, plus results rendered as tables/JSON (and where possible SVG/HTML snippets).

## BioUML architecture facts (recon, 2026-09-10)
- **Server plumbing**: `ru.biosoft.server` — `Service` interface (`processRequest(Integer command, Map data, Response out)`), `ServiceRegistry`, `ServletRegistry`, `AbstractJSONServlet`, `ServletExtension`, `ServerConnector`. Existing JSON-over-HTTP request/response pattern is the template for the MCP servlet.
- **Analysis core**: `ru.biosoft.analysiscore` — `AnalysisMethod` (abstract, has `AnalysisParameters` via BeanInfo/`com.developmentontheedge` ComponentModel), `AnalysisMethodInfo` (class + attributes + `createAnalysisMethod()`), `AnalysisMethodRegistry`/`AnalysisMethodReader`, `AnalysesGroup`/`AnalysesGroupRegistry` (groups of methods), `AnalysisTask` (runnable), `RunAnalysis` (meta-method that executes another method with a parameter table row). Parameters are bean properties introspectable via `ComponentFactory.getModel(params, Policy.DEFAULT, true)` → **we can auto-generate MCP tool input schemas from ComponentModel/BeanInfo**.
- **Repository**: `ru.biosoft.access` — `DataCollection`/`DataElement` hierarchy, repository actions via extension point `ru.biosoft.access.repositoryActionsProvider` (pending agent report for the full action list).
- **Diagram model**: `biouml.model` (`Diagram`, `DiagramElement`, `Node`, `Edge`) + `biouml.model.dynamics` (variables/equations/events); `biouml.plugins.server.DiagramService` shows an existing JSON service pattern for diagrams.
- **Build**: Java 21, Maven multi-module + Ant; tests in `**/_test/`, JUnit 3.8; new plugin = `src/biouml/plugins/<name>/` + `plugconfig/biouml.plugins.<name>/` (MANIFEST.MF + plugin.xml).

## Open Questions (assumed)
- MCP SDK choice: **MCP Java SDK (`io.modelcontextprotocol.sdk:mcp`)** is the canonical one; if offline repo lacks it, fallback is a hand-rolled JSON-RPC 2.0 implementation over `AbstractJSONServlet` (MCP is a thin protocol: initialize/tools/list/tools/call + optional resources/prompts). Assumption: check local Maven repo / internet availability in phase 1; hand-rolled fallback is low-risk since MCP over streamable-HTTP is JSON-RPC + SSE.
- Tool naming convention: `biouml_<domain>_<action>` (e.g. `biouml_repo_list`, `biouml_analysis_run`).
- Long-running analyses: analyses can take minutes — MCP tool call must return a task id + status-poll tool (`biouml_task_status`), matching how the UI's task monitor works.

## Risks
1. **MCP SDK dependency unavailable offline** → fallback hand-rolled JSON-RPC; phase 1 spike decides.
2. **Analysis parameter schemas are heterogeneous** (BeanInfo-driven, some dynamic) → generic schema generation from ComponentModel; expose raw parameter map as escape hatch.
3. **Dual build system** (Maven + Ant) — new bundle must compile under both; Ant flat-pass compiles all Java, so new package under `src/biouml/plugins/mcp/` is low-risk but MANIFEST/pom wiring must be right.
4. **Session auth through servlet** — MCP streamable-HTTP uses session headers; must verify cookie/session propagation in the Tomcat-embedded servlet context.
5. **Long-running analysis semantics** — naive synchronous call would time out; need task-async pattern.

## Dependencies / ordering
- Phase 1 (spike: SDK + JSON-RPC transport) gates everything.
- Repository action inventory (agent report) gates the tool-catalog design.
- DiagramService + access-core patterns are templates for the service layer.
- geneXplain compatibility is a design constraint (extension point), not a code dependency — validated in a later phase by compiling against biouml2_based where feasible.
