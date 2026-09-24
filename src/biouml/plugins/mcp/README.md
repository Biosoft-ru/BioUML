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

### 2. Authentication

Three interchangeable paths; the `Authorization` header wins when present:

- **BioUML web session** — the `JSESSIONID` cookie the web UI login created (or the session id
  explicitly as `?sessionId=<sessionId>`).
- **BioStore token (Basic)** — an `Authorization: Basic <base64>` header whose decoded value is
  `<username>::token:<uuid>`. The `:token:<uuid>` part is a credential issued by the BioStore auth
  server; BioUML passes it verbatim to the configured `SecurityProvider`, which performs the real
  validation. This is the path for clients that already hold a BioStore token (Claude Code's
  `.mcp.json`, third-party clients).
- **OAuth 2.1 bearer token** — an `Authorization: Bearer <token>` header with an access token minted
  by the MCP OAuth server (see *OAuth for remote clients* below).

The special **`system`** session (the server's own identity) is privileged and is used for
headless/automated calls. Any other session must belong to a logged-in user; unauthenticated
requests are refused with HTTP **401** — a real 401 status line plus a `WWW-Authenticate: Bearer
resource_metadata="…"` challenge (when the OAuth issuer is determinable) plus a structured
JSON-RPC error body.

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

### 4. OAuth for remote clients (Claude.ai custom connectors)

Remote MCP clients that cannot present a session or a BioStore token up front — notably **Claude.ai
custom connectors** — sign in through the MCP-spec OAuth flow: the client probes the endpoint,
receives the `401` + `WWW-Authenticate` challenge, fetches the protected-resource metadata
(RFC 9728) and the authorization-server metadata (RFC 8414), then completes an OAuth 2.1
authorization-code + PKCE (S256) exchange against the authorization server hosted at
`/biouml/oauth`:

```
client ──401+WWW-Authenticate──▶ /biouml/mcp
client ──GET .well-known/oauth-protected-resource──▶ /biouml/oauth/…
client ──GET .well-known/oauth-authorization-server──▶ /biouml/oauth/…
browser ──GET /authorize (user signs in)──▶ 302 redirect_uri?code=…&state=…
client ──POST /token (code + code_verifier)──▶ {access_token, "Bearer", expires_in}
client ──Authorization: Bearer <access_token>──▶ /biouml/mcp
```

**Configuration** (system properties on the server JVM; no clients configured ⇒ OAuth is off):

| Property | Meaning |
|---|---|
| `biouml.mcp.oauth.issuer` | Public base URL of the authorization server, e.g. `https://biouml2test.biouml.org/biouml/oauth`. When set it is used verbatim and wins over everything derived. Unset ⇒ derived from the request's public hostname: `X-Forwarded-Host` (first value) with `X-Forwarded-Proto` as the scheme when present, else the `Host` header → `https://<host>/biouml/oauth`. |
| `biouml.mcp.oauth.public.host` | Optional operator-declared public hostname (e.g. `biouml2test.biouml.org`). When set and the *derived* host differs from it, a one-time WARNING is logged — this surfaces the classic misconfiguration where the app sits behind a proxy that forwards an internal `Host` and would otherwise emit an unreachable internal URL in its OAuth metadata. Not required; purely a misconfiguration canary. |
| `biouml.mcp.oauth.clients` | Static client allow-list: `clientId=uri1\|uri2;clientId2=uri3`. Redirect URIs are matched **exactly** (open-redirect defense). No RFC 7591 dynamic registration. |

Example (test server, Claude connector as a public client):

```
-Dbiouml.mcp.oauth.issuer=https://biouml2test.biouml.org/biouml/oauth
-Dbiouml.mcp.oauth.clients=claude-connector=https://claude.ai/api/mcp/oauth/callback
```

**Client registration — two modes:**

- **Dynamic (RFC 7591, "Register automatically")** — the default for Claude and most MCP clients.
  The server advertises `registration_endpoint` in the RFC 8414 metadata; the client `POST`s
  `{"redirect_uris":[…]}` to `/biouml/oauth/register` and receives a server-generated
  `client_id` (and `client_id_issuer`). No pre-configuration is needed — this is the path Claude's
  *"Register automatically"* mode takes. Registrations are in-memory (single-JVM) and expire after
  24 h; the client re-registers on connect, which is the normal MCP pattern.
- **Static (allow-list)** — set `biouml.mcp.oauth.clients` to pre-register a known client id + its
  exact redirect URIs. Useful when a client presents a fixed id (the UI's *"Use your own OAuth
  client"* mode) or when you want to vet clients ahead of time. A dynamically-registered client
  takes precedence over the allow-list if the ids happened to collide (they won't — DCR ids are
  server-generated `mcp_cl_…`).

**Claude Web setup:** *Connectors → Add custom connector* → server URL
`https://biouml2test.biouml.org/biouml/mcp` → the UI discovers the OAuth flow from the 401,
registers a client via DCR, and lets the user sign in with their BioUML account (the `/authorize`
login form). With DCR there is nothing to configure on the server for Claude.

> **Behind a proxy:** Claude's checker probes the public URL, so the OAuth metadata it fetches must
> point at the *public* host, not the internal backend. The server prefers the proxy's
> `X-Forwarded-Host` / `X-Forwarded-Proto` over the `Host` header, so make sure your reverse proxy
> forwards those (most do by default). If it doesn't — or if you'd rather not rely on it — set
> `-Dbiouml.mcp.oauth.issuer=https://<public-host>/biouml/oauth` explicitly; that value always wins.
> Set `-Dbiouml.mcp.oauth.public.host=<public-host>` to get a log WARNING if the derived host ever
> diverges from what you expect.

**Token lifetime:** 1 hour (no refresh-token grant in v1 — re-running the flow is the renewal
path). Tokens and codes are in-memory, so the deployment must run a single JVM (biouml2test does);
a multi-node deployment needs a shared store.

## Tool reference

<!-- BEGIN GENERATED TOOL TABLE — do not edit by hand; McpReadmeTableTest asserts this
     table's row count equals the live catalog. Regenerate by running that test. -->
| Tool | Description |
|------|-------------|
| `biouml_repo_collections` | List the top-level registered repository collections (databases, data, analyses, ...). |
| `biouml_repo_list` | List the children of a repository collection. |
| `biouml_repo_describe` | Describe a repository element (name, class, child count, children). |
| `biouml_repo_search` | Search element names (case-insensitive substring) under a scope (default: all roots). Synchronous; returns fast on a responsive server but can exceed the client timeout on a large repository — for large or slow repositories use biouml_repo_search_async and poll biouml_repo_search_status. |
| `biouml_repo_search_async` | Run a repository search (same as biouml_repo_search) in the background and return a taskId immediately, instead of blocking. On a large/slow repository a synchronous search can exceed the client's request timeout, so: (1) call this to start the search, (2) poll biouml_repo_search_status with the taskId until status is 'done', (3) call biouml_repo_search again with the same query/scope to read the matches — by then the search has warmed the repository caches so it returns quickly. The background run itself does not store its matches. |
| `biouml_repo_search_status` | Report the status (queued/running/done/cancelled/error) of a background repository search queued via biouml_repo_search_async. |
| `biouml_repo_get_actions` | List the context-menu actions available for an element (key, name, description, headlessSafe). |
| `biouml_repo_run_action` | Run a context-menu action (by ActionCommandKey) on an element headlessly. Interactive-only actions return requires_interactive_ui. |
| `biouml_repo_create_folder` | Create a new folder inside a folder-collection parent. |
| `biouml_repo_remove` | Remove an element by path. dryRun=true reports what would be removed without changing anything. |
| `biouml_repo_copy_element` | Copy a single element (file, table, diagram, or any cloneable data element) to a new location — the headless equivalent of the web UI's 'Save a copy'. destPath is the full path of the new copy (its parent must exist and be writable); the last path segment is the new name. The source is left untouched. |
| `biouml_repo_copy_folder` | Copy a folder (its whole subtree) to a new location — the headless equivalent of the web UI's 'Copy folder'. Runs asynchronously as a background task and returns a taskId immediately; poll biouml_task_status with the taskId until it completes (a large folder copy can exceed the client's request timeout). |
| `biouml_repo_reinitialize` | Re-initialize a collection that previously failed to load — the headless equivalent of the web UI's 'Retry'/'Reinitialize' menu item. Returns the path and whether it is now valid. |
| `biouml_repo_export_formats` | List the export formats available for a repository element (in accept-priority order). Use this to discover what biouml_repo_export can produce for the element before calling it. |
| `biouml_repo_export` | Export a repository element to a file on the server's local filesystem — the headless equivalent of the web UI's 'Export' menu item. format is one of biouml_repo_export_formats (omit to use the highest-priority format); targetDir is a server-local directory (omit for the current dir) — the output file is the element name + the format suffix. Returns {file, format, bytes}. |
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
- **OAuth is single-JVM and has no refresh grant** — see *OAuth for remote clients*.

## Security

- **Auth** — the endpoint requires a valid BioUML web session, a BioStore token, or an OAuth
  bearer token (see *Quick start* and *OAuth for remote clients*). Unauthenticated requests →
  real HTTP 401 + `WWW-Authenticate` challenge. The privileged `system` session is the server's
  own identity.
- **Input validation** — every tool validates its arguments at the boundary. A missing required
  argument or a wrong-typed argument returns a structured `invalid_params` envelope; a bad
  repository path returns `not_found` / `path_escape`. Unknown tools return JSON-RPC `-32601`.
- **Path containment** — repository path resolution is restricted to the registered repository
  roots. A path that escapes them (e.g. `../../etc/passwd`) returns `path_escape` and no file
  outside the roots is read.
- **No stack-trace leakage** — all failures are returned as structured `{ok:false, code, error}`
  envelopes. Exception details are logged server-side (via `java.util.logging`, method/tool/
  duration/status — no PII or request bodies), never sent to the client.
- **OAuth hardening** — redirect URIs must match the allow-list exactly (no open redirect);
  authorization codes are one-time (no replay); `state` round-trips untouched (CSRF); PKCE is
  S256-only and compared in constant time; access tokens are opaque `SecureRandom` values; the
  login form HTML-escapes every reflected (client-controlled) value.

## Development

- Sources: `src/biouml/plugins/mcp/` (tools, support, server, web).
- OSGi/bundle metadata: `plugconfig/biouml.plugins.mcp/` (this directory's `plugin.xml`,
  `META-INF/MANIFEST.MF`, `pom.xml`).
- Tests: `src/biouml/plugins/mcp/_test/` — `Mcp*Test` covers the repository/analysis/diagram/
  simulation tools, the HTTP + in-process transports, the end-to-end journey, the OAuth flow
  (`McpOAuthServletTest`: discovery docs, authorize session/form paths, PKCE, one-time codes,
  redirect-binding negatives; `McpServletTest` adds the Bearer path + 401 challenges), and the
  hardening (malformed input, path-escape, denied action, truncation, performance).
  `McpReadmeTableTest` keeps this file's tool table in sync with the code.
