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

Related operations are one tool with an `op` argument: e.g. `biouml_diagram_edit` with `op` `move`,
`shift`, `resize`, `rename`, `remove` or an align/distribute op; `biouml_task` with `list`, `status` or
`cancel`. The input schema lists each tool's ops as the enum of `op`. The tools these ops replaced
(`biouml_diagram_move_element`, `biouml_task_status`, …) are not listed any more but stay callable by
their old names with their old arguments, so existing agents and scripts keep working.

**Lite mode for small models.** `/biouml/mcp?mode=lite` lists only two tools, so the tool list takes
little of the model's context:

- `biouml_catalog` — without arguments the tool groups (repo, diagram, analysis, …) with a summary and
  their tool names; with `group` the group's tools, each with a one-sentence summary, its ops and its
  required arguments; with `tool` that tool's full description and input schema.
- `biouml_call` — `{"tool": "<name>", "arguments": {…}}` runs a tool (a JSON string for `arguments`
  is accepted too); missing required arguments are reported with the tool's input schema.

Every tool also stays callable by its own name in lite mode.

<!-- BEGIN GENERATED TOOL TABLE — do not edit by hand; McpReadmeTableTest asserts this
     table's row count equals the live catalog. Regenerate by running that test. -->
| Tool | Description |
|------|-------------|
| `biouml_perspective_list` | List the perspectives available to the current user — BioUML's task-oriented entry points (e.g. GTRD, Microbiome, Regulatory mutations) — and the one in effect for this session. Each has a description (declared, or taken from the perspective's start page), its repository roots, entry points (the analyses, workflows and collections work in that perspective usually starts from; pass their paths to biouml_analysis_describe or biouml_repo_describe), and guide:true when biouml_perspective_select returns a step-by-step guide for doing the perspective's work with MCP tools. The perspective in effect decides which import/export formats are offered; switch it with biouml_perspective_select. |
| `biouml_perspective_select` | Switch the perspective for this MCP session only (the user's saved web-UI perspective is not changed) and learn how work in it is done. name is one of biouml_perspective_list. From then on the formats of biouml_repo_import and biouml_repo_export and import autodetection follow the perspective's importer/exporter rules. Returns the perspective's description, repository roots, entry points and allow/deny rules; guide, when there is one: how to do with MCP tools what the perspective's start page does (read it first); and — when the web UI files are found on the server — an intro block with what the start page offers users: for a wizard (a page with step-id elements) its steps (title, a short guide, helpPage — read the whole help page with biouml_perspective_page), more entry points (with an exists flag), analysisPresets (the analyses the page starts, with the parameter values it passes, checked against the analysis — pass them to biouml_analysis_run — and chosenByUser, the parameters the user sets), pathTemplates (repository paths the page builds, e.g. 'data/Projects/{currentProject}/Data'), exampleProjects (projects the page lists as examples, e.g. demo projects with results to learn from), importTargets (folders the page imports into), help links and a page summary. |
| `biouml_perspective_page` | Read a page of the web UI as text: a wizard step's help page (the helpPage of a step in biouml_perspective_select), a perspective's start page or a user guide included in them. page is its path in the web application, e.g. 'wizard/help/step-start.shtml'; server-side includes are expanded. Returns {page, title, text (headings as '## ...', one paragraph, list item or table row per line), totalChars, nextOffset (when more follows: call again with offset=nextOffset), links (repository paths from '#de=' links, and web links)}. |
| `biouml_repo_collections` | List the top-level registered repository collections (databases, data, analyses, ...). |
| `biouml_repo_list` | List the children of a repository collection. Project contents may change in a parallel web UI session; if an element under a project is missing, re-list it (and the projects with biouml_project op list) instead of relying on an earlier listing. |
| `biouml_repo_describe` | Describe a repository element (name, class, child count, children). Project contents may change in a parallel web UI session; if an element under a project is missing, re-list it (and the projects with biouml_project op list) instead of relying on an earlier listing. |
| `biouml_repo_remove` | Remove an element by path. dryRun=true reports what would be removed without changing anything. |
| `biouml_repo_job_status` | ASYNC poll — Report the progress of a background job started by an async start-tool (biouml_repo_copy op folder, biouml_repo_script op run, biouml_repo_import). Call it REPEATEDLY (with a short delay between calls) until the returned 'completed' field is true; the work is not done until then. The 'message' field is the job's accumulated log up to this moment — it grows as the job runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's jobcontrol provider. jobID is the id returned by the start tool. Returns {jobID, status, progress, message, completed}. |
| `biouml_repo_reinitialize` | Re-initialize a collection that previously failed to load — the headless equivalent of the web UI's 'Retry'/'Reinitialize' menu item. Returns the path and whether it is now valid. |
| `biouml_repo_git_enabled` | Report whether a collection has git version-control enabled (and git is available on the server). Delegates to the platform's git provider. |
| `biouml_repo_preferences` | Read the current session's preferences. Delegates to the platform's preferences provider. |
| `biouml_repo_new_element` | Create a new element in a collection — the headless equivalent of the web UI's 'New X' menu items. kind is one of: 'folder', 'table', 'test', 'workflow', 'research', 'notebook' (Jupyter), or a script type from biouml_repo_script op types (e.g. 'js', 'R', 'Java', 'Nextflow', 'WDL', 'math'). parentPath is the target collection; name is the new element's name; content is optional initial content (used for scripts/notebooks, ignored otherwise). Returns {created, kind}. |
| `biouml_repo_login` | Log into a credentials-protected collection — the headless equivalent of the web UI's 'Login' menu item (CredentialsCollection.processCredentialsBean). path is the collection; fields is a flat map of credential field name -> value (e.g. {"user":"...","password":"..."}) matched case-insensitively to the collection's credentials bean. Returns {loggedIn, path}. |
| `biouml_repo_log` | Fetch the current session's server log tail — the headless equivalent of the web UI's 'Logs' view pane. Returns the in-memory java.util.logging capture for this session (a bounded, SEVERE-by-default buffer of everything the JVM logged under it — cross-job server output such as unhandled exceptions, NOT a single job's log). Diagnostic only: returns an error if the experimental logging feature was not enabled at server boot. Delegates to the platform's log provider. |
| `biouml_repo_search` | Search element names (case-insensitive substring) under scope (default: all roots). op search (default): return the matches; on a large repository it can exceed the client's timeout — then use op start, which runs the same search in the background and returns a taskId at once, poll op status (taskId: queued/running/done/cancelled/error) until 'done', and call op search again with the same query and scope: the warmed caches make it quick. |
| `biouml_repo_action` | The context-menu actions of the element path. op list: the actions available (key, name, description, headlessSafe). op run: run action (a key from op list) headlessly; interactive-only actions return requires_interactive_ui. |
| `biouml_repo_copy` | Copy srcPath to destPath, the full path of the copy (its parent must exist and be writable; the last segment is the new name); the source is left untouched. op element: one element (file, table, diagram, any cloneable element) — the web UI's 'Save a copy'; done when the call returns. op folder: a folder with its whole subtree; ASYNC — returns a jobID at once, the copy is done when biouml_repo_job_status reports 'completed' true. |
| `biouml_repo_export` | Export a repository element to a file on the server — the web UI's 'Export'. op formats: the formats available for path, in priority order (they follow the perspective in effect, see biouml_perspective_select). op export (default): write path in format (one of op formats; default the first) into targetDir (a server-local directory; default a new temporary one), named after the element with the format's suffix; returns {file, format, bytes, sha256, downloadId}. op download: the exported file downloadId in base64 chunks of length bytes (default 262144, max 1048576) from offset (default 0); returns {name, offset, bytes, totalBytes, sha256, base64, done, nextOffset} — call again with offset=nextOffset until done, join the decoded chunks and check sha256. A downloadId lasts 24 h and works only for the user who exported. |
| `biouml_repo_import` | Import a file from the server's filesystem into the collection parentPath. op formats: the formats available for parentPath, in priority order, the first being 'autodetect' (they follow the perspective in effect, see biouml_perspective_select). op import (default): ASYNC — returns a jobID at once; the import is done when biouml_repo_job_status reports 'completed' true, and its final message is the path of the imported element. file is a server-local path; format is one of op formats (omit to autodetect; an ambiguous autodetect is an error listing the candidates); name defaults to the file name without extension. |
| `biouml_repo_document` | The text of the document element path (script, notebook, text file, ...). op read: return it. op write: replace it with content. |
| `biouml_repo_script` | Run scripts (JS, R, Java, ...). op types: the script types available on this server (type id, title, element class). op run: ASYNC — run the source text script of type (an id from op types); returns a jobID at once; when biouml_repo_job_status reports 'completed' true, op result (jobID) returns the output: the printed buffer, tables, images and HTML. A script element is created with biouml_repo_new_element. |
| `biouml_repo_omics_type` | The omics type (Transcriptomics, Genomics, ...) of the data element path. op detect: detect it. op set: set it to omicsType. |
| `biouml_repo_filesystem_type` | The element type of the file path in a file-system collection — the web UI's 'Change element type'. op types: the types available ({path, count, types}). op set: change it to type, one of op types; returns {changed, type}. |
| `biouml_analysis_list` | List all available analysis methods grouped by their analyses group. |
| `biouml_analysis_describe` | Describe an analysis method: its description (and help, the text of its help page when it has one), its parameter input schema (name, type, default, description, and for choice parameters allowedValues — at most 200, with allowedValuesCount when there are more), input names, and output names. The fields of a nested bean and of each item of an array of beans are listed with the keys biouml_analysis_run takes ('expertSettings/cutoffFC', 'conditions/0/condition'). params (optional, as for biouml_analysis_run) are bound first: the choices of some parameters depend on others — e.g. the columns to choose from are those of the table passed — and array items appear once the array is set. filter (optional) keeps only the allowed values that contain it, case-insensitively (matchingCount tells how many match), e.g. filter 'carcinoma' to find a disease among thousands; without properties it lists only the choice parameters with matches. properties (optional) lists only the parameters named (with their fields and items, e.g. ['conditions']). With filter or properties the help text is left out. |
| `biouml_analysis_run` | ASYNC — Run an analysis. By default (sync=false, the default) this returns IMMEDIATELY with {taskId, status}; the analysis runs in the background and is NOT done when this call returns. To finish: (1) call this to get the taskId, (2) repeatedly call biouml_task with op status (with a short delay) until its status is 'done'/'error', (3) then read the results. Set sync=true to instead run inline and wait for the result in one call (only for quick analyses). params is a map of bean-property values: nested keys use '/' separators (or pass a JSON object), array items are addressed by index ('conditions/0/condition') or passed as a JSON list ('conditions': [{'condition': '...'}]); path-typed properties take repository path strings. Choice parameters list their accepted values in biouml_analysis_describe (allowedValues). |
| `biouml_analysis_repeat` | Re-run a completed analysis from its stored output collection (reads the stored analysisName + parameters and runs again). |
| `biouml_analysis_get_result` | Read an analysis output: for a table, column names/types, rows (from offset, default 0; at most limit, default 50, max 500; truncated tells whether more follow) and their row ids (rowIds, aligned with rows — the table's key, e.g. a gene or drug id); columns (optional) names the columns to return, in that order — wide tables such as drug lists with synonyms are much smaller with only the columns needed; for a track (e.g. an imported VCF), its sites the same way: rowIds are the site ids, columns chromosome, from, to, strand, type and the site properties (a VCF's RefAllele, AltAllele, genotype fields such as Format_GT_<sample>, ...); for a folder, its children; for a simulation result (e.g. the output of 'Simulation analysis'), the time series ({vars, times, values}, plus Q1/Q2/Q3 for stochastic results). |
| `biouml_task` | Background tasks, e.g. of biouml_analysis_run. op list: active and recent tasks (id, type, status). op status: ASYNC poll — the status of task taskId (queued/running/paused/done/error/cancelled; status is the numeric job status, state its name), progress (0-100) and result paths; call it repeatedly, with a short delay, until the status is 'done' or 'error'. It also returns the log's warnings and errors grouped by message ignoring numbers (problems: the latest 20 distinct ones with their counts; problemCount: all problem lines; problemKinds: all distinct messages), e.g. skipped workflow steps, and the log's last lines (logTail; lines that differ only in numbers, such as progress lines, are shown once with [×N]). op cancel: cancel a queued or running task. |
| `biouml_diagram_create` | Create a new, empty diagram in a target collection. type is 'math' (default), 'pathway', or the class name (full or simple) of a diagram type a plugin declares, e.g. 'AnnotationDiagramType' for a Genome Enhancer annotation diagram; an unknown type is refused with the list of registered ones. The diagram type decides what a new diagram already holds; its toolbar actions (biouml_diagram_action) add more, e.g. the categories of an annotation diagram. Returns the new diagram's path and type. |
| `biouml_diagram_describe` | Describe a diagram: element count, per-element summaries for every element including those nested in compartments (id, path, name, kind, position, node type; the variable of a species; the formula of a reaction or equation; source/target/role of an edge) and — when a dynamic model is present — its variables and equation count. reload=true first drops the copy the server keeps in memory for this session and re-reads the diagram from storage, i.e. describes what is actually saved (unsaved changes made by a script are lost). |
| `biouml_diagram_node_types` | List the node types that biouml_diagram_add_node accepts. |
| `biouml_diagram_add_node` | Add a node to a diagram. Returns {id, path, title} and, for a species or compartment of a model, its variable. name may be qualified by a compartment ('default/V' creates V inside compartment default); title is the displayed label (default: the name); width/height set the node size; styleFrom copies the look (fill, border, font) of an existing node, e.g. a neighbouring species; fillColor sets the fill, e.g. '#c8e6ff'. nodeType is one of the keys from biouml_diagram_node_types (default 'Stub'). In a diagram with a mathematical model (e.g. any SBML model) species, compartments and reactions are created by the diagram type, with their model variables and rate equations; for SBML models the species may be given its SBGN entity type ('unspecified', 'simple chemical', 'macromolecule', 'nucleic acid feature', 'perturbing agent', 'complex'; 'Species' is a macromolecule). A reaction created this way has no participants and rate 0 — prefer biouml_diagram_add_reaction. A failed add is undone, so the diagram stays saveable. |
| `biouml_diagram_add_edge` | Add an edge between two nodes (ids are names or paths such as 'default.s1'). Between a reaction and a species of a model the edge makes the species a participant of the reaction: edgeType is its role — reactant, product or modifier (default: reactant for species -> reaction, product for reaction -> species) — and the participant's rate equation is created. Otherwise a plain relation edge is added. Returns the assigned edge id. |
| `biouml_diagram_add_reaction` | Add a reaction to a model diagram in one step: the reaction node, its participant edges, the $$rate_<name> variable and the participants' rate equations, as the diagram editor does. formula is the kinetic law (species as variables, e.g. 'k1*$default.s1'); reactants/products/modifiers are species given as 's1', '2*s1' or '2 s1' (stoichiometry 2) by name, path or variable; name defaults to a generated one; compartment defaults to the first participant's; parameters is an object {name: value} that declares or sets the parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, path, rateVariable, participants}. |
| `biouml_diagram_add_equation` | Add an equation to a model diagram: type 'scalar' (assignment rule, default), 'rate' (rate rule), 'algebraic' or 'initial assignment'. variable is the target — a parameter (declared if missing) or a species by name, path or variable; formula is the right-hand side; name is the node id (default generated); parameters is an object {name: value} that declares or sets parameters of the formula. Any other undeclared identifier in the formula is declared with value 0 and listed in declaredParameters. Returns {id, type, variable, formula}. |
| `biouml_diagram_add_event` | Add an event to a model diagram: when trigger (a boolean formula, e.g. 'time >= t_start') becomes true, after delay (a formula, default 0), the assignments are performed in the given order. assignments is a list of 'variable = formula' strings, e.g. ['Vaccination_on = 1', '$default.s4 = 0'] (an object {variable: formula} is accepted too, but clients may reorder its keys) — variables are parameters or species by name, path or variable and must exist (declare new parameters through parameters, an object {name: value} applied first); name is the node id (default generated); x, y place the node. Any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, trigger, delay, assignments}. |
| `biouml_diagram_set_parameters` | Set initial values of model variables: parameters is an object {name: value}; names are parameters (declared if missing) or species by name, path or variable. constant (optional) marks them constant or not. Returns {set}. |
| `biouml_diagram_add_data_element` | Add a repository element (a track, table, diagram, ...) to a diagram — the headless equivalent of dragging it from the repository tree onto the diagram in the web UI. The diagram type decides what node it becomes. E.g. to give a diagram that describes an analysis' input data a new track, drop the track into the compartment for the data, then copy the new node into each other compartment it belongs to with biouml_diagram_copy_node; remove the nodes of the old track with biouml_diagram_edit (op remove). If the diagram type places the element outside the requested compartment, the result has a warning. diagramPath is the diagram; elementPath is the repository path of the element; compartment (optional) is the id of the compartment to drop it into; x/y (optional) the drop point. Returns {element, added[{id, path, name, kind, ...}]}. |
| `biouml_diagram_copy_node` | Copy a node into a compartment of the same diagram — the headless equivalent of dragging a node onto another compartment in the web UI. The diagram type decides whether the compartment accepts it. E.g. copy the node of a track added with biouml_diagram_add_data_element into another compartment it belongs to. elementId is the node (a name or a path such as 'Data.sample', the node sample in the compartment Data); compartment is the target compartment id (omit for the diagram itself); x/y (optional) the drop point. Returns {copied, added{id, path, name, ...}}. |
| `biouml_diagram_set_formula` | Change the formula of an existing reaction (its kinetic law; the rate equation follows) or equation (assignment rule, rate rule, ...). Give elementId (the reaction or equation node) or variable (the variable a rule defines, e.g. 'Population'). parameters is an object {name: value} that declares or sets parameters of the formula; any other undeclared identifier is declared with value 0 and listed in declaredParameters. Returns {id, kind, variable, oldFormula, formula}. |
| `biouml_diagram_set_simulation_options` | Set the simulation time span and step stored with a model diagram (used when the diagram is simulated): initialTime, completionTime and timeIncrement; values not given are kept. biouml_diagram_describe shows the current ones. Returns {engine, initialTime, completionTime, timeIncrement}. |
| `biouml_diagram_image` | Render a diagram as a PNG picture returned to the caller (an MCP image block, like biouml_simulation_output), to check the layout. The view is built from the current state of the diagram. scale is the zoom factor (default 1); the picture is scaled down to fit maxWidth x maxHeight pixels (default 1600 x 1600). Returns {width, height, scale} plus the image. |
| `biouml_diagram_export` | Write a diagram as DML to a file on the server: in targetDir (default: the server's working directory), named after the diagram, with the suffix format (default 'dml'). For other formats use biouml_repo_export. Returns the file path and size. |
| `biouml_diagram_import` | Import a diagram file (DML by default) into a target collection. Returns the new element's path. |
| `biouml_diagram_add_from_search` | Add upstream/downstream neighbours from a graph search to a diagram — the headless equivalent of the web UI's 'Add upstream elements' / 'Add downstream elements' menu items (AddFromSearchUpAction / AddFromSearchDownAction). path is the diagram to add elements to; elements are the node paths to expand (>=1); direction is 'up' or 'down'. Each selected element is queried in the BioHub for linked elements in the given direction and they are added to the diagram. Requires a BioHub query engine for the element's source. Returns {applied, status, direction, count}. The diagram is saved; if the edit or the save fails, the change is discarded and the diagram is re-read from its last saved version. |
| `biouml_diagram_edit` | Change existing elements of a diagram as the diagram editor does; elementId is a name or a path such as 'default.s1'. op move: move node elementId to (x, y); a compartment takes its content along and attached edges are re-routed; returns {id, x, y}. op shift: shift the nodes elements by (dx, dy), e.g. a column of equations; a listed node inside a listed compartment moves once, with it; returns {moved, movedWithCompartment, dx, dy}. Both warn when a node ends up at a negative x or y (beyond the left or top edge). op resize: set width and height (pixels) of elementId, e.g. enlarge a compartment; returns {id, width, height}. op rename: set the display title of elementId to newName. op remove: remove elementId (node or edge) with the edges and reaction participants that depend on it, and the rate variables of removed reactions (removedVariables). op align-up, align-down, align-left, align-right, align-centerx, align-centery, distribute-hor, distribute-ver: the web UI's 'Align ...' / 'Distribute ...' on the nodes elements (>=2 to align, >=3 to distribute); returns {applied, op, count}. |
| `biouml_diagram_extract` | Create a new diagram from the elements (node/edge paths, >=1) of the diagram path: name is the new diagram's name, targetCollection where to create it (default: the source's collection). op subset: the web UI's 'Save subset' — a new diagram with only the elements (plus the compartments and edges that hold them), name default '<source> subset'; returns {applied, status, path, created, kept}. op split: the web UI's 'Split diagram' of a composite diagram — the elements move to the new diagram (name default 'Module'); autoIncludeReactions pulls in every reaction whose participants are all selected; addModule embeds the new diagram back as a sub-diagram module with connection ports, changing and saving the source too; returns {applied, status, path, created}. |
| `biouml_diagram_submodel` | Edit the sub-models of the composite or pathway diagram path. op update: the web UI's 'Update submodel' — re-read every sub-diagram's diagram (recursively) so the composite shows the latest sub-models; returns {applied, status}. op change_subdiagram: point the sub-diagram subdiagram (its path) at the diagram target, keeping the node name and rewiring the ports; returns {applied, status, subdiagram, target}. op change_port: change the type of the port node port to portType (input, output or contact), rewiring its edges; returns {applied, status, port, portType}. The diagram is saved; if the edit or the save fails, it is re-read from its last saved version. |
| `biouml_diagram_clone` | Clones of a node of the pathway diagram path. op clone: the web UI's 'Clone node' — clone node (a node with a variable role); cloneName defaults to a generated name; reactions are the reactions whose edges are redirected to the clone (default none); separateClones makes one clone per reaction (named cloneName_reaction); returns {applied, status, clone}. op merge: the web UI's 'Merge clone' — merge the clone node back into its original: its edges are redirected to the original and it is removed; returns {applied, status, merged}. The diagram is saved; if the edit or the save fails, it is re-read from its last saved version. |
| `biouml_diagram_action` | The diagram's own toolbar actions — the buttons the web UI shows for an open diagram, which plugins add for their diagram types, e.g. on a Genome Enhancer annotation diagram 'Experiment control template' (an Experiment and a Control category), 'One category', 'Multiple category template' (numRow x numCol categories), 'Logical category template'. op list: the actions of the diagram diagramPath that the current perspective allows, each with its name, description, the selection it needs and its properties with their defaults (what its dialog asks for). op run: run the action named action on diagramPath as the web UI does; elements are the selected elements (names or paths such as 'Experiment', when the action needs a selection); properties is an object {name: value} of the action's properties (the others keep their defaults). The diagram is saved; returns {action, added[{id, path, name, kind, x, y, nodeType}], removed[ids]}. If the action fails, the diagram is re-read from its last saved version. |
| `biouml_simulation_list_solvers` | List the ODE solvers available to the simulation engine (name, type, implementation class). |
| `biouml_simulation_start` | ASYNC — Start a diagram simulation as a background job. This returns IMMEDIATELY with a jobID; the simulation runs in the background and is NOT done when this call returns. To finish: (1) call this to get the jobID, (2) repeatedly call biouml_simulation_status (with a short delay) until 'completed' is true, (3) then call biouml_simulation_output to plot or fetch the result. The diagram must have a dynamic model with at least one rate (ODE) equation. Delegates to the platform's simulation provider. |
| `biouml_simulation_status` | ASYNC poll — Report the progress of a simulation job started by biouml_simulation_start. Call it REPEATEDLY until the returned 'completed' field is true; the simulation is not done until then. ⏱️ Do NOT poll faster than every 10 seconds — each poll is a full server request and polling every 1–2 seconds wastes server resources and adds latency for no benefit; wait ~10 s between polls (longer, e.g. 30 s, for big simulations). The 'message' field is the job's accumulated log up to this moment — it grows as the simulation runs, so read it on each poll to watch progress and to diagnose failures. Delegates to the platform's simulation provider. jobID is the id returned by biouml_simulation_start. Returns {jobID, status, progress, message, completed}. |
| `biouml_simulation_output` | The result of the simulation job jobID (from biouml_simulation_start), once biouml_simulation_status reports 'completed'. op plot (default): render it as a plot on the server — the web UI's 'New plot' — and return a PNG image plus {jobID, plot, variables, points, timeRange, finals, imageFormat, width, height}; variables picks the result variables (default: all but time); width/height in px. The series stays on the server, so use this to look at a result. op data: the raw time series {vars, times, values} (plus Q1/Q2/Q3 for stochastic results) — every number, which for any non-trivial simulation can exhaust your context; only when you need the numbers. op save: store the result in the repository at destinationPath (the full path of the new element; its parent must exist and be writable) — the web UI's 'Save result'; otherwise a result lives in memory only and is lost when the server restarts. |
| `biouml_project` | The current user's projects. op list: the projects with their permissions (admin/canWrite/canDelete), disk quota and the properties the web UI reads (e.g. demo: "true" for a demo project, an example with finished results). op create: create the project name (a SQL-backed research collection; latin letters, numbers, spaces and a few symbols). op delete: delete the project name with its data (needs delete permission). op size: the disk usage in bytes of the project's data folder. The user may work in the BioUML web UI in a separate session at the same time, so projects can be created, renamed, shared or deleted outside this conversation and an earlier list may be out of date: when a project is not found, re-read the list with op list (it reloads the session's permissions) before concluding it does not exist. |
| `biouml_user` | The current user's account. op info: the profile (username and profile fields). op change_password: change the password from oldPassword to newPassword. op change_info: update the profile fields given as a flat object fields (e.g. name, email). |
| `biouml_table_add_row` | Add a new row to a table — the headless equivalent of the web UI's 'Add row' menu item (TableDataCollectionUtils.addRow + finalizeAddition + save). path is the table; name is the new row's name (a duplicate name is a clean error); the row is initialised with each column's default value. |
| `biouml_table_replace` | Replace string content within a table's cells — the headless equivalent of the web UI's 'Replace content' menu item. path is the table; from is the search string, to is the replacement (both required; from==to or empty from is a clean error); exactMatch (default false) makes the match whole-cell instead of substring; selectionOnly limits the operation to rows listed in rows (omit to operate on the whole table). Returns the number of changed rows. |
| `biouml_table_subset` | Create a subset (copy) of a table containing only the given rows — the headless equivalent of the web UI's 'Subset table' menu item (TableRowsExporter.exportTable). path is the source table; destinationPath is the full path of the new table to create (its parent must exist); rows is the list of row names to include. Returns the new table's path. |
| `biouml_bsa_track_genome` | Report or set the genome of a track (genome build id + sequences collection) — the headless equivalent of the track's 'Genome (sequences collection)' property. Analyses that convert between builds rely on it to tell which build the positions are in (e.g. whether a track needs a liftover to hg38). VCF imports take the genome from the file's ##reference header when it names one; use this tool to check or correct it. path is the track; genome (optional) is a build id or alias ('hg19', 'GRCh37', 'hg38', ...) or the path of a sequences collection; omit it to only report. Returns {path, genomeId, sequences}, plus changed:true when set, plus the installed genomes (availableGenomes[{genomeId, name, sequences}]) when listGenomes is true or the track has no sequences collection. |
| `biouml_bsa_save_selection` | Save selected items of sourcePath into a new collection or track at destinationPath (its full path) — the web UI's 'Save selection' menu items; names are the items to save. op models (default): site models of a site-model collection, into a new SiteModelCollection. op track: sites of a track, into a new track. Returns {created, count}. |
| `biouml_brain_generate` | Generate models from the brain diagram path — the web UI's 'Generate ...' menu items; the new diagrams are saved. op equations: deploy the regional/cellular/receptor model equations into new diagrams. op composite_diagram: build a composite diagram wiring the cellular and regional submodels. op multilevel_model: deploy the submodels and build the multi-level agent-model diagram. Returns {operation, path, status, completed}. |
<!-- END GENERATED TOOL TABLE -->

## Perspectives (task-oriented entry points)

Perspectives are the entry points users choose in the web UI (Default, GTRD, Microbiome, Regulatory
mutations, …; plugins contribute more). `biouml_perspective_list` tells an agent which perspectives the
user may open and, for each, its description, repository roots and *entry points* — the analyses,
workflows and collections work usually starts from. Pass an entry point's path to
`biouml_analysis_describe` (analyses, workflows) or `biouml_repo_describe` (collections).

The perspective in effect filters importers and exporters (its `importers`/`exporters` rules), so it changes
what `biouml_repo_import` and `biouml_repo_export` (`op: formats`) and import autodetection offer. Until
`biouml_perspective_select` is called, that is the perspective the user last chose in the web UI.
`biouml_perspective_select` switches it **for the MCP session only**; the user's saved perspective is not
changed. When the client authenticates with the web UI's own `JSESSIONID`, the MCP session *is* that
browser session, so the selection applies there too until the user picks a perspective in the UI.

`biouml_perspective_select` also reads the perspective's intro page (the start page the web UI shows) when
the web UI files are deployed next to the server, and returns what it offers users as an `intro` block:
further entry points (each with an `exists` flag), `analysisPresets` (an analysis plus the parameter values
the page passes — checked against the analysis, so a value it no longer takes is dropped — and `chosenByUser`,
the parameters the user sets; pass them to `biouml_analysis_run`), `pathTemplates` (repository paths the page's
scripts build, e.g. `data/Projects/{currentProject}/Data`), `exampleProjects` (the user's
projects the page lists as demo projects), `importTargets`, `help` links and a page `summary`. For a wizard
(elements with a `step-id` attribute) it returns its `steps`: each with its title, the
first paragraphs of its help page (`help/<step-id>.shtml` next to the wizard) and the page's path, which
`biouml_perspective_page` reads in full. Perspectives without a declared description are described by the
page's heading (a wizard's page title) and first paragraph (a wizard's first help paragraph). Pages are parsed
statically: links and string literals in their inline scripts and their own script files (not shared libraries
under a `lib` folder, nor minified files); scripts are not run. Server-side includes (`<!--#include file=...-->`)
are expanded as Tomcat does for `.shtml` pages, and pages they embed with an iframe or jQuery `.load()` are
followed. The web UI is found at the `-Dbiouml.web.path`
location (a directory or `.war`) if set, otherwise from the embedded Tomcat's `bioumlweb.war`, otherwise at
`webapps/bioumlweb` (directory, then `.war`) under `catalina.base` — e.g. `/var/lib/tomcat9/webapps/bioumlweb`
on a Debian `tomcat9` install — or under `<biouml.server.path>/tomcat`. Without it there is no `intro` block.

A perspective may also have a *guide* — how to do with MCP tools what its start page does, written for agents —
returned as `guide` by `biouml_perspective_select` (`guide: true` in the list). Guides are bundled with this plugin
in `biouml/plugins/mcp/support/resources/perspective-guides` (`index.properties` maps a perspective name to a
`.txt` file); a directory with the same layout set by
`-Dbiouml.mcp.guides` wins over them, so a site can add or update guides without a rebuild.

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
{"method":"tools/call","params":{"name":"biouml_task","arguments":{"op":"status","taskId":"<taskId>"}}}

// 5. Read the result (table columns + first rows, or a folder's children).
{"method":"tools/call","params":{"name":"biouml_analysis_get_result","arguments":{"path":"<resultPath>"}}}
```

## Example: build a diagram and simulate it

```jsonc
{"method":"tools/call","params":{"name":"biouml_diagram_create","arguments":{"parentPath":"mydata/diagrams","name":"decay","type":"math"}}}
{"method":"tools/call","params":{"name":"biouml_diagram_add_node","arguments":{"diagramPath":"mydata/diagrams/decay","name":"X","nodeType":"Substance"}}}
{"method":"tools/call","params":{"name":"biouml_diagram_export","arguments":{"diagramPath":"mydata/diagrams/decay","targetDir":"/tmp"}}}
{"method":"tools/call","params":{"name":"biouml_simulation_start","arguments":{"diagramPath":"mydata/diagrams/decay"}}}
```

## Example: extend an SBML model

SBML models are stored as SBGN diagrams and written back to SBML on every save, so model elements
must carry their model roles. `add_reaction`, `add_equation` and `set_parameters` create them the way
the diagram editor does; each call saves the diagram, and a call that fails is undone.

```jsonc
// copy first, then edit the copy
{"method":"tools/call","params":{"name":"biouml_repo_copy","arguments":{"op":"element","srcPath":"data/Examples/model","destPath":"data/Collaboration/My project/Data/model v2"}}}
// a new species V in compartment "default" (its variable is $default.V)
{"method":"tools/call","params":{"name":"biouml_diagram_add_node","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","name":"default/V","nodeType":"Species"}}}
// S -> V with mass action; k_vac is declared with value 0.006
{"method":"tools/call","params":{"name":"biouml_diagram_add_reaction","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","name":"vaccination","formula":"k_vac*$default.S","reactants":["S"],"products":["V"],"parameters":{"k_vac":0.006}}}}
// an assignment rule
{"method":"tools/call","params":{"name":"biouml_diagram_add_equation","arguments":{"diagramPath":"data/Collaboration/My project/Data/model v2","variable":"Total","formula":"$default.S + $default.V"}}}
```

In scripts (`biouml_repo_script` with `op: run`, type `js`) the `model` host object does the same:
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
// 4. export the result, then download the file in base64 chunks (offset = nextOffset until done; check sha256)
{"method":"tools/call","params":{"name":"biouml_repo_export","arguments":{"op":"formats","path":"data/Projects/P/Data/Filtered/sample_both"}}}
{"method":"tools/call","params":{"name":"biouml_repo_export","arguments":{"path":"data/Projects/P/Data/Filtered/sample_both","format":"<one of the formats>"}}}
{"method":"tools/call","params":{"name":"biouml_repo_export","arguments":{"op":"download","downloadId":"<downloadId>","offset":0}}}
```

`biouml_analysis_describe` lists the accepted values of choice parameters such as `mode`
(`allowedValues`, at most 200; `allowedValuesCount` gives the total).

## Limitations

- **Interactive-only actions** — `biouml_repo_action` (`op: run`) only invokes actions classified as
  headless-safe. Actions that open documents or show input dialogs (e.g. Open, Login, most
  "New …" wizards) return `requires_interactive_ui` instead of running. The classification is
  default-deny: an action is headless-safe only if it is on an explicit allowlist.
- **Async semantics** — `biouml_analysis_run` with `sync:false` queues the analysis and returns a
  task id; poll with `biouml_task` (`op: status`) and read the output with `biouml_analysis_get_result`.
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
