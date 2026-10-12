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

- Host objects: `data` (repository: `data.get(path)`) and `model` (read helpers). `Packages.*` gives full Java access.
- Rhino gotchas:
  - Java strings are not JS strings: wrap in `String(x)` before `===`, `indexOf`, or using as object keys.
  - Java arrays use `.length`; Java collections use `.size()` / `.get(i)` / `.iterator()`.
  - `SomeClass.class` does not work; use `instance.getClass()`.
  - Reflection is the fastest way to discover an API: list `obj.getClass().getMethods()` with names and `getParameterTypes()`.

## 4. Inspecting a model

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

Reaction anatomy (needed to build new ones correctly):
- Node in the compartment, kernel `biouml.standard.type.Reaction` with a `KineticLaw` formula, role `Equation` of type `scalar` for variable `$$rate_<reactionName>`.
- Each participant: a `SpecieReference` (role `reactant`/`product`, `setSpecie("default.s6")`) plus an `Edge` whose role is an `Equation` of type `rate` for `$default.s6` with formula `-$$rate_<name>` (reactant) or `$$rate_<name>` (product). **Without these edge equations the species ODEs ignore the reaction**: the model compiles but nothing flows.
- In formulas: species as `$default.s6`, parameters by bare name, time as `time`.

## 5. Editing recipes

All snippets assume `P = Packages`, `diagram`, `comp`, `em` from section 4, and end with a save:

```js
diagram.getOrigin().put(diagram);   // the working save; data.save(...) returned false here
```

### Parameters

```js
if (!em.containsVariable("Vaccination_rate")) em.declareVariable("Vaccination_rate", 0.006);
em.getVariable("Vaccination_rate").setConstant(true);
```

A constant variable cannot be changed by an event. If an event must switch a value, declare a separate non-constant variable (e.g. `Variant_factor = 1`) and multiply it into the formula.

### New species

```js
var sp = new P.biouml.standard.type.Specie(null, "V");
sp.setTitle("Vaccinated");
var node = new P.biouml.model.Node(comp, sp);
node.setLocation(new P.java.awt.Point(380, 545));
node.setShapeSize(new P.java.awt.Dimension(70, 40));
node.setRole(new P.biouml.model.dynamics.VariableRole(node, 0.0)); // initial value
comp.put(node);                     // variable becomes $default.V
```

If the model has a total such as `Population = sum of compartments`, add the new species to that formula.

### New reaction (reactant -> product)

```js
function addReaction(name, formula, reactId, prodId, x, y){
  var rx = new P.biouml.standard.type.Reaction(null, name);
  rx.setKineticLaw(new P.biouml.standard.type.KineticLaw(formula));
  var rn = new P.biouml.model.Node(comp, rx);
  rn.setLocation(new P.java.awt.Point(x, y));
  rn.setShapeSize(new P.java.awt.Dimension(15, 15));
  var r1 = new P.biouml.standard.type.SpecieReference(rx, name + "__" + reactId + "_as_reactant", "reactant");
  r1.setSpecie("default." + reactId);
  var r2 = new P.biouml.standard.type.SpecieReference(rx, name + "__" + prodId + "_as_product", "product");
  r2.setSpecie("default." + prodId);
  rx.put(r1); rx.put(r2);
  rn.setRole(new P.biouml.model.dynamics.Equation(rn, "scalar", "$$rate_" + name, formula));
  comp.put(rn);
  var e1 = new P.biouml.model.Edge(r1, comp.get(reactId), rn);
  e1.setRole(new P.biouml.model.dynamics.Equation(e1, "rate", "$default." + reactId, "-$$rate_" + name));
  var e2 = new P.biouml.model.Edge(r2, rn, comp.get(prodId));
  e2.setRole(new P.biouml.model.dynamics.Equation(e2, "rate", "$default." + prodId, "$$rate_" + name));
  comp.put(e1); comp.put(e2);
  var vn = "$$rate_" + name;         // must be declared explicitly, or generation fails with a null pointer
  if (em.getVariable(vn) == null) {
    var v = new P.biouml.model.dynamics.Variable(vn, em, em.getVariables());
    v.setInitialValue(0.0); em.put(v);
  }
}
addReaction("re_vaccination", "Vaccination_rate*$default.s6", "s6", "V", 280, 530);
```

After adding reactions, check that every equation's variable exists:

```js
var it = em.getEquations().iterator(), miss = [];
while (it.hasNext()) { var q = it.next(); if (em.getVariable(q.getVariable()) == null) miss.push(q.getVariable()); }
print("missing vars: " + (miss.join(",") || "none"));
```

### Change an existing reaction rate

Update both the kinetic law and the node's equation:

```js
var n = comp.get("re8");
n.getKernel().getKineticLaw().setFormula(f);
n.getRole().setFormula(f);
```

### Change an assignment rule / algebraic formula

```js
var it = em.getEquations().iterator();
while (it.hasNext()) { var eq = it.next(); if (String(eq.getVariable()) === "Population") eq.setFormula("..."); }
```

### New event

Events are top-level diagram nodes (not in the compartment):

```js
var stub = new P.biouml.standard.type.Stub(null, "vaccination_start", "math-event");
var node = new P.biouml.model.Node(diagram, stub);
node.setLocation(new P.java.awt.Point(40, 1385));
var a = new P.biouml.model.dynamics.Assignment("Vaccination_on", "1.0");
node.setRole(new P.biouml.model.dynamics.Event(node, "time >= Vaccination_start_day", "0", [a]));
diagram.put(node);
```

### Remove an element

```js
diagram.remove("end_of_epidemic");   // or comp.remove(name) for nodes inside the compartment
```

Before removing an event or rule, understand what it does: some models contain irreversible switches (e.g. an event that zeroes transmission forever) that block the dynamics you are adding.

### Simulation span and step

```js
var so = diagram.getAttributes().getValue("simulationOptions");
so.setCompletionTime(730.0);
so.setTimeIncrement(0.1);
```

## 6. Layout: keep new elements readable

New nodes default to overlapping existing ones. Before placing anything, list the positions of all nodes (section 4) and follow the existing layout:

- **Equations and events** (`math-equation`, `math-event` top-level nodes) usually sit in a column below the reaction scheme. Put new ones in the same column, continuing the existing vertical spacing below the last one.
- **New species and reactions** go inside the compartment. If there is no free space, enlarge it (`comp.setShapeSize(new P.java.awt.Dimension(w, newH))`) and place them in a new row below the existing scheme, near the species they connect to.
- After moving nodes, reset the attached edges so the editor redraws them: for each `Edge` whose `getInput()` or `getOutput()` was moved, call `e.setPath(null)`.
- Layout changes do not affect the math, but re-run the simulation once to confirm the diagram still loads and compiles.

## 7. Typical workflow

1. Run the original, plot a few key variables, report finals.
2. Copy to the user's project. Confirm the destination folder exists with `biouml_repo_list`.
3. Inspect: reactions and formulas, equations, events, constants, node positions.
4. Look up current literature values for any new parameters with web search, and note which values are assumptions.
5. Apply all edits in one script with `step()` logging, then save.
6. Simulate; on `Model was not generated!` read the reason, fix (usually an undeclared `$$rate_*` variable, a missing edge equation, or a preprocessed diagram), and retry. If the diagram is corrupted, remove and re-copy.
7. Plot before/after with the same variable list; save the result with `biouml_simulation_save_result` if the user wants it kept.
8. Report what changed, the key numbers, and the model's remaining limitations.

## 8. Worked example: modernising a 2020 COVID-19 SEIR model

A model built with this procedure (`Westerhoff and Kolodkin improved v2`) added:

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