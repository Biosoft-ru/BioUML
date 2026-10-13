# Recon: Repository context menu actions (2026-09-10)

## Architecture (key finding)
- Extension points: `ru.biosoft.access.repositoryActionsProvider` (legacy, returns `javax.swing.Action[]`) + `ru.biosoft.access.elementAction` (new-style `AbstractElementAction` subclasses with `priority`; ≥40 = also double-click).
- Aggregator: `ru.biosoft.access.repository.PluginActions` (`getActions(Object bean)`; elementActions sorted by priority desc, deduped by `ActionCommandKey`, then legacy providers in OSGi order).
- Menu rendering: `RepositoryPane.PopupListener` (right-mouse → JPopupMenu; accelerators wired via KeyListener).
- **MCP entry point**: `new PluginActions().getActions(dataElement)`; items identified by `DataElementPath` strings, resolved via `CollectionFactory.getDataElement(path)`.
- **ActionCommandKey** (stable machine ID like `cmd-new-workflow`) = natural MCP tool identifier. Each Action carries Name/ShortDescription/LongDescription.
- Invoke: `action.putValue(PARAMETER, path)` + `action.actionPerformed(null)` (AbstractElementAction: `setDataElement`).
- `biouml.workbench.RepositoryActionsProvider` returns null under `analyses` path — inside analyses/ only elementActions apply.
- Headless analysis run: `AnalysisMethodInfo.createAnalysisMethod()` → `TaskManager.getInstance().addAnalysisTask(method, true)`; params via `AnalysisParametersFactory` / `AnalysisRelaunchBeanProvider`.
- `biouml.workbench.ActionsInfoBuilder` = build-time app enumerating all actions (reference for machine-readable list).

## Actions by item type
### Databases (Modules)
- New Module (module.NewModuleAction), New Composite Module (xml.NewCompositeModuleAction), Remove Module, Export Module, Edit Module (xml), Import documents (ImportElementAction)
- Lucene (modules w/ index): Search (LuceneSearchAction), Index Editor (IndexEditorAction), Rebuild Index (RebuildIndexAction)
- SQL modules: Data Search (ru.biosoft.access.search.DataSearchAction)
- BioPAX modules: BioPAX Export/Import
- Chemistry collections: Open structures (OpenStructuresAction)
- Universal: Retry(100 ReinitializeAction), Login(90 LoginModuleAction), Open(50 OpenDocumentAction), Create folder(30), Export element(30)

### Data projects (user & public)
- Create folder, New data element (NewDataElementAction), New diagram (diagram.NewDiagramAction), Open as table (research OpenAsTableAction), New workflow / research / nextflow script / R script / JS script / table / test, Import, Export element, Remove, Data Search, Repeat analysis (if analysisName property), New optimization / Remove optimization, Open Physicell result (unconditional)

### Analyses / Tasks
- `analyses/Methods/<name>` = `AnalysisMethodInfo`; run via AnalysisDocument (Run/Cancel) → `AnalysisTask` → `TaskManager` (JDBC `tasks` table, TasksViewPart)
- Results (collections w/ analysisName): Repeat analysis(20 RepeatAnalysisAction)
- OptimizationAnalysisAction (priority 60) on OptimizationMethod-capable AnalysisMethodInfo
- Simulation results: New plot/Open plot (OpenPlotAction), Remove, Apply state (state.ApplyStateAction)
- Task run/kill via `TaskManager` (restartAllInterruptedTasks, cancelTask...)

### Diagrams
- Import, Remove; Open; Export element; Open interactive simulation (OpenInteractiveSimulationAction)
- Element-level (node/edge) popups: PopupActionsProvider — Edit, Pin/Unpin, Add vertex, edge straighten/snap, Rotate node, Copy node

### All elementAction registrations (priority)
New nextflow script(10), New Java code(10), Open genome browser(60 bsa OpenSequenceAction), Open track as table(70 bsa OpenTrackAsTable), Open chat(50 users), Open group chat(50), New workflow(10), New research(10), Open journal as table(50), New R script(10), New JS script(10), New table(10), Start optimization(60), New test(10), Create folder(30), Open(50)/Login(90)/Export element(30)/Retry(100)
