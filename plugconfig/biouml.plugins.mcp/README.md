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

The client authenticates the same way as §2 — the `Authorization` header (or a session) is what the
server checks. **A streamable-HTTP MCP client can't present a web-session id on its own**, so for
Claude Code / Claude Desktop / any other static client the credential to embed is the **BioStore
token via the `Authorization: Basic` header** (a web-session `?sessionId=` is only useful when you
already hold a live `JSESSIONID`, e.g. reusing the UI's logged-in session in a browser or `curl`).

**BioStore Basic (the static path for Claude Code / Claude Desktop).** The decoded Basic payload is
`<username>:<password>` where the *password* is the verbatim BioStore token
(`:token:<uuid>` — note the leading colon), so the full credential string is
`<username>::token:<uuid>` (three colons). Example:

```bash
# build the Basic header once (note the three colons in the payload)
CRED=$(printf '%s' '<username>::token:<uuid>' | base64 -w0)
echo "Authorization: Basic $CRED"
```

Claude Code (`.mcp.json` / `claude mcp add`) supports a static `Authorization` header:

```jsonc
{
  "mcpServers": {
    "biouml": {
      "url": "http://localhost:8080/bioumlweb/mcp",
      "headers": {
        "Authorization": "Basic <base64-of-username::token:uuid>"
      }
    }
  }
}
```

or:

```sh
claude mcp add --transport http --header "Authorization: Basic <base64-of-username::token:uuid>" \
  biouml "http://localhost:8080/bioumlweb/mcp"
```

> The two-colon form `username:token:uuid` (no empty user segment) is **rejected** — the server
> splits on the *first* `:`, so the token must be the password and the username must be present.

**Web session (only when you already hold a live `JSESSIONID`).** If a browser/UI session is
already logged in, you can point a client at the endpoint with that session id — this is the path
`curl` and the web UI use, not the path a standalone Claude client takes:

```jsonc
{
  "mcpServers": {
    "biouml": {
      "url": "http://localhost:8080/bioumlweb/mcp?sessionId=<your-session-id>"
    }
  }
}
```

**OAuth 2.1 (remote / Claude.ai connectors)** — see *OAuth for remote clients* below; the client
needs no static credential, it completes the flow against the `401` challenge.

A minimal raw-HTTP exchange (JSON-RPC), authenticated with a BioStore Basic header:

```bash
# initialize
curl -s -X POST "http://localhost:8080/bioumlweb/mcp" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Basic $CRED" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'

# tools/list
curl -s -X POST "http://localhost:8080/bioumlweb/mcp" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Basic $CRED" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# tools/call — list repository collections
curl -s -X POST "http://localhost:8080/bioumlweb/mcp" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Basic $CRED" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"biouml_repo_collections","arguments":{}}}'
```

## Tool reference

<!-- BEGIN GENERATED TOOL TABLE — do not edit by hand; McpReadmeTableTest asserts this
     table's row count equals the live catalog. Regenerate by running that test. -->
| Tool | Description |
|------|-------------|
| `biouml_perspective_list` | List the perspectives available to the current user — BioUML's task-oriented entry points (e.g. GTRD, Microbiome, Regulatory mutations) — and the one in effect for this session. Each has a description (declared, or taken from the perspective's start page), its repository roots, and entry points: the analyses, workflows and collections work in that perspective usually starts from (pass their paths to biouml_analysis_describe or biouml_repo_describe). The perspective in effect decides which import/export formats are offered; switch it with biouml_perspective_select. |
| `biouml_perspective_select` | Switch the perspective for this MCP session only (the user's saved web-UI perspective is not changed). name is one of biouml_perspective_list. From then on biouml_repo_import_formats, biouml_repo_export_formats and import autodetection follow the perspective's importer/exporter rules. Returns the perspective's description, repository roots, entry points and allow/deny rules, and — when the web UI files are found on the server — an intro block with what the perspective's start page offers users: more entry points (with an exists flag), analysisPresets (analysis path + parameter values to pass to biouml_analysis_run), importTargets (folders the page imports into), help links and a page summary. |
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
| `biouml_repo_copy_folder` | ASYNC — Copy a folder (its whole subtree) to a new location. This returns IMMEDIATELY with a jobID; the copy continues in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status with the jobID (with a short delay between calls) until its 'completed' field is true, (3) the copy is then done. Delegates to the platform's folder provider. |
| `biouml_repo_job_status` | ASYNC poll — Report the progress of a background job started by an async start-tool (e.g. biouml_repo_copy_folder, biouml_repo_run_script, biouml_repo_import). Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the work is not done until then. The 'message' field is the job's accumulated log up to this moment — it grows as the job runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's jobcontrol provider. jobID is the id returned by the start tool. Returns {jobID, status, progress, message, completed}. |
| `biouml_repo_reinitialize` | Re-initialize a collection that previously failed to load — the headless equivalent of the web UI's 'Retry'/'Reinitialize' menu item. Returns the path and whether it is now valid. |
| `biouml_repo_export_formats` | List the export formats available for a repository element (in accept-priority order). Use this to discover what biouml_repo_export can produce for the element before calling it. The list follows the exporter rules of the perspective in effect (biouml_perspective_list / biouml_perspective_select). |
| `biouml_repo_export` | Export a repository element to a file on the server's local filesystem — the headless equivalent of the web UI's 'Export' menu item. format is one of biouml_repo_export_formats (omit to use the highest-priority format); targetDir is a server-local directory (omit for the current dir) — the output file is the element name + the format suffix. Returns {file, format, bytes}. |
| `biouml_repo_import_formats` | List the import formats available for a target collection (in accept-priority order). Use this to discover what biouml_repo_import can read into the collection before calling it. The first entry is always 'autodetect'. The list (and autodetection in biouml_repo_import) follows the importer rules of the perspective in effect (biouml_perspective_list / biouml_perspective_select). |
| `biouml_repo_import` | ASYNC — Import a file from the server's local filesystem into a target collection. This returns IMMEDIATELY with a jobID; the import continues in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status (with a short delay) until 'completed' is true — the final status message is the path of the imported element. Delegates to the platform's import provider. parentPath is the target collection; file is a server-local file path; format is one of biouml_repo_import_formats (omit to autodetect — an ambiguous autodetect is a clean error listing the candidates); name is optional (defaults to the file name without extension). |
| `biouml_repo_script_types` | List the script types that can be created in a collection (JS, R, Java, ...) — the headless equivalent of the 'New JS script' / 'New R script' / 'New Java code' menu items. Returns one row per type with its 'type' id (what biouml_repo_new_script takes), title, and element class. Only types whose product is available on this server are listed. |
| `biouml_repo_new_script` | Create a new script element (JS, R, Java, ...) in a target collection — the headless equivalent of the web UI's 'New JS script' / 'New R script' / 'New Java code' menu items. parentPath is the target collection; type is one of biouml_repo_script_types; name is the new element's name; content is the initial script text (omit for a blank script). Returns {created, type}. |
| `biouml_repo_new_element` | Create a new element in a collection — the headless equivalent of the web UI's 'New X' menu items. kind is one of: 'table', 'test', 'workflow', 'research', 'notebook' (Jupyter), or a script type from biouml_repo_script_types (e.g. 'js', 'R', 'Java', 'Nextflow', 'WDL', 'math'). parentPath is the target collection; name is the new element's name; content is optional initial content (used for scripts/notebooks, ignored otherwise). Returns {created, kind}. |
| `biouml_repo_document_content` | Read the text content of a document element (script, notebook, text file, ...). Delegates to the platform's document provider. |
| `biouml_repo_save_document_content` | Write the text content of a document element (script, notebook, text file, ...). Delegates to the platform's document provider. path is the element to update; content is the new text. |
| `biouml_repo_run_script` | ASYNC — Run a script (JS, R, Java, ...). This returns IMMEDIATELY with a jobID; the script runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_repo_job_status (with a short delay) until 'completed' is true, (3) then call biouml_repo_script_result to fetch the output. Delegates to the platform's script provider. script is the source text; type is the script type (one of biouml_repo_script_types). |
| `biouml_repo_script_result` | ASYNC result — Fetch the output of a script job started by biouml_repo_run_script. Call it only AFTER biouml_repo_job_status reports 'completed' is true. Delegates to the platform's script provider. jobID is the id returned by biouml_repo_run_script. Returns the job's printed buffer, tables, images, and HTML. |
| `biouml_repo_detect_omics_type` | Detect the omics type (Transcriptomics, Genomics, ...) of a data element. Delegates to the platform's omicsType provider. |
| `biouml_repo_set_omics_type` | Set the omics type of a data element (e.g. Transcriptomics, Genomics). Delegates to the platform's omicsType provider. |
| `biouml_repo_git_enabled` | Report whether a collection has git version-control enabled (and git is available on the server). Delegates to the platform's git provider. |
| `biouml_repo_preferences` | Read the current session's preferences. Delegates to the platform's preferences provider. |
| `biouml_repo_filesystem_types` | List the element types available for a file inside a file-system collection — the headless equivalent of the web UI's 'Change element type' menu item. path is the element. Returns {path, count, types}. |
| `biouml_repo_set_filesystem_type` | Change the element type of a file inside a file-system collection — the headless equivalent of the web UI's 'Change element type' menu item (FileSystemCollection.setElementType). path is the element; type is the target type (one of biouml_repo_filesystem_types). Returns {changed, type}. |
| `biouml_repo_login` | Log into a credentials-protected collection — the headless equivalent of the web UI's 'Login' menu item (CredentialsCollection.processCredentialsBean). path is the collection; fields is a flat map of credential field name -> value (e.g. {"user":"...","password":"..."}) matched case-insensitively to the collection's credentials bean. Returns {loggedIn, path}. |
| `biouml_repo_log` | Fetch the current session's server log tail — the headless equivalent of the web UI's 'Logs' view pane. Returns the in-memory java.util.logging capture for this session (a bounded, SEVERE-by-default buffer of everything the JVM logged under it — cross-job server output such as unhandled exceptions, NOT a single job's log). Diagnostic only: returns an error if the experimental logging feature was not enabled at server boot. Delegates to the platform's log provider. |
| `biouml_table_add_row` | Add a new row to a table — the headless equivalent of the web UI's 'Add row' menu item (TableDataCollectionUtils.addRow + finalizeAddition + save). path is the table; name is the new row's name (a duplicate name is a clean error); the row is initialised with each column's default value. |
| `biouml_table_replace` | Replace string content within a table's cells — the headless equivalent of the web UI's 'Replace content' menu item. path is the table; from is the search string, to is the replacement (both required; from==to or empty from is a clean error); exactMatch (default false) makes the match whole-cell instead of substring; selectionOnly limits the operation to rows listed in rows (omit to operate on the whole table). Returns the number of changed rows. |
| `biouml_table_subset` | Create a subset (copy) of a table containing only the given rows — the headless equivalent of the web UI's 'Subset table' menu item (TableRowsExporter.exportTable). path is the source table; destinationPath is the full path of the new table to create (its parent must exist); rows is the list of row names to include. Returns the new table's path. |
| `biouml_bsa_save_selection` | Save the selected site models into a new SiteModelCollection — the headless equivalent of the web UI's 'Save selection' menu item (SiteModelTransformer.createCollection + clone + save). sourcePath is the collection holding the site models; modelNames is the list of site-model names to save; destinationPath is the full path of the new collection to create. Returns {created, count}. |
| `biouml_bsa_track_genome` | Report or set the genome of a track (genome build id + sequences collection) — the headless equivalent of the track's 'Genome (sequences collection)' property. Analyses that convert between builds rely on it to tell which build the positions are in (e.g. whether a track needs a liftover to hg38). VCF imports take the genome from the file's ##reference header when it names one; use this tool to check or correct it. path is the track; genome (optional) is a build id or alias ('hg19', 'GRCh37', 'hg38', ...) or the path of a sequences collection; omit it to only report. Returns {path, genomeId, sequences, availableGenomes[{genomeId, name, sequences}]}, plus changed:true when set. |
| `biouml_bsa_save_selection_track` | Save the selected sites into a new track — the headless equivalent of the web UI's 'Save selection as track' menu item (SqlTrack.createTrack + addSite + save). sourcePath is the source track; siteNames is the list of site names to save; destinationPath is the full path of the new track to create. Returns {created, count}. |
| `biouml_brain_generate_equations` | Generate the brain-model equations from a brain diagram — the headless equivalent of the web UI's 'Generate equations' menu item. path is the brain diagram. Deploys the regional/cellular/receptor model equations into new diagrams and saves them. Returns {operation, path, status, completed}. |
| `biouml_brain_generate_composite_diagram` | Generate the brain composite diagram from a brain diagram — the headless equivalent of the web UI's 'Generate composite diagram' menu item. path is the brain diagram. Builds a composite diagram wiring the cellular+regional submodels and saves it. Returns {operation, path, status, completed}. |
| `biouml_brain_generate_multilevel_model` | Generate the brain multi-level model from a brain diagram — the headless equivalent of the web UI's 'Generate multi-level model' menu item. path is the brain diagram. Deploys the submodels and builds the multi-level agent-model diagram. Returns {operation, path, status, completed}. |
| `biouml_analysis_list` | List all available analysis methods grouped by their analyses group. |
| `biouml_analysis_describe` | Describe an analysis method: its parameter input schema (name, type, default, description, and for choice parameters allowedValues — at most 200, with allowedValuesCount when there are more), input names, and output names. |
| `biouml_analysis_run` | ASYNC — Run an analysis. By default (sync=false, the default) this returns IMMEDIATELY with {taskId, status}; the analysis runs in the background and is NOT done when this call returns. To finish: (1) call this to get the taskId, (2) repeatedly call biouml_task_status (with a short delay) until its status is 'done'/'error', (3) then read the results. Set sync=true to instead run inline and wait for the result in one call (only for quick analyses). params is a map of bean-property values: nested keys use '/' separators (or pass a JSON object), array items are addressed by index ('conditions/0/condition') or passed as a JSON list ('conditions': [{'condition': '...'}]); path-typed properties take repository path strings. Choice parameters list their accepted values in biouml_analysis_describe (allowedValues). |
| `biouml_task_status` | ASYNC poll — Report a background task's status (queued/running/paused/done/error/cancelled), progress (0-100), and result paths. Call it REPEATEDLY (with a short delay between calls) until the status reaches a terminal state ('done' or 'error'); the task is not finished until then. This is the poll for biouml_analysis_run (async mode). taskId is the id returned by the start action. Also returns the task log's latest warnings/errors (problems, e.g. skipped workflow steps) and its last lines (logTail). |
| `biouml_task_cancel` | Cancel a queued or running task. |
| `biouml_task_list` | List active and recent tasks (id, type, status). |
| `biouml_analysis_repeat` | Re-run a completed analysis from its stored output collection (reads the stored analysisName + parameters and runs again). |
| `biouml_analysis_get_result` | Read an analysis output collection: for a table, column names/types and the first N rows; for a folder, its children. |
| `biouml_diagram_create` | Create a new diagram in a target collection. type is 'math' (default) or 'pathway'. Returns the new diagram's path. |
| `biouml_diagram_describe` | Describe a diagram: element count, per-element summaries for every element including those nested in compartments (id, path, name, kind, position, node type; the variable of a species; the formula of a reaction or equation; source/target/role of an edge) and — when a dynamic model is present — its variables and equation count. reload=true first drops the copy the server keeps in memory for this session and re-reads the diagram from storage, i.e. describes what is actually saved (unsaved changes made by a script are lost). |
| `biouml_diagram_node_types` | List the node types that biouml_diagram_add_node accepts. |
| `biouml_diagram_add_node` | Add a node to a diagram. Returns {id, path, title} and, for a species or compartment of a model, its variable. name may be qualified by a compartment ('default/V' creates V inside compartment default); title is the displayed label (default: the name); width/height set the node size; styleFrom copies the look (fill, border, font) of an existing node, e.g. a neighbouring species; fillColor sets the fill, e.g. '#c8e6ff'. nodeType is one of the keys from biouml_diagram_node_types (default 'Stub'). In a diagram with a mathematical model (e.g. any SBML model) species, compartments and reactions are created by the diagram type, with their model variables and rate equations; for SBML models the species may be given its SBGN entity type ('unspecified', 'simple chemical', 'macromolecule', 'nucleic acid feature', 'perturbing agent', 'complex'; 'Species' is a macromolecule). A reaction created this way has no participants and rate 0 — prefer biouml_diagram_add_reaction. A failed add is undone, so the diagram stays saveable. |
| `biouml_diagram_add_edge` | Add an edge between two nodes (ids are names or paths such as 'default.s1'). Between a reaction and a species of a model the edge makes the species a participant of the reaction: edgeType is its role — reactant, product or modifier (default: reactant for species -> reaction, product for reaction -> species) — and the participant's rate equation is created. Otherwise a plain relation edge is added. Returns the assigned edge id. |
| `biouml_diagram_add_reaction` | Add a reaction to a model diagram in one step: the reaction node, its participant edges, the $$rate_<name> variable and the participants' rate equations, as the diagram editor does. formula is the kinetic law (species as variables, e.g. 'k1*$default.s1'); reactants/products/modifiers are species given as 's1', '2*s1' or '2 s1' (stoichiometry 2) by name, path or variable; name defaults to a generated one; compartment defaults to the first participant's; parameters is an object {name: value} that declares or sets the parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, path, rateVariable, participants}. |
| `biouml_diagram_add_equation` | Add an equation to a model diagram: type 'scalar' (assignment rule, default), 'rate' (rate rule), 'algebraic' or 'initial assignment'. variable is the target — a parameter (declared if missing) or a species by name, path or variable; formula is the right-hand side; name is the node id (default generated); parameters is an object {name: value} that declares or sets parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, type, variable, formula}. |
| `biouml_diagram_add_event` | Add an event to a model diagram: when trigger (a boolean formula, e.g. 'time >= t_start') becomes true, after delay (a formula, default 0), the assignments are performed in the given order. assignments is a list of 'variable = formula' strings, e.g. ['Vaccination_on = 1', '$default.s4 = 0'] (an object {variable: formula} is accepted too, but clients may reorder its keys) — variables are parameters or species by name, path or variable and must exist (declare new parameters through parameters, an object {name: value} applied first); name is the node id (default generated); x, y place the node. Any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, trigger, delay, assignments}. |
| `biouml_diagram_set_parameters` | Set initial values of model variables: parameters is an object {name: value}; names are parameters (declared if missing) or species by name, path or variable. constant (optional) marks them constant or not. Returns {set}. |
| `biouml_diagram_add_data_element` | Add a repository element (a track, table, diagram, ...) to a diagram — the headless equivalent of dragging it from the repository tree onto the diagram in the web UI. The diagram type decides what node it becomes. E.g. to give a diagram that describes an analysis' input data a new track, drop the track into the compartment for the data and into each other compartment it belongs to; remove the nodes of the old track with biouml_diagram_remove_element. diagramPath is the diagram; elementPath is the repository path of the element; compartment (optional) is the id of the compartment to drop it into; x/y (optional) the drop point. Returns {element, added[{id, path, name, kind, ...}]}. |
| `biouml_diagram_remove_element` | Remove an element (node or edge) from a diagram by its id (a name or a path such as 'default.s1'), with the edges and reaction participants that depend on it; the rate variables of removed reactions are removed too (listed in removedVariables). |
| `biouml_diagram_move_element` | Move a node to a new (x, y) position, as the diagram editor does: a compartment takes its content along and the attached edges are re-routed. Returns {id, x, y}, plus warnings when the node or a node of a moved compartment ends up at a negative x or y, i.e. beyond the left or top edge of the canvas. |
| `biouml_diagram_move_elements` | Shift several nodes by (dx, dy) in one call, e.g. a column of equations or a group of species. elements are node ids (names or paths such as 'default.s1'). A compartment takes its content along; listed elements inside a listed compartment move with it, once. Edges are re-routed. Returns {moved, movedWithCompartment, dx, dy}, plus warnings when a moved node (or a node of a moved compartment) ends up at a negative x or y, i.e. beyond the left or top edge of the canvas. |
| `biouml_diagram_resize_element` | Resize a node, e.g. enlarge a compartment to make room for new species and reactions. elementId is a name or a path such as 'default'; width and height are in pixels. Returns {id, width, height}. |
| `biouml_diagram_set_formula` | Change the formula of an existing reaction (its kinetic law; the rate equation follows) or equation (assignment rule, rate rule, ...). Give elementId (the reaction or equation node) or variable (the variable a rule defines, e.g. 'Population'). parameters is an object {name: value} that declares or sets parameters of the formula; any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, kind, variable, oldFormula, formula}. |
| `biouml_diagram_set_simulation_options` | Set the simulation time span and step stored with a model diagram (used when the diagram is simulated): initialTime, completionTime and timeIncrement; values not given are kept. biouml_diagram_describe shows the current ones. Returns {engine, initialTime, completionTime, timeIncrement}. |
| `biouml_diagram_rename_element` | Rename an element's display title. |
| `biouml_diagram_image` | Render a diagram as a PNG picture returned to the caller (an MCP image block, like biouml_simulation_plot), to check the layout. The view is built from the current state of the diagram. scale is the zoom factor (default 1); the picture is scaled down to fit maxWidth x maxHeight pixels (default 1600 x 1600). Returns {width, height, scale} plus the image. |
| `biouml_diagram_save` | Save a diagram to a DML file on disk (in targetDir, or the current directory). Returns the file path and size. |
| `biouml_diagram_export` | Export a diagram to a file (DML by default). Returns the file path and size. |
| `biouml_diagram_import` | Import a diagram file (DML by default) into a target collection. Returns the new element's path. |
| `biouml_diagram_layout` | Apply a layout operation to the selected diagram nodes — the headless equivalent of the web UI's 'Align ...' / 'Distribute ...' diagram context-menu items. path is the diagram; op is one of align-up, align-down, align-left, align-right, align-centerx, align-centery, distribute-hor, distribute-ver; nodes is the list of node paths to lay out (>=2 for align, >=3 for distribute). The diagram view is regenerated headlessly and the nodes moved via the diagram's semantic controller. Returns {applied, op, count}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_update_submodel` | Update the sub-diagrams of a composite diagram — the headless equivalent of the web UI's 'Update submodel' menu item (UpdateSubModelAction). Recursively re-reads each sub-diagram's current diagram and clears its cached view so the composite reflects the latest sub-models. path is the composite diagram. Returns {applied, status}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_split` | Split the selected elements of a composite diagram into a new diagram — the headless equivalent of the web UI's 'Split diagram' menu item (SplitDiagramAction). path is the composite diagram; elements are the node/edge paths to move to the new diagram (>=1); name is the new diagram's name (default 'Module'); targetCollection is the collection to create it in (default: the source's); autoIncludeReactions pulls in every reaction whose participants are all selected; addModule embeds the new diagram back as a sub-diagram module with connection ports. Returns {applied, status, path, created}. With addModule the source diagram is changed and saved too; if that fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_clone_node` | Clone a pathway node — the headless equivalent of the web UI's 'Clone node' menu item (CloneNodeAction). path is the pathway diagram; node is the name of the node to clone (must carry a variable role); cloneName is the new node's name (default auto-generated); separateClones creates one clone per reaction (named cloneName_reaction); reactions are the names of the reactions whose edges are redirected to the clone (default: none). Returns {applied, status, clone}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_change_subdiagram` | Change a sub-diagram's target diagram — the headless equivalent of the web UI's 'Change subdiagram' menu item (ChangeSubdiagramAction). path is the composite diagram containing the sub-diagram; subdiagram is the path of the sub-diagram to replace; target is the path of the new diagram the sub-diagram should point at. The existing sub-diagram is replaced by a fresh one preserving the node name and rewiring the ports. Returns {applied, status, subdiagram, target}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_change_port` | Change a pathway port's type — the headless equivalent of the web UI's 'Change port type' menu item (ChangePortTypeAction). path is the pathway diagram; port is the name of the port node; portType is the new type (input, output, or contact). The port is recreated with the new type and its edges are rewired. Returns {applied, status, port, portType}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_merge_clone` | Merge a cloned pathway node back into its original — the headless equivalent of the web UI's 'Merge clone' menu item (MergeCloneAction). path is the pathway diagram; clone is the name of the clone node to merge away (must be a variable-role node whose variable references a different original element). Its edges are redirected to the original and the clone is removed. Returns {applied, status, merged}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_save_subset` | Save a subset of a diagram's elements to a new diagram — the headless equivalent of the web UI's 'Save subset' menu item (SaveDiagramSubsetAction). path is the source diagram; elements are the node/edge paths to keep (>=1); name is the new diagram's name (default '<source> subset'); targetCollection is the collection to create it in (default: the source's). The new diagram contains only the selected elements (plus the compartments/edges that hold them). Returns {applied, status, path, created, kept}. |
| `biouml_diagram_add_from_search` | Add upstream/downstream neighbours from a graph search to a diagram — the headless equivalent of the web UI's 'Add upstream elements' / 'Add downstream elements' menu items (AddFromSearchUpAction / AddFromSearchDownAction). path is the diagram to add elements to; elements are the node paths to expand (>=1); direction is 'up' or 'down'. Each selected element is queried in the BioHub for linked elements in the given direction and they are added to the diagram. Requires a BioHub query engine for the element's source. Returns {applied, status, direction, count}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_simulation_start` | ASYNC — Start a diagram simulation as a background job. This returns IMMEDIATELY with a jobID; the simulation runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_simulation_status (with a short delay) until 'completed' is true, (3) then call biouml_simulation_result to fetch the time series. The diagram must have a dynamic model with at least one rate (ODE) equation. Delegates to the platform's simulation provider. |
| `biouml_simulation_status` | ASYNC poll — Report the progress of a simulation job started by biouml_simulation_start. Call it REPEATEDLY until the returned 'completed' field is true; the simulation is not done until then. ⏱️ Do NOT poll faster than every 10 seconds — each poll is a full server request and polling every 1–2 seconds wastes server resources and adds latency for no benefit; wait ~10 s between polls (longer, e.g. 30 s, for big simulations). The 'message' field is the job's accumulated log up to this moment — it grows as the simulation runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {jobID, status, progress, message, completed}. |
| `biouml_simulation_result` | ASYNC result — Fetch the <em>raw time-series</em> result of a completed simulation job. ⚠️ USE ONLY AS A LAST RESORT: this returns the ENTIRE numeric series, which for any non-trivial simulation is a huge amount of data that can exhaust your context (it has caused context-overflow errors before). If you want to SEE or SUMMARIZE the result, do NOT call this tool — call biouml_simulation_plot instead, which returns a compact image + a tiny metadata summary ({variables, points, timeRange, finals}) with no raw numbers. Call it only AFTER biouml_simulation_status reports 'completed', and only if you genuinely need every number (e.g. to compute a statistic no other tool provides). Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {vars, times, values} (and Q1/Q2/Q3 for stochastic results). |
| `biouml_simulation_plot` | Render a PLOT of a completed simulation's result ON THE SERVER and return it as a PNG image plus a tiny metadata summary — the headless equivalent of the web UI's 'New plot' / '+' action. PREFERRED over biouml_simulation_result for simply inspecting a result: the full time series stays on the server and only an image + {variables, points, timeRange, finals} come back, so a large result does not blow your context. Call it AFTER biouml_simulation_status reports 'completed'. jobID is the id returned by biouml_simulation_start. Optionally pass 'variables' (subset of the result's variable names) and 'width'/'height' (px); by default every non-time variable is plotted. Returns a small metadata JSON {jobID, plot, variables, points, timeRange, finals, imageFormat, width, height} plus the PNG as a separate MCP image content block (vision — NOT base64 text in the JSON). |
| `biouml_simulation_save_result` | Persist the result of a completed simulation INTO THE REPOSITORY (durable, survives server restarts) — the headless equivalent of the web UI's 'Save result' button. By default MCP simulations keep their result only in memory (it is lost when the server restarts), so use this to keep it. Call it AFTER biouml_simulation_status reports 'completed'. jobID is the id returned by biouml_simulation_start. destinationPath is the FULL repository path of the new result element to create (e.g. 'myproject/mydiagram/result1'); its parent must already exist and be writable. Returns the provider's response (usually 'ok'). |
| `biouml_simulation_list_solvers` | List the ODE solvers available to the simulation engine (name, type, implementation class). |
| `biouml_project_list` | List the current user's projects with their permissions (admin/canWrite/canDelete) and disk quota. |
| `biouml_project_create` | Create a new project (a SQL-backed research collection) for the current user. The name may only contain latin letters, numbers, spaces and a few symbols. |
| `biouml_project_delete` | Delete a project (and its data) for the current user. Requires delete permission. |
| `biouml_project_size` | Report the disk usage (in bytes) of a single project's data folder. |
| `biouml_user_info` | Return the current user's profile (username and profile fields) from the security provider. |
| `biouml_user_change_password` | Change the current user's password. Requires the current (old) password and a new one. |
| `biouml_user_change_info` | Update the current user's profile fields (e.g. name, email) in the security provider. Pass a flat object of field name -> value. |
<!-- END GENERATED TOOL TABLE -->

## Perspectives (task-oriented entry points)

Perspectives are the entry points users choose in the web UI (Default, GTRD, Microbiome, Regulatory
mutations, …; plugins contribute more). `biouml_perspective_list` tells an agent which perspectives the
user may open and, for each, its description, repository roots and *entry points* — the analyses,
workflows and collections work usually starts from. Pass an entry point's path to
`biouml_analysis_describe` (analyses, workflows) or `biouml_repo_describe` (collections).

The perspective in effect filters importers and exporters (its `importers`/`exporters` rules), so it changes
what `biouml_repo_import_formats`, `biouml_repo_export_formats` and import autodetection offer. Until
`biouml_perspective_select` is called, that is the perspective the user last chose in the web UI.
`biouml_perspective_select` switches it **for the MCP session only**; the user's saved perspective is not
changed. When the client authenticates with the web UI's own `JSESSIONID`, the MCP session *is* that
browser session, so the selection applies there too until the user picks a perspective in the UI.

`biouml_perspective_select` also reads the perspective's intro page (the start page the web UI shows) when
the web UI files are deployed next to the server, and returns what it offers users as an `intro` block:
further entry points (each with an `exists` flag), `analysisPresets` (an analysis path plus the parameter
values the page fills in — pass them to `biouml_analysis_run`), `importTargets`, `help` links and a page
`summary`. Perspectives without a declared description are described by the page's heading and first
paragraph. Pages are parsed statically (links and string literals in their scripts; scripts are not run), and
pages they embed with an iframe or jQuery `.load()` are followed. The web UI is found at the `-Dbiouml.web.path`
location (a directory or `.war`) if set, otherwise from the embedded Tomcat's `bioumlweb.war`, otherwise at
`webapps/bioumlweb` (directory, then `.war`) under `catalina.base` — e.g. `/var/lib/tomcat9/webapps/bioumlweb`
on a Debian `tomcat9` install — or under `<biouml.server.path>/tomcat`. Without it there is no `intro` block.

A perspective declares its description and entry points in `plugin.xml` (both optional; `title` defaults to
the element name). Declared entry points come first and are reported even where the web UI files are not
deployed:

```xml
<perspective name="Microbiome" priority="99" intro="intro-microbiome">
  <description>Microbial genome assembly from NGS reads: ...</description>
  <repository>...</repository>
  <entryPoints>
    <entry path="analyses/Workflows/Microbiome/Assembly basic" title="Basic assembly workflow"
           description="Reads are supplied as FASTQ files, adapters as FASTA"/>
  </entryPoints>
  ...
</perspective>
```

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
{"method":"tools/call","params":{"name":"biouml_simulation_start","arguments":{"diagramPath":"mydata/diagrams/decay"}}}
```

## Example: extend an SBML model

SBML models are stored as SBGN diagrams and written back to SBML on every save, so model elements
must carry their model roles. `add_reaction`, `add_equation` and `set_parameters` create them the way
the diagram editor does; each call saves the diagram, and a call that fails is undone.

```jsonc
// copy first, then edit the copy
{"method":"tools/call","params":{"name":"biouml_repo_copy_element","arguments":{"srcPath":"data/Examples/model","destPath":"data/Collaboration/My project/Data/model v2"}}}
// a new species V in compartment "default" (its variable is $default.V)
{"method":"tools/call","params":{"name":"biouml_diagram_add_node","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","name":"default/V","nodeType":"Species"}}}
// S -> V with mass action; k_vac is declared with value 0.006
{"method":"tools/call","params":{"name":"biouml_diagram_add_reaction","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","name":"vaccination","formula":"k_vac*$default.S","reactants":["S"],"products":["V"],"parameters":{"k_vac":0.006}}}}
// an assignment rule
{"method":"tools/call","params":{"name":"biouml_diagram_add_equation","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","variable":"Total","formula":"$default.S + $default.V"}}}
```

In scripts (`biouml_repo_run_script`, type `js`) the `model` host object does the same:
`model.addSpecies(diagram, "default/V", 0)`, `model.addReaction(diagram, "vaccination", "k_vac*$default.S",
[model.createSpecieReference(diagram, "S", "reactant"), model.createSpecieReference(diagram, "V", "product")])`,
`model.addEquation(diagram, "scalar", "Total", "$default.S + $default.V")`; they throw on bad input and
change the diagram in memory only — finish with `diagram.save()`.

## Example: a VCF file from a share link (tools only)

```jsonc
// 1. fetch + import (a Google Drive share link works too); the genome comes from the ##reference header
{"method":"tools/call","params":{"name":"biouml_analysis_run","arguments":{"name":"Upload from FTP","params":{
   "fileURL":"https://drive.google.com/file/d/<id>/view","importerFormat":"VCF format (*.vcf)",
   "resultPath":"data/Projects/P/Data/sample"}}}}
