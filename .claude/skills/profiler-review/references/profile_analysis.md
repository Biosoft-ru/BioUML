# Profile Analysis Guide

Deep reference for interpreting async-profiler output and applying optimizations in the BioUML codebase.

## Understanding Profile Output Formats

### Collapsed Stacks

Format: `function1;function2;function3 sample_count`

Each line is a call chain (bottom → top) followed by the number of samples where this exact chain was observed. Sorted by sample count descending.

**How to read:**
- The rightmost function is the leaf (where CPU time was spent)
- The leftmost function is the entry point (usually the task handler)
- Sample count ≈ proportion of CPU time spent in this chain

### Tree Profile

Hierarchical view showing CPU time distribution across call chains. Each node shows:
- Function name
- Self time (time spent in the function itself, excluding callees)
- Total time (including all descendants)

### Traces

Individual call chains ranked by sample count. Each trace shows the full stack from entry to leaf.

## Common BioUML Hot Spots

Based on the codebase structure, these areas commonly show up as hot paths:

### 1. Diagram Rendering (`biouml.standard`, `biouml.model`)

Look for:
- Repeated layout computations (recalculate on every frame)
- String concatenation in render loops (use StringBuilder)
- Unnecessary object creation in paint methods
- Redundant coordinate transformations

**Optimization patterns:**
- Cache layout results with invalidation on model change
- Pre-compute bounding boxes
- Reuse graphics context objects

### 2. Simulation Solvers (`biouml.plugins.simulation`, `ru.biosoft.analysis`)

Look for:
- Per-step allocations in ODE solvers (JVode, etc.)
- Repeated math function calls (cache results)
- Unnecessary synchronization in shared state access

**Optimization patterns:**
- Pre-allocate working arrays (see commit `c5922eeb` for JVode per-step allocation fix)
- Use fixed-point arithmetic where precision allows
- Batch state updates to reduce lock contention

### 3. Data Access (`ru.biosoft.access`, `ru.biosoft.table`)

Look for:
- N+1 query patterns in repository access
- Repeated parsing of the same configuration
- Unnecessary deep copies of data structures

**Optimization patterns:**
- Cache parsed configurations (ServerMonitorConfig already uses this pattern)
- Use lazy loading for large data sets
- Implement object pooling for frequently created/destroyed objects

### 4. Plugin Loading (`biouml.workbench`, OSGi layer)

Look for:
- Repeated plugin.xml parsing
- Synchronous extension point resolution on hot paths
- Unnecessary class loading

**Optimization patterns:**
- Cache extension point results
- Lazy-initialize plugin resources
- Use weak references for optional dependencies

## Optimization Decision Tree

```
High sample count in function?
├── Function called in tight loop?
│   ├── Can result be cached? → Memoize / add field cache
│   ├── Can loop be eliminated? → Batch / vectorize
│   └── Can allocation be moved out? → Pre-allocate
├── Function involves I/O?
│   ├── Can it be async? → Use async-profiler's task tracking
│   └── Can it be batched? → Buffer writes
├── Function holds a lock?
│   ├── Is critical section large? → Reduce scope
│   └── Is there a lock-free alternative? → Use AtomicReference, etc.
└── Function is native/JNI?
    ├── Can work be done in Java? → Avoid JNI overhead
    └── Is it waiting on OS? → Check for busy-wait (pthread_cond_timedwait)
```

## Stack Trace Interpretation

### Native frames

Frames like `__pthread_cond_timedwait`, `epoll_wait`, `read` indicate:
- Thread is blocked on a condition variable → check for busy-wait loops
- Thread is waiting on I/O → check for synchronous network/disk calls
- Thread is in a sleep/poll loop → consider event-driven alternative

### GC-related frames

Frames like `java.util.Arrays.copyOf`, `java.lang.StringBuilder.append` in hot paths indicate:
- Excessive string building → use pre-sized StringBuilder
- Array resizing in loops → pre-allocate with known size
- Object creation in tight loops → reuse or pool objects

### Reflection frames

Frames involving `java.lang.reflect.Method.invoke`, `Class.getDeclaredMethod`:
- Reflection is slow → cache Method objects or use direct calls
- Consider code generation (ByteBuddy, ASM) for hot reflection paths

## Verification Checklist

After implementing optimizations:

1. **Correctness**: Do existing tests still pass? (`mvn -pl src test`)
2. **Build**: Do both build systems compile? (`mvn package -DskipTests && cd src && ant compile`)
3. **No regression**: Does the profile show improvement on next run?
4. **Style**: Does code match surrounding conventions? (check CLAUDE.md)
5. **OSGi wiring**: If packages changed, are MANIFEST.MF and plugin.xml updated?

## Profile Summary Structure Reference

The server returns this structure:

```
=== BioUML Profile Summary ===
Generated: <timestamp>

--- Task Metadata ---
Task ID: <id or N/A>
User: <user or N/A>
Type: <type or N/A>
Source: <source or N/A>
Started: <start time>
Duration: <duration>

--- Collapsed Stacks (Top 100 by Sample Count) ---
<stack> <count>
...

--- Tree Profile (Hierarchical Call Chains) ---
<hierarchical view or "not available">

--- Traces (Call Chains by Sample Count) ---
<call chains or "not available">

--- Instructions for AI Agent ---
<guidance text from server>
```

Focus analysis on the Collapsed Stacks section when Tree/Traces are unavailable (common for JVM-wide profiling).
