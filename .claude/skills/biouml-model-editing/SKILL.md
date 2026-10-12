---
name: "biouml-model-editing"
description: "Run, inspect, edit and re-simulate BioUML ODE diagrams via the BioUML MCP tools and server-side JavaScript: add species, reactions, parameters, events, fix layout, save results."
---

# BioUML model editing and simulation

Use this skill when the user wants to simulate a BioUML diagram, look at its results, or change the model (new species, reactions, parameters, events, simulation span, layout) on a BioUML server reached through the BioUML MCP tools (tool names end in `biouml_simulation_*`, `biouml_repo_*`, `biouml_diagram_*`).

## 1. Golden rules

1. **Never edit the original.** Copy first with `biouml_repo_copy_element(srcPath, destPath)`. When the user asks for a new version, copy the previous version to a new name and edit the copy.
2. **To reset a broken copy**, `biouml_repo_remove(path)` and copy again. Do not try to patch a corrupted diagram.
3. **Never call `engine.preprocess(diagram)` on the stored diagram.** Preprocessing mutates it in place (adds `RATE_OF_*` rules etc.); after saving, the model fails to compile with errors like `Duplicate assignment rules for variable RATE_OF_default_s9`. If you need to validate, only do it without saving afterwards, or better, just run the simulation.
4. **Prefer the MCP diagram tools for edits** (section 5). Each call saves the diagram; a call that fails is undone (or the diagram is re-read from its last saved version), so it never leaves a half-made element behind. Scripts change the diagram in memory only and must end with `diagram.save()`.
5. **Check `declaredParameters`** in the result of `add_reaction` / `add_equation`: identifiers of the formula that were not model variables are declared there with value 0. A species written without `$` that ends up in that list is a typo; a genuine parameter needs a value (`parameters` argument or `biouml_diagram_set_parameters`).

## 2. Running a simulation

1. `biouml_simulation_start(diagramPath)` returns a `jobID` immediately.
2. Wait (use `sleep 10`-`15` in Bash; do not poll faster than every ~10 s), then `biouml_simulation_status(jobID)` until `completed: true`. Read `message`: a successful run ends with `simulation finished`; a failed one contains `SEVERE - ... Model was not generated!` with the reason.
3. View results with `biouml_simulation_plot(jobID, variables=[...], width, height)`. It returns a PNG plus `finals` (last values). Always pass a short `variables` list; the default plots every variable and is unreadable. Species are named `$<compartment>.<id>`, e.g. `$default.s6`; parameters and assignment-rule variables by bare name, e.g. `Recovered`.
4. Avoid `biouml_simulation_result`: it returns the full time series and can overflow the context.
5. To persist a result: `biouml_simulation_save_result(jobID, destinationPath)`. The parent folder must exist (otherwise `DataElementNotFoundException`). An existing element at that path is silently overwritten. The saved element is a `biouml.standard.simulation.SimulationResult`.

## 3. Running scripts

- `biouml_repo_run_script(script, type="js")` returns a `jobID`. Wait ~8 s, then `biouml_repo_job_status(jobID)`; the `message` field contains everything written with `print()`.
- An uncaught exception can reduce the whole output to `null` or a single error line. Wrap each step:

```js
function step(name, fn){ try { fn(); print("[OK] " + name); } catch(e){ print("[FAIL] " + name + " -> " + e); } }
```

- Host objects: `data` (repository: `data.get(path)`), `model` (read and edit helpers, section 5) and `sbml`. `Packages.*` gives full Java access; it is rarely needed now.
- The `model.add*` functions throw on bad input (unknown species, unknown equation type); `step()` prints the message.
- Rhino gotchas:
  - Java strings are not JS strings: wrap in `String(x)` before `===`, `indexOf`, or using as object keys.
  - Java arrays use `.length`; Java collections use `.size()` / `.get(i)` / `.iterator()`.
  - `SomeClass.class` does not work; use `instance.getClass()`.
  - Reflection is the fastest way to discover an API: list `obj.getClass().getMethods()` with names and `getParameterTypes()`.

## 4. Inspecting a model

`biouml_diagram_describe(path)` lists every element, including those inside compartments: `id`, `path` (e.g. `default.s6`), kind, position, the `variable` of a species, the `formula` of a reaction or equation, and `source`/`target`/`role`/`stoichiometry` of reaction edges, plus all model variables with initial values. Use it first; use a script for anything it does not show (events, simulation options).

```js
var diagram = data.get(path);
var comp = diagram.get("default");        // main compartment (name may differ)
var em = diagram.getRole();                // executable model (e.g. SbmlEModel)

model.getVariableNames(diagram);           // all variables
model.getMath(diagram, "equation_3");      // formula of an equation / rule / event node
var rs = model.getReactions(diagram);      // Reaction[]: r.getName(), r.getFormula(), r.getSpecieReferences()
em.getEvents();                            // Event[]: getTrigger(), getEventAssignment()[i].getVariable()/getMath()
var it = em.getEquations().iterator();     // all equations: getType(), getVariable(), getFormula()
em.getVariable("Name").getInitialValue();  // isConstant(), setConstant(), setInitialValue()
```

