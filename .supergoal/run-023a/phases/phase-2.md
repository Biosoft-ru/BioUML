SUPERGOAL_PHASE_START
Phase: 2 of 6 — Repository tool layer (discovery + actions)
Task: Expose the repository model and all context-menu actions as MCP tools via PluginActions + DataElementPath
Mandatory commands: mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am; cd src && ant compile; mvn -pl src test -Dtest='Mcp*Test'
Acceptance criteria: 9
Evidence required: tool list printed (tools/list result with ≥8 tools); sample get_actions + run_action transcript; test log tails
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest='Mcp*Test'` → exit 0

## Evidence required
- `tools/list` (or in-process catalog dump) listing all `biouml_repo_*` tools with descriptions
- Test transcript: list/describe/search on fixture with exact child names asserted
- `get_actions` output for a folder (ActionCommandKeys + names)
- create-folder via run_action, then re-list showing the new child
- Dry-run vs real remove assertions
- Surefire output for all `Mcp*Test`

## Why
This is the core of "what users do via the repository context menus becomes MCP-callable." The architecture (recon) gives a clean mapping: every repository node is a `DataElement` at a `DataElementPath`; `new PluginActions().getActions(element)` returns the exact same actions the right-click menu shows, each with a stable `ActionCommandKey` — those become tool names for the dynamic "run action" path, while the high-value actions get first-class typed tools.
## Work
Implement in `src/biouml/plugins/mcp/tools/repo/` + `support/`:
1. **RepositoryAccess support class** (`McpRepositorySupport`):
   - `resolve(String path)` → `DataElement` via `CollectionFactory.getDataElement(path)`; wrap failures as MCP tool errors with the resolved-path context.
   - `describe(DataElement)` → JSON-serializable map: name, class, path, child count, child names (first 50), collection type.
   - `list(String path)` → children summary (name + path + type) for the first 100 children.
   - `search(String query, String scope)` → use the existing Lucene/`DataSearch` facilities (or simple recursive name match as fallback) returning matching paths.
   - Session context: resolve repository roots (`data`, `databases`, `analyses`, `data_resources`) from the BioUML repository config (mirror how `run.sh`/launcher wires them); expose `list_collections` returning top-level collections.
2. **First-class tools** (typed input schemas, JSON):
   - `biouml_repo_collections` — list top-level collections (databases, data, analyses, ...).
   - `biouml_repo_list` — `{path}` → children.
   - `biouml_repo_describe` — `{path}` → element metadata.
   - `biouml_repo_search` — `{query, scope?}`.
   - `biouml_repo_get_actions` — `{path}` → the `ActionCommandKey`/name/description list from `PluginActions.getActions` (the discovery tool that makes the action catalog self-describing).
   - `biouml_repo_run_action` — `{path, action}` where `action` is an `ActionCommandKey`; resolves the action from `PluginActions`, sets the target via `putValue(PARAMETER, path)` / `setDataElement`, invokes `actionPerformed(null)` in a headless-safe way, and returns a structured result (or error) — with a **safety allowlist**: GUI-only actions (Open, dialogs requiring user input like Login) must be reported as "requires interactive UI" rather than invoked, UNLESS the action's perform path works headless (elementActions that mutate the repository are OK).
   - `biouml_repo_create_folder` — `{parentPath, name}`.
   - `biouml_repo_remove` — `{path}` (confirm-less but with a `dryRun` boolean defaulting to false; returns what WOULD/IS deleted).
   - `biouml_repo_import` — `{parentPath, files:[{path, type?}]}` mapping to the `ImportElementAction`/`DataElementImporterRegistry` headless path.
   - `biouml_repo_export` — `{path, targetDir}` via `DataElementExporterRegistry`.
3. **Headless action invocation harness** (`McpActionInvoker`): wraps `javax.swing.Action` execution off-EDT (actions are invoked from the repository pane on the EDT; here we must guard: run in a headless-safe manner, capture exceptions, timeout ~60s). Log each invocation (action, path, duration, result).
4. **Tests** (`_test/`):
   - `McpRepositoryToolsTest` — with a small in-memory/temp-file `LocalRepository` fixture: resolve/list/describe/search work; get_actions on a folder returns ≥ the expected keys (Create folder, Export element, ...); run_action `create-folder` actually creates a child verifiable by re-list.
   - Fixture must be temp-dir based and cleaned up.
## Acceptance criteria
1. `biouml_repo_collections` returns ≥3 collections (databases, data, analyses) in the test fixture.
2. `biouml_repo_list`/`describe`/`search` return correct results on the fixture (assert exact child names).
3. `biouml_repo_get_actions` on a fixture folder returns an array containing at least `cmd-create-folder` (or its actual key) and each entry has name+description non-empty.
4. `biouml_repo_run_action` create-folder mutates the fixture: subsequent list shows the new child.
5. `biouml_repo_remove` with `dryRun=true` deletes nothing (assert); with `dryRun=false` removes (assert).
6. GUI-only actions (e.g. Open/Login) are NOT silently invoked — the tool returns an explicit `requires_interactive_ui` error (assert on one).
7. Every tool returns a stable JSON shape `{ok, data?, error?, code?}`.
8. All `Mcp*Test` under the plugin pass (`mvn -pl src test -Dtest='Mcp*Test'`).
9. Both builds green (mvn package + ant compile).
## Notes
- The exact `ActionCommandKey` strings are discovered at runtime (print them in tests); do not hardcode assumptions beyond the create-folder key pattern — verify against `ru.biosoft.access.generic.CreateFolderAction`'s declared key.
- Actions that open documents (OpenDocumentAction) are inherently GUI; classify them as non-invokable headless.
- Keep tool names prefixed `biouml_repo_` for namespace clarity (MCP servers expose a flat tool list).
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END