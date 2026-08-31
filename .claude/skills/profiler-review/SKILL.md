---
name: profiler-review
description: This skill should be used when the user asks to "review profiler results", "analyze CPU profile", "optimize hot code", "check server performance", "run profiler review", "/profiler-review", or wants to improve BioUML code based on async-profiler output from the server monitoring plugin. Triggers on any request to fetch, analyze, or optimize based on server-side profiling data.
argument-hint: <Type server URL>
version: 2.0.0
---

# BioUML Profiler Review Skill

Analyzes CPU profiler results from the BioUML server monitoring plugin and suggests/implements code improvements.

## Overview

This skill fetches **all recent profiler reports** (last 24 hours, >1000 bytes) from a remote BioUML server, analyzes hot call chains across them, implements optimizations, verifies tests pass, and creates a GitHub PR.

## Prerequisites

- `.env` file at repo root with `MONITORING_USER` and `MONITORING_PASS`
- Git configured with GitHub access (via `gh` CLI or remote)
- Dual build system: both Maven and Ant must compile after changes

## Usage

Accepts a server URL as an argument. Required — no default.

```bash
/profiler-review https://biouml2test.biouml.org
/profiler-review https://ict.biouml.org
```

## Workflow

### Step 0: Fix the server URL for the rest of this run

​```bash
export PROFILE_SERVER_URL="$ARGUMENTS"
echo "Using server: $PROFILE_SERVER_URL"
​```

### Step 1: Fetch Recent Profile Reports

**IMPORTANT: Use the server URL the user provided as the skill argument.** Do NOT use any URL from the Usage examples above.

First, verify the URL you will use (replace with the user-provided argument):

```bash
echo "Using server: $PROFILE_SERVER_URL"
```

**List recent profiles** — fetch the report list filtered by age (24h) and size (>1000 bytes). Pass the server URL as the **second argument** to the script:

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh list "$PROFILE_SERVER_URL"
```

If `$PROFILE_SERVER_URL` is not set, pass the URL directly:

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh list https://ict.biouml.org
```

This returns a JSON array of profile objects, each with:
- `id` — filename (used to fetch the profile)
- `size` — file size in bytes
- `timestamp` — last modified epoch millis
- `format` — output format (collapsed, tree, traces, etc.)
- `path` — absolute path on the server
- `metadata` — optional sidecar data (triggered task, duration, etc.)

**Fetch each profile's content** — for every profile in the list, download the raw profile data. Pass the server URL as the **last argument**:

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh get <profile_id> "$PROFILE_SERVER_URL"
```

If `$PROFILE_SERVER_URL` is not set, pass the URL directly:

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh get <profile_id> https://ict.biouml.org
```

Save each profile's content to a separate temporary file (e.g., `profile_1.collapsed`, `profile_2.tree`). If a profile ID has multiple format files (`.collapsed`, `.tree`, `.traces`), fetch all of them.

**If no profiles match** (empty list): report that the server has no recent profiling data and stop.

### Step 1.5: Fetch the Sub-Process Log (external scripts)

async-profiler only samples the JVM. If a task's time is spent in an external
script (perl / R / nextflow / git), the CPU profile looks nearly empty (just
futex/park waits). The server's `SubProcessMonitor` records every long-running
external descendant process to a persistent log (`subprocesses.jsonl`) and
exposes it via `action=subProcessLog`. **Always fetch it** so a profile that
"looks idle" can be cross-checked against off-JVM work.

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh subprocess --slow-only "$PROFILE_SERVER_URL"
```

This prints a table: `timestamp  [SLOW] pid=… age=…s  <full command line>`.
Interpretation (the log is *correlational* evidence, not proof of causation —
an external process being alive is not proof the task is blocked on it, and its
absence is not proof the work was in-JVM):
- **Records present, and their timestamps line up with the profile window** →
  a plausible explanation for an "idle-looking" CPU profile is that the task's
  wall-clock time was spent in an external process. Worth investigating the
  slow sub-process (script-level profiling, I/O, missing indexes) before
  concluding the JVM is the bottleneck — but confirm the overlap rather than
  assuming it.
- **No records / "work was in-JVM"** → no long-running external process was
  *observed* during that window; the in-JVM hot-path analysis (Step 2) is the
  better starting point, but a short-lived subprocess may have gone unrecorded.

Filter to a profile's window if needed: `--since <epochMillis> --until <epochMillis>`
(from the profile's `metadata.startTime` / `endTime`). `--slow-only` keeps only
scans that contained an over-threshold process.

> Note: the `subProcessLog` action requires the server to be running a build
> that includes the persistent sub-process log (this PR). Older deployments
> return an error, which the script degrades to "0 records".

### Step 2: Analyze Profile Output

Aggregate analysis across all fetched profiles. Focus on these sections per profile:

1. **Collapsed Stacks** — top functions by sample count (most frequent call chains)
2. **Tree Profile** — hierarchical CPU time distribution
3. **Traces** — individual call chains ranked by samples

For each hot function, identify:
- The file and line number (if stack traces include source info)
- The calling context (what calls this function repeatedly)
- The likely optimization opportunity (caching, algorithmic improvement, reducing allocations, avoiding synchronization)

**Prioritize functions that:**
- Appear in the top 10 collapsed stacks across profiles
- Have high sample counts relative to total
- Appear in multiple call chains or multiple profiles
- Are called in tight loops or hot paths
- Are in the BioUML application code (not third-party or JDK internals)

**Cross-profile synthesis:** look for patterns that appear consistently across multiple profiling sessions — these indicate persistent hot spots rather than transient spikes.

### Step 3: Locate Source Code

Use the package root structure to find relevant files:

| Package | Location |
|---------|----------|
| `biouml.model.*` | `src/biouml/model/` |
| `biouml.standard.*` | `src/biouml/standard/` |
| `biouml.plugins.*` | `src/biouml/plugins/<name>/` |
| `ru.biosoft.*` | `src/ru/biosoft/` |
| `com.developmentontheedge.*` | `src/com/developmentontheedge/` |

Stack traces typically show fully-qualified class names — map them to file paths by replacing `.` with `/` and appending `.java`.

### Step 4: Implement Improvements

When optimizing, follow these patterns (in order of impact):

1. **Eliminate per-step allocations** — reuse objects in loops (e.g., pre-allocated buffers, object pools)
2. **Cache expensive computations** — memoize results that don't change per invocation
3. **Reduce synchronization** — minimize lock contention, use lock-free structures where safe
4. **Algorithmic improvements** — replace O(n²) with O(n log n) or O(1) where possible
5. **Avoid unnecessary object creation** — use primitives, string builders, or in-place mutations

**Write code that matches the surrounding code style:**
- Match comment density and naming conventions
- Follow the existing idiom (check nearby code)
- Add a comment explaining *why* the optimization was made

### Step 5: Verify Build and Tests

After making changes, verify **both** build systems compile:

```bash
mvn package -DskipTests && cd src && ant compile
```

Then run the full test suite:

```bash
mvn -pl src test
```

If tests fail, fix the breakage before proceeding. Check the Surefire exclusion list in `src/pom.xml` to understand which tests are intentionally skipped.

### Step 6: Create GitHub PR

Create a new branch, commit changes, and open a PR:

```bash
git checkout -b profiler-optimize/<short-description>
git add -A
git commit -m "perf(profiler): optimize hot paths identified by async-profiler

Co-Authored-By: Claude <noreply@anthropic.com>"
git push -u origin profiler-optimize/<short-description>
gh pr create --title "perf(profiler): optimize hot paths from async-profiler" \
  --body "Optimizations based on async-profiler analysis from $PROFILE_SERVER_URL (the server URL provided by the user).

## Profiles Analyzed
- Number of reports: <count>
- Time range: last 24 hours
- Total samples across all profiles: <count>

## Top Hot Functions
1. <function> — <samples> samples — <brief description>
2. <function> — <samples> samples — <brief description>
3. <function> — <samples> samples — <brief description>

## Changes
- <list each optimization>

## Verification
- [x] Maven build passes (`mvn package -DskipTests`)
- [x] Ant build passes (`cd src && ant compile`)
- [x] All tests pass (`mvn -pl src test`)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

## Edge Cases

- **No recent profiles** (empty list from `action=list`): Report that the server has no profiling data from the last 24 hours with size >1000 bytes. Check that the monitoring plugin is running and profiling is enabled.
- **Empty profile** (0 samples): No optimizations needed; report that the server has profiling data but no samples were captured.
- **Native frames only** (e.g., `__pthread_cond_timedwait`): The hot path is in native code or waiting on a condition variable — suggest checking for busy-wait loops or excessive thread synchronization in Java code.
- **Profile looks almost entirely idle/park but the task is slow**: The work *may* be in an **external sub-process** (perl/R/nextflow). Check the sub-process log (Step 1.5) — if a SLOW record's timestamp lines up with the profile's time window, that external program is a likely candidate for the bottleneck (confirm the overlap before concluding it, not the JVM).
- **Tree/Traces not available**: Only Collapsed Stacks are populated — focus analysis on the top 100 collapsed chains.
- **Profile from a specific task**: The metadata section shows task type — tailor optimizations to the task domain (diagram rendering, simulation, data import, etc.).
- **Small profiles** (≤1000 bytes): Likely trivial or incomplete captures — skip these in the filter; they won't yield meaningful optimizations.

## Additional Resources

For detailed profile analysis patterns and optimization strategies, consult:
- **`references/profile_analysis.md`** — Deep guide on interpreting async-profiler output and common optimization patterns in BioUML
