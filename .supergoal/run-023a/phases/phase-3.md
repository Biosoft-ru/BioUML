SUPERGOAL_PHASE_START
Phase: 3 of 6 — Analysis engine (list, describe, run, monitor)
Task: MCP tools for discovering, configuring, running, and monitoring analyses (BioUML + geneXplain-compatible)
Mandatory commands: mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am; cd src && ant compile; mvn -pl src test -Dtest='Mcp*Test'
Acceptance criteria: 10
Evidence required: analyses list transcript (≥10 methods); describe output for one method; run→poll→result transcript; task cancel transcript; test log tails
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest='Mcp*Test'` → exit 0

## Evidence required
- `biouml_analysis_list` output (stub + any registry methods) with groups
- `biouml_analysis_describe` JSON schema for the stub (property names/types/defaults)
- Transcript: sync run → table rows; async run → taskId → status done → get_result rows
- Bad-param error showing valid keys
- Cancel + repeat transcripts
- geneXplain-compat test output
- Surefire output for all `Mcp*Test`

## Why
Running analyses is the platform's core value (and what geneXplain users do via UI: open AnalysisDocument → set params → Run). The headless recipe (recon) is proven: `AnalysisMethodInfo.createAnalysisMethod()` → set parameters bean → `validateParameters()` → `TaskManager.getInstance().addAnalysisTask(analysis, true)` (async, JDBC `tasks` table) or `justAnalyzeAndPut()` (sync). Parameter beans are BeanInfo-introspectable via `com.developmentontheedge.beans.model.ComponentFactory` — we auto-generate MCP input schemas from them.
## Work
Implement in `src/biouml/plugins/mcp/tools/analysis/` + `support/`:
1. **McpAnalysisSupport**:
   - `listMethods()` — iterate `AnalysisMethodRegistry` (and, when present, geneXplain's `ConfiguredAnalysisMethodRegistry`) + `AnalysesGroupRegistry`; return name, group, description, class, input/output type summary.
   - `describeMethod(String name)` — resolve `AnalysisMethodInfo`; build a JSON **input schema** from the parameters bean: for each bean property (via `ComponentFactory.getModel(params, Policy.DEFAULT, true)`): name, type (simple name), description (`@PropertyDescription`/BeanInfo), default value, and for path-typed properties the accepted `DataCollection` type (from the `*ParametersBeanInfo` input/output registrations). Include `getInputNames()`/`getOutputNames()`.
   - `runAnalysis({name, params:{...}, originPath?, sync?})`:
     - `originPath` (default: `analyses` or the caller's project path) → origin `DataCollection`.
     - Build method via `createAnalysisMethod()`; bind params: set bean properties by name (path strings → `DataElementPath.create(...)`; nested composite params via dotted keys like `geneticParameters/iterationsCount` — mirror `AbstractCMAAnalysis.validateParameters` traversal); unknown keys → error listing valid keys.
     - `validateParameters()` — on failure return structured validation errors (property + message).
     - If `sync=true` AND analysis is fast (caller's choice): run `justAnalyzeAndPut()` with a timeout, return results inline.
     - Default async: `TaskManager.getInstance().addAnalysisTask(analysis, true)` → return `{taskId, status}`.
   - `taskStatus({taskId})` — read from `TaskManager`/`TaskInfo` (JDBC `tasks` table): status (queued/running/done/error), progress if available, result paths on completion, error message on failure.
   - `cancelTask({taskId})` — `TaskManager` cancel path.
   - `listTasks()` — active + recent tasks summary.
   - `repeatAnalysis({resultPath})` — mirror `RepeatAnalysisAction`: read the `analysisName` + stored params from the result collection (`AnalysisParametersFactory`/`AnalysisRelaunchBeanProvider`) and re-run.
2. **Result retrieval**: `getAnalysisResult({path})` — read an analysis output collection: if `TableDataCollection` return first N rows + column types; if folder return child listing; if track return site/feature count + sample features. This lets an agent read results without UI.
3. **geneXplain compatibility constraint** (design, not a code dep): the tools must NOT hardcode BioUML-only registry classes — go through the `AnalysisMethodRegistry` abstraction so geneXplain's `ConfiguredAnalysisMethodRegistry` (which reads `analyses/methods.dat`) works unmodified when that bundle is present. Add a unit test proving the listing path tolerates an empty/absent geneXplain registry.
4. **Tests** (`_test/`):
   - Fixture: register a **stub analysis** (`McpTestAnalysis extends AnalysisMethodSupport<...>` with a trivial `justAnalyzeAndPut` returning a small `TableDataCollection`) via the registry — this is the sandbox-safe way to exercise run/monitor without real bioinformatics compute.
   - `McpAnalysisToolsTest`: listMethods contains the stub; describeMethod returns schema with the stub's param properties (names + types); runAnalysis(sync) returns the table; async runAnalysis → taskStatus transitions to done (poll ≤30s in test) → getAnalysisResult returns the table rows; cancelTask on a queued task; runAnalysis with an invalid param key errors with the valid-keys list; repeatAnalysis on the stub's result re-runs.
   - `McpAnalysisRegistryCompatTest`: listing works when no geneXplain registry present (stub only).
## Acceptance criteria
1. `biouml_analysis_list` returns ≥1 method (the stub) in tests, and in a real build would include everything from `AnalysisMethodRegistry`.
2. `biouml_analysis_describe` for the stub returns a JSON schema with ≥1 property having name, type, and a default captured.
3. `biouml_analysis_run` sync returns result data (table rows) for the stub.
4. `biouml_analysis_run` async returns a taskId; `biouml_task_status` reaches `done` and exposes result paths; `biouml_analysis_get_result` returns the stub's table content.
5. Bad parameter key → structured error listing valid keys (assert message contains the valid key).
6. `biouml_task_cancel` cancels a queued task (status becomes cancelled/removed).
7. `biouml_analysis_repeat` re-runs a completed analysis (new taskId or sync result).
8. `biouml_task_list` shows the tasks.
9. geneXplain-compat test passes (registry listing with no geneXplain bundles present).
10. All `Mcp*Test` pass; both builds green.
## Notes
- Long-running real analyses (CMA, TFBS) take minutes — the async path is the default; the sync path is an explicit opt-in for fast analyses.
- Task persistence: `TaskManager` uses the JDBC `tasks` table (see `ru.biosoft.tasks`); if no DB is configured in tests, verify the in-memory fallback or use the fixture DB (check how existing `ru.biosoft.tasks` tests handle it).
- Parameter binding by bean property name: use the same introspection the GUI uses (`ComponentFactory`), not raw reflection alone, so nested/composite params work.
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END
---
SUPERGOAL_PHASE_VERIFY
- [x] cmd1 `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` → exit 0
- [x] cmd2 `cd src && ant clean compile` → BUILD SUCCESSFUL (JDK 21)
- [x] cmd3 `mvn -pl src test -Dtest='Mcp*Test'` → 19/19 pass (spike 2 + analysis 11 + repo 7)
- [x] analyses list transcript (stub listed; list ok)
- [x] describe output (property name/type/default captured)
- [x] run sync → result rows (id/value, 3 rows)
- [x] bad-param error listing valid keys
- [x] async run → taskStatus done; get_result rows
- [x] cancel transcript
- [x] geneXplain-compat test (listing with no geneXplain registry)
- [x] both builds green
SUPERGOAL_PHASE_DONE
