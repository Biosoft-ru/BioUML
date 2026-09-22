# BioUML MCP Service

BioUML exposes its platform functionality as [Model Context Protocol](https://modelcontextprotocol.io/) (MCP)
tools, so that AI agents (Claude Desktop, the Claude CLI, or any MCP client) can drive the platform
programmatically instead of through the user interface: browse and mutate the repository, run
analyses (synchronously or as queued tasks), build and simulate diagrams.

The server is a plain `javax.servlet` endpoint (the repo is Tomcat 6 / `javax`, not `jakarta`),
mapped at **`/mcp`** on the embedded Tomcat web app. It speaks MCP streamable-HTTP
(protocol `2025-03-26`) with a plain-JSON (non-SSE) transport: POST a JSON-RPC request, get a
JSON-RPC response. There is also an in-process server (`biouml.plugins.mcp.server.BioumlMcpServer`)
for in-JVM agents that can call the platform directly.

## Quick start

### 1. Run the web app

```sh
# MySQL must be running first (see the repo README / docker-compose.yaml).
mvn -pl tomcat-embedded exec:java
# → http://localhost:8080/bioumlweb/
```

The MCP endpoint is `http://localhost:8080/bioumlweb/mcp`.

### 2. Authentication (BioUML web session)

The MCP endpoint reuses the BioUML web session — **no new auth system**. You need a session
exactly as you would for the web UI:

- Log in through the BioUML web UI (or your usual session-issuing flow) and use that
  `JSESSIONID` cookie, **or**
- Pass the session id explicitly as a query parameter: `?sessionId=<sessionId>`.

The special **`system`** session (the server's own identity) is privileged and is used for
headless/automated calls. Any other session must belong to a logged-in user; unauthenticated
requests are refused with HTTP **401** and a structured JSON-RPC error body.

### 3. Connect an MCP client

**Claude Desktop** (`claude_desktop_config.json`) — for a server you run as a local process, point
the stdio bridge at your server; for this HTTP endpoint, use any streamable-HTTP MCP client:

```jsonc
{
  "mcpServers": {
    "biouml": {
      "url": "http://localhost:8080/bioumlweb/mcp?sessionId=<your-session-id>"
    }
  }
}
```

**Claude CLI** (`claude mcp add`):

```sh
claude mcp add --transport http biouml "http://localhost:8080/bioumlweb/mcp?sessionId=<your-session-id>"
```

A minimal raw-HTTP exchange (JSON-RPC):

```bash
# initialize
curl -s -X POST "http://localhost:8080/bioumlweb/mcp?sessionId=$SID" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'

# tools/list
curl -s -X POST "http://localhost:8080/bioumlweb/mcp?sessionId=$SID" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# tools/call — list repository collections
curl -s -X POST "http://localhost:8080/bioumlweb/mcp?sessionId=$SID" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"biouml_repo_collections","arguments":{}}}'
```

## Tool reference

<!-- BEGIN GENERATED TOOL TABLE — do not edit by hand; McpReadmeTableTest asserts this
     table's row count equals the live catalog. Regenerate by running that test. -->
| Tool | Description |
|------|-------------|
| `biouml_repo_collections` | List the top-level registered repository collections (databases, data, analyses, ...). |
| `biouml_repo_list` | List the children of a repository collection. |
| `biouml_repo_describe` | Describe a repository element (name, class, child count, children). |
| `biouml_repo_search` | Search element names (case-insensitive substring) under a scope (default: all roots). |
| `biouml_repo_get_actions` | List the context-menu actions available for an element (key, name, description, headlessSafe). |
| `biouml_repo_run_action` | Run a context-menu action (by ActionCommandKey) on an element headlessly. Interactive-only actions return requires_interactive_ui. |
| `biouml_repo_create_folder` | Create a new folder inside a folder-collection parent. |
| `biouml_repo_remove` | Remove an element by path. dryRun=true reports what would be removed without changing anything. |
| `biouml_analysis_list` | List all available analysis methods grouped by their analyses group. |
| `biouml_analysis_describe` | Describe an analysis method: its parameter input schema (name, type, default, description), input names, and output names. |
| `biouml_analysis_run` | Run an analysis. params is a flat map of bean-property values (nested keys use '/' separators; path-typed properties take repository path strings). sync=true runs inline and returns the result; otherwise it is queued and returns {taskId,status}. |
| `biouml_task_status` | Report a task's status (queued/running/paused/done/error/cancelled), progress (0-100), and result paths. |
| `biouml_task_cancel` | Cancel a queued or running task. |
| `biouml_task_list` | List active and recent tasks (id, type, status). |
| `biouml_analysis_repeat` | Re-run a completed analysis from its stored output collection (reads the stored analysisName + parameters and runs again). |
| `biouml_analysis_get_result` | Read an analysis output collection: for a table, column names/types and the first N rows; for a folder, its children. |
| `biouml_diagram_create` | Create a new diagram in a target collection. type is 'math' (default) or 'pathway'. Returns the new diagram's path. |
| `biouml_diagram_describe` | Describe a diagram: element count, per-element summaries (id, name, kind, position, node type) and — when a dynamic model is present — its variables and equation count. |
| `biouml_diagram_node_types` | List the node types that biouml_diagram_add_node accepts. |
| `biouml_diagram_add_node` | Add a node to a diagram. Returns the assigned element id. nodeType is one of the keys from biouml_diagram_node_types (default 'Stub'). |
| `biouml_diagram_add_edge` | Add an edge between two nodes. Returns the assigned edge id. |
| `biouml_diagram_remove_element` | Remove an element (node or edge) from a diagram by its id. |
| `biouml_diagram_move_element` | Move a node to a new (x, y) position. |
| `biouml_diagram_rename_element` | Rename an element's display title. |
| `biouml_diagram_save` | Save a diagram to a DML file on disk (in targetDir, or the current directory). Returns the file path and size. |
| `biouml_diagram_export` | Export a diagram to a file (DML by default). Returns the file path and size. |
| `biouml_diagram_import` | Import a diagram file (DML by default) into a target collection. Returns the new element's path. |
| `biouml_simulation_run` | Run a headless ODE simulation of a diagram and return its time series. The diagram must have a dynamic model with at least one rate (ODE) equation; otherwise a missing_dynamic_model error is returned. Times and the solver are optional. |
| `biouml_simulation_list_solvers` | List the ODE solvers available to the simulation engine (name, type, implementation class). |
| `biouml_project_list` | List the current user's projects with their permissions (admin/canWrite/canDelete) and disk quota. |
| `biouml_project_create` | Create a new project (a SQL-backed research collection) for the current user. The name may only contain latin letters, numbers, spaces and a few symbols. |
| `biouml_project_delete` | Delete a project (and its data) for the current user. Requires delete permission. |
| `biouml_project_size` | Report the disk usage (in bytes) of a single project's data folder. |
| `biouml_user_info` | Return the current user's profile (username and profile fields) from the security provider. |
| `biouml_user_change_password` | Change the current user's password. Requires the current (old) password and a new one. |
| `biouml_user_change_info` | Update the current user's profile fields (e.g. name, email) in the security provider. Pass a flat object of field name -> value. |
<!-- END GENERATED TOOL TABLE -->

## Running an analysis (list → describe → run → poll → get_result)

```jsonc
// 1. What analyses are available?
{"method":"tools/call","params":{"name":"biouml_analysis_list","arguments":{}}}

// 2. What parameters does one take?
{"method":"tools/call","params":{"name":"biouml_analysis_describe","arguments":{"name":"<method>"}}}

// 3. Run it asynchronously (returns {taskId, status}).
{"method":"tools/call","params":{"name":"biouml_analysis_run",
   "arguments":{"name":"<method>","sync":false,"params":{"input":"<path>","iterations":"100"}}}}

// 4. Poll until done.
{"method":"tools/call","params":{"name":"biouml_task_status","arguments":{"taskId":"<taskId>"}}}

// 5. Read the result (table columns + first rows, or a folder's children).
{"method":"tools/call","params":{"name":"biouml_analysis_get_result","arguments":{"path":"<resultPath>"}}}
```

## Example: build a diagram and simulate it

```jsonc
{"method":"tools/call","params":{"name":"biouml_diagram_create","arguments":{"parentPath":"mydata/diagrams","name":"decay","type":"math"}}}
{"method":"tools/call","params":{"name":"biouml_diagram_add_node","arguments":{"diagramPath":"mydata/diagrams/decay","name":"X","nodeType":"Substance"}}}
{"method":"tools/call","params":{"name":"biouml_diagram_save","arguments":{"diagramPath":"mydata/diagrams/decay","targetDir":"/tmp"}}}
{"method":"tools/call","params":{"name":"biouml_simulation_run","arguments":{"diagramPath":"mydata/diagrams/decay","t0":"0","tf":"10","inc":"0.1"}}}
```

## Limitations

- **Interactive-only actions** — `biouml_repo_run_action` only invokes actions classified as
  headless-safe. Actions that open documents or show input dialogs (e.g. Open, Login, most
  "New …" wizards) return `requires_interactive_ui` instead of running. The classification is
  default-deny: an action is headless-safe only if it is on an explicit allowlist.
- **Async semantics** — `biouml_analysis_run` with `sync:false` queues the analysis and returns a
  task id; poll with `biouml_task_status` and read the output with `biouml_analysis_get_result`.
  There is no push/notification channel on this transport — clients poll.
- **Headless simulation** — `biouml_simulation_run` requires the diagram to have a dynamic model
  with at least one ODE rate equation. In a plain headless JVM the ODE code-generation (Velocity
  template) may not be available, in which case the tool returns a `missing_dynamic_model` error
  rather than a time series; on a provisioned OSGi/Tomcat runtime it returns the series.
- **Result size caps** — list/describe tools cap their output (children ≤ 100, table rows ≤ 50,
  describe payloads ≤ 200 KB) and set a `truncated:true` flag when they do.

## Security

- **Auth** — the endpoint requires a valid BioUML web session (see *Quick start*). Unauthenticated
  requests → HTTP 401. The privileged `system` session is the server's own identity.
- **Input validation** — every tool validates its arguments at the boundary. A missing required
  argument or a wrong-typed argument returns a structured `invalid_params` envelope; a bad
  repository path returns `not_found` / `path_escape`. Unknown tools return JSON-RPC `-32601`.
- **Path containment** — repository path resolution is restricted to the registered repository
  roots. A path that escapes them (e.g. `../../etc/passwd`) returns `path_escape` and no file
  outside the roots is read.
- **No stack-trace leakage** — all failures are returned as structured `{ok:false, code, error}`
  envelopes. Exception details are logged server-side (via `java.util.logging`, method/tool/
  duration/status — no PII or request bodies), never sent to the client.

## Development

- Sources: `src/biouml/plugins/mcp/` (tools, support, server, web).
- OSGi/bundle metadata: `plugconfig/biouml.plugins.mcp/` (this directory's `plugin.xml`,
  `META-INF/MANIFEST.MF`, `pom.xml`).
- Tests: `src/biouml/plugins/mcp/_test/` — `Mcp*Test` covers the repository/analysis/diagram/
  simulation tools, the HTTP + in-process transports, the end-to-end journey, and the hardening
  (malformed input, path-escape, denied action, truncation, performance). `McpReadmeTableTest`
  keeps this file's tool table in sync with the code.