// 2. check the genome (analyses that convert between builds read it); correct it if needed
{"method":"tools/call","params":{"name":"biouml_bsa_track_genome","arguments":{"path":"data/Projects/P/Data/sample"}}}
{"method":"tools/call","params":{"name":"biouml_bsa_track_genome","arguments":{"path":"data/Projects/P/Data/sample","genome":"hg19"}}}
// 3. keep the variant genotypes only (keeps genome and omics type); poll biouml_task_status
{"method":"tools/call","params":{"name":"biouml_analysis_run","arguments":{"name":"Filter VCF by genotype","params":{
   "inputTrack":"data/Projects/P/Data/sample","mode":"both","outputPath":"data/Projects/P/Data/Filtered"}}}}
```

`biouml_analysis_describe` lists the accepted values of choice parameters such as `mode`
(`allowedValues`, at most 200; `allowedValuesCount` gives the total).

## Limitations

- **Interactive-only actions** — `biouml_repo_run_action` only invokes actions classified as
  headless-safe. Actions that open documents or show input dialogs (e.g. Open, Login, most
  "New …" wizards) return `requires_interactive_ui` instead of running. The classification is
  default-deny: an action is headless-safe only if it is on an explicit allowlist.
- **Async semantics** — `biouml_analysis_run` with `sync:false` queues the analysis and returns a
  task id; poll with `biouml_task_status` and read the output with `biouml_analysis_get_result`.
  There is no push/notification channel on this transport — clients poll.
- **Headless simulation** — `biouml_simulation_start` requires the diagram to have a dynamic model
  with at least one ODE rate equation. In a plain headless JVM the ODE code-generation (Velocity
  template) may not be available, in which case the start call returns a `missing_dynamic_model`
  error rather than a job; on a provisioned OSGi/Tomcat runtime it starts the async simulation job.
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