Node layout: iterate `diagram.getNameList()` and `comp.getNameList()`; each node has `getLocation()`, `getShapeSize()`, `getKernel().getType()` (`reaction`, `math-equation`, `math-event`, ...). Skip `Packages.biouml.model.Edge` instances.

Reaction anatomy (what `add_reaction` / `model.addReaction` build; useful when a model does not behave):
- Node in the compartment, kernel `biouml.standard.type.Reaction` with a `KineticLaw` formula, role `Equation` of type `scalar` for variable `$$rate_<reactionName>`.
- Each participant: a `SpecieReference` (role `reactant`/`product`, `setSpecie("default.s6")`) plus an `Edge` whose role is an `Equation` of type `rate` for `$default.s6` with formula `-$$rate_<name>` (reactant) or `$$rate_<name>` (product). **Without these edge equations the species ODEs ignore the reaction**: the model compiles but nothing flows.
- In formulas: species as `$default.s6`, parameters by bare name, time as `time`.

## 5. Editing recipes

### With the MCP tools (preferred)

Ids are element names (`s6`) or paths in the diagram (`default.s6`); a new element goes into a compartment when its name is qualified (`default/V`). Species are referred to in formulas as `$default.s6`.

| Change | Tool and arguments |
|---|---|
| New species | `biouml_diagram_add_node(diagramPath, name="default/V", nodeType="unspecified", title="Vaccinated", x, y)` — returns its `variable` (`$default.V`). `nodeType` is the SBGN glyph: `unspecified`, `simple chemical`, `macromolecule` (= `Species`), `nucleic acid feature`, `perturbing agent`, `complex`; use the type of the model's existing species. `width`/`height` default to 70×40 |
| Initial value or parameter value | `biouml_diagram_set_parameters(diagramPath, parameters={"V": 100, "Vaccination_rate": 0.006})` — species by name, missing parameters are declared; `constant=true/false` applies to every name in the call |
| New reaction | `biouml_diagram_add_reaction(diagramPath, name="re_vaccination", formula="Vaccination_rate*$default.s6", reactants=["s6"], products=["V"], parameters={"Vaccination_rate": 0.006}, x, y)` — stoichiometry as `"2*s6"`; `modifiers` for catalysts |
| Participant of an existing reaction | `biouml_diagram_add_edge(diagramPath, sourceId="s6", targetId="re8", edgeType="reactant")` (`product` for reaction -> species, `modifier`) |
| Assignment rule / rate rule / initial assignment | `biouml_diagram_add_equation(diagramPath, name="population_rule", type="scalar", variable="Population", formula="$default.s1 + $default.s2", x, y)` — a missing target parameter is declared |
| Event | `biouml_diagram_add_event(diagramPath, name="vaccination_start", trigger="time >= Vaccination_start_day", assignments={"Vaccination_on": "1"}, parameters={"Vaccination_start_day": 30, "Vaccination_on": 0}, x, y)` — `delay` optional; assignments run in the given order |
| Remove an element | `biouml_diagram_remove_element(diagramPath, elementId)` — also removes its edges, reaction participants and the rate variables of removed reactions |
| Clone / merge a species | `biouml_diagram_clone_node`, `biouml_diagram_merge_clone` |
| Move / align | `biouml_diagram_move_element`, `biouml_diagram_layout` |
| Look at the diagram | `biouml_diagram_image(path, scale, maxWidth, maxHeight)` — returns the rendered diagram as a picture |

`add_reaction` creates everything the simulator needs: the node, the participant edges, the `$$rate_<name>` variable and the participants' rate equations. Do not build reactions from `add_node` + `add_edge` unless you are extending an existing reaction.

### With a script

There is no MCP tool for simulation options or editing an existing formula; use `biouml_repo_run_script`. The `model` functions create elements the same way the tools do. Always end with a save:

```js
var diagram = data.get(path);
var em = diagram.getRole();

// parameters: model.addReaction / addEvent do not declare them; do it first
var params = {Vaccination_rate: 0.006, Vaccination_start_day: 30, Vaccination_on: 0};
for (var name in params)
  if (!em.containsVariable(name)) em.declareVariable(name, params[name]);
em.getVariable("Vaccination_rate").setConstant(true);

model.addSpecies(diagram, "default/V", 0);                 // returns the variable $default.V
model.addReaction(diagram, "re_vaccination", "Vaccination_rate*$default.s6", [
  model.createSpecieReference(diagram, "s6", "reactant"),
  model.createSpecieReference(diagram, "V", "product")      // optional 4th argument: stoichiometry
]);
model.addEquation(diagram, "population_rule", "scalar", "Population", "$default.s1 + $default.V");   // the name is optional
var ev = model.addEvent(diagram, "vaccination_start", "time >= Vaccination_start_day", 0,   // the name is optional
  [model.createAssignment(diagram, "Vaccination_on", "1")]);
ev.getDiagramElement().setLocation(new java.awt.Point(40, 1385));

diagram.save();                                            // data.save(dc, path) expects the parent folder's path
```

