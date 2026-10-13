SUPERGOAL_PHASE_START
Phase: 4 of 6 — Diagram tools + simulation
Task: Create/edit BioUML diagrams, build dynamic models, and run simulations via MCP
Mandatory commands: mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am; cd src && ant compile; mvn -pl src test -Dtest='Mcp*Test'
Acceptance criteria: 9
Evidence required: create→add-node→save transcript; describe of created diagram; simulation run+result transcript; test log tails
## Mandatory commands
0. **Environment:** every build/test command runs with `JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem` and `PATH="$JAVA_HOME/bin:$PATH"` (the shell default java is JDK 8 — Maven fails with 'invalid target release: 21' and Ant compiles at level 8 on it).
1. `mvn -q package -DskipTests -pl plugconfig/biouml.plugins.mcp -am` → exit 0
2. `cd src && ant compile` → exit 0
3. `mvn -pl src test -Dtest='Mcp*Test'` → exit 0

## Evidence required
- Transcript: create diagram → add 2 nodes + edge → describe (3 elements, types/positions)
- Node-type list (≥5)
- Saved `.dml` file on disk (ls -l)
- Export/import round-trip element count
- Solver list (≥1, Java)
- Simulation run + result rows (or documented fallback error)
- Surefire output for all `Mcp*Test`

## Why
"Complete usage instead of UI" includes the desktop's other half: drawing diagrams (nodes/edges), attaching a mathematical model (dynamics), and simulating. `biouml.model` (Diagram/Node/Edge/Compartment + `dynamics/` variables/equations/events) and `biouml.plugins.simulation` provide everything; `biouml.plugins.server.DiagramService` is the existing JSON pattern to mirror.
## Work
Implement in `src/biouml/plugins/mcp/tools/diagram/`:
1. **McpDiagramSupport**:
   - `create({parentPath, name, type?})` → new `Diagram` in the target collection (math/gene-network types via `biouml.standard`); return its path.
   - `describe({path})` → diagram JSON: element count, element summary (id, name, type, position, node-type), compartment tree, dynamic model summary (variables: name/type/initial; equations: count + first few; events count).
   - `addNode({diagramPath, name, nodeType, x, y})` — nodeType from the enum used by `biouml.standard` (Protein, Gene, RNA, Drug, ... — derive the actual enum from `Node`/standard packages at implementation time; expose the allowed list via `diagram_node_types`).
   - `addEdge({diagramPath, sourceId, targetId, edgeType, label?})`.
   - `removeElement({diagramPath, elementId})`.
   - `moveElement({diagramPath, elementId, x, y})`, `renameElement(...)`.
   - `setVariable({...})` / `addEquation({...})` / `setInitialCondition(...)` — minimal dynamic-model editing (mirror what the model editor does via `biouml.model.dynamics`).
   - `save({path})` — persist via the diagram's writer (DML) — mirror `DiagramService` save path.
   - `import_diagram({parentPath, file, format?})` — DML/SBML/BioPAX import via existing importers (`DataElementImporterRegistry`).
   - `export_diagram({path, format, targetDir})`.
2. **Simulation tools** (mirror `biouml.plugins.simulation` + interactive-simulation action):
   - `run_simulation({diagramPath, solver?, duration?, stepSize?, parameters?})` — build the simulation run (Java-based `biouml.plugins.simulation` path; solvers: Java (default), and declare others when registered), submit via TaskManager as an async task (simulations can be slow), return taskId.
   - `get_simulation_result({taskId|path})` — result table (time series: first N rows + columns) + plot file paths if produced.
   - `list_solvers()` — registered simulation solvers from the `biouml.plugins.simulation.solver` extension point.
3. **Tests** (`_test/`):
   - `McpDiagramToolsTest`: create a math diagram in a temp repo fixture; add 2 nodes + 1 edge; describe shows 3 elements with correct types/positions; rename/move mutate correctly; save writes a `.dml` file to the fixture (file exists, non-empty); import the saved file back and element count matches.
   - `McpSimulationToolsTest`: build a trivial diagram with a dynamic model (e.g. `X' = -X`, X0=1) if the dynamics API allows a minimal model; run_simulation async → task done within 60s → get_simulation_result returns time-series rows (first row t=0 X=1); assert solver list ≥1 (Java). If a minimal dynamic model proves impractical to construct headlessly in the fixture, fall back to: `list_solvers` ≥1 + run_simulation on the fixture diagram returns a structured error citing the missing model (assert error shape) — and document which path was taken.
## Acceptance criteria
1. `biouml_diagram_create` + `addNode` x2 + `addEdge` produce a diagram whose `describe` reports exactly 3 elements of the expected types.
2. `biouml_diagram_node_types` returns the real node-type list (≥5 entries).
3. `biouml_diagram_save` produces a DML file on disk (exists, size > 0).
4. Round-trip: export then import the diagram → element count preserved.
5. `removeElement`/`moveElement`/`renameElement` each mutate as asserted.
6. `biouml_simulation_solvers` returns ≥1 solver (Java).
7. Simulation criterion (binary): EITHER `run_simulation` on the minimal fixture model (X'=-X, X0=1) reaches task status `done` and `get_simulation_result` returns a time-series row with t=0 present — OR, if a minimal dynamic model cannot be constructed headlessly, a test `McpSimulationToolsTest.test_missing_model_error` (written in this phase) asserts `run_simulation` returns the exact pre-registered error code `missing_dynamic_model` with a structured message. Exactly one of the two paths holds; which one is printed as evidence.
8. All `Mcp*Test` pass; both builds green.
9. Diagram tools use the same JSON envelope `{ok, data?, error?, code?}`.
## Notes
- The dynamics model API (`biouml.model.dynamics`) may require specific setup (model generation from diagram nodes). Investigate `biouml.plugins.simulation` test fixtures in `**/simulation_test/` for a headless model-construction recipe — reuse it.
- Do not invent solvers; `list_solvers` must reflect registered extension-point solvers only.
- Node/edge IDs: use the diagram's existing ID scheme (check `DiagramElement` id generation) — `addNode` returns the assigned id.
[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
SUPERGOAL_PHASE_END