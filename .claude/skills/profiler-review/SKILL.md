---
name: profiler-review
description: This skill should be used when the user asks to "review profiler results", "analyze CPU profile", "optimize hot code", "check server performance", "run profiler review", "/profiler-review", or wants to improve BioUML code based on async-profiler output from the server monitoring plugin. Triggers on any request to fetch, analyze, or optimize based on server-side profiling data.
version: 1.0.0
---

# BioUML Profiler Review Skill

Analyzes CPU profiler results from the BioUML server monitoring plugin and suggests/implements code improvements.

## Overview

This skill fetches the latest profiler summary from a remote BioUML server, analyzes hot call chains in the BioUML codebase, implements optimizations, verifies tests pass, and creates a GitHub PR.

## Prerequisites

- `.env` file at repo root with `MONITORING_USER` and `MONITORING_PASS`
- Access to the BioUML test server at `https://biouml2test.biouml.org`
- Git configured with GitHub access (via `gh` CLI or remote)
- Dual build system: both Maven and Ant must compile after changes

## Workflow

### Step 1: Fetch Profile Summary

Run the helper script to fetch the latest profile from the server:

```bash
bash .claude/skills/profiler-review/scripts/fetch_profile.sh
```

The script reads credentials from `.env` and calls:
```
https://biouml2test.biouml.org/biouml/support/profile?action=summary&id=latest&user=$MONITORING_USER&pass=$MONITORING_PASS
```

Save the output to a temporary file for analysis.

### Step 2: Analyze Profile Output

Parse the profile summary for hot spots. Focus on these sections:

1. **Collapsed Stacks** — top functions by sample count (most frequent call chains)
2. **Tree Profile** — hierarchical CPU time distribution
3. **Traces** — individual call chains ranked by samples

For each hot function, identify:
- The file and line number (if stack traces include source info)
- The calling context (what calls this function repeatedly)
- The likely optimization opportunity (caching, algorithmic improvement, reducing allocations, avoiding synchronization)

**Prioritize functions that:**
- Appear in the top 10 collapsed stacks
- Have high sample counts relative to total
- Appear in multiple call chains
- Are called in tight loops or hot paths

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
  --body "Optimizations based on async-profiler summary from biouml2test.biouml.org.

## Profile Summary
- Duration: <from profile>
- Top hot function: <function name>
- Total samples analyzed: <count>

## Changes
- <list each optimization>

## Verification
- [x] Maven build passes (`mvn package -DskipTests`)
- [x] Ant build passes (`cd src && ant compile`)
- [x] All tests pass (`mvn -pl src test`)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

## Edge Cases

- **Empty profile** (0 samples): No optimizations needed; report that the server has no recent profiling data
- **Native frames only** (e.g., `__pthread_cond_timedwait`): The hot path is in native code or waiting on a condition variable — suggest checking for busy-wait loops or excessive thread synchronization in Java code
- **Tree/Traces not available**: Only Collapsed Stacks are populated — focus analysis on the top 100 collapsed chains
- **Profile from a specific task**: The metadata section shows task type — tailor optimizations to the task domain (diagram rendering, simulation, data import, etc.)

## Additional Resources

For detailed profile analysis patterns and optimization strategies, consult:
- **`references/profile_analysis.md`** — Deep guide on interpreting async-profiler output and common optimization patterns in BioUML