A constant variable cannot be changed by an event. If an event must switch a value, declare a separate non-constant variable (e.g. `Variant_factor = 1`) and multiply it into the formula. If the model has a total such as `Population = sum of compartments`, add new species to that formula.

If `biouml_diagram_add_reaction` is not in the tool list, the server runs an older build whose `model.add*` functions and `add_node` do not create working model elements: ask for the server to be updated rather than building elements by hand.

### Change an existing reaction rate

The reaction's rate equation reads its formula from the kinetic law, so updating the kinetic law is enough:

```js
diagram.findNode("re8").getKernel().getKineticLaw().setFormula(f);
diagram.save();
```

### Change an assignment rule / algebraic formula

```js
var it = em.getEquations().iterator();
while (it.hasNext()) { var eq = it.next(); if (String(eq.getVariable()) === "Population") eq.setFormula("..."); }
diagram.save();
```

### Remove an element

`biouml_diagram_remove_element`, or in a script `diagram.remove("end_of_epidemic")` (`comp.remove(name)` for nodes inside a compartment) followed by `diagram.save()`.

Before removing an event or rule, understand what it does: some models contain irreversible switches (e.g. an event that zeroes transmission forever) that block the dynamics you are adding.

### Simulation span and step

```js
var so = diagram.getAttributes().getValue("simulationOptions");
so.setCompletionTime(730.0);
so.setTimeIncrement(0.1);
diagram.save();
```

## 6. Layout: keep new elements readable

New nodes default to overlapping existing ones. Before placing anything, list the positions of all nodes (section 4) and follow the existing layout; afterwards check the result with `biouml_diagram_image`:

- **Equations and events** (`math-equation`, `math-event` top-level nodes) usually sit in a column below the reaction scheme. Put new ones in the same column, continuing the existing vertical spacing below the last one.
- **Match the existing species**: same `nodeType` (glyph) and size as the species around them, a readable `title`.
- **New species and reactions** go inside the compartment: pass `x`, `y` to the tools (without them a reaction is placed between its participants, other nodes at (0, 0)). If there is no free space, enlarge it in a script (`comp.setShapeSize(new Packages.java.awt.Dimension(w, newH))`) and place them in a new row below the existing scheme, near the species they connect to.
- After moving nodes, reset the attached edges so the editor redraws them: for each `Edge` whose `getInput()` or `getOutput()` was moved, call `e.setPath(null)`.
- Layout changes do not affect the math, but re-run the simulation once to confirm the diagram still loads and compiles.

## 7. Typical workflow

1. Run the original, plot a few key variables, report finals.
2. Copy to the user's project. Confirm the destination folder exists with `biouml_repo_list`.
3. Inspect: reactions and formulas, equations, events, constants, node positions.
4. Look up current literature values for any new parameters with web search, and note which values are assumptions.
5. Apply the edits with the MCP tools (section 5); put what they cannot do (events, simulation span, formula changes) in one script with `step()` logging that ends with `diagram.save()`.
6. Simulate; on `Model was not generated!` read the reason, fix (usually a parameter left at 0 or undeclared, a typo in a formula, or a preprocessed diagram), and retry. If the diagram is corrupted, remove and re-copy.
7. Plot before/after with the same variable list; save the result with `biouml_simulation_save_result` if the user wants it kept.
8. Report what changed, the key numbers, and the model's remaining limitations.

## 8. Worked example: modernising a 2020 COVID-19 SEIR model

A model built this way (`Westerhoff and Kolodkin improved v2`; it was built with hand-made reactions, today each reaction row is one `biouml_diagram_add_reaction` call) added:

| Change | Implementation | Value used |
|---|---|---|
| Vaccination S -> V | reaction `Vaccination_rate*Vaccination_on*$default.s6`; event switches `Vaccination_on` to 1 at `Vaccination_start_day` | 0.006/day from day 30 |
| Vaccine waning V -> S | reaction `Vaccine_waning_rate*$default.V` | 0.0037/day (~9 months) |
| Natural immunity waning, recovered -> S | two reactions from the recovered compartments | 0.0021/day (median reinfection ~16 months, Lancet Microbe) |
| Leaky vaccine V -> infected | `(1 - Vaccine_efficacy)*Total_Infection_coefficient*$default.V` | efficacy 0.6 (assumption) |
| New, more transmissible variant | non-constant `Variant_factor` multiplied into the force of infection; event sets it to `Variant_multiplier` at `Variant_day` | x2.5 on day 55 |
| Lower variant severity | `Severity_factor` multiplied into all COVID death reactions; event sets it to 0.31 | death HR 0.31 Omicron vs Delta (England cohort) |
| Remove irreversible `end_of_epidemic` event | `diagram.remove(...)` | needed for reinfection waves |
| Longer horizon | completion time 730 days, step 0.1 | shows repeated waves and endemic equilibrium |

Limitations to state when reporting such a model: breakthrough infections follow the same severity path as unvaccinated ones; parameters are literature values, not fitted; derived counters like `Registered` can behave oddly once recovered people return to susceptible.