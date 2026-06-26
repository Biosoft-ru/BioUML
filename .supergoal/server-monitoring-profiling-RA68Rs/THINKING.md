# THINKING.md — Server Monitoring & Profiling

## Goals
1. Detect tasks running longer than a configurable threshold (default: hours)
2. Profile slow tasks using async-profiler to identify CPU bottlenecks
3. Run periodic profiling even without detected slow tasks (proactive)
4. Store and serve profiling results via the existing support API
5. Suggest code improvements based on profiling analysis

## Constraints
- BioUML runs on Java 21 with Eclipse Equinox OSGi
- Tasks execute in per-user `ThreadPoolExecutor` instances
- Thread naming convention: `"Task: <taskName>"` (set in TaskLaunch.run())
- No existing monitoring/profiling infrastructure
- async-profiler requires native binaries (Linux x86_64)
- Server is remote (platform.genexplain.com) — needs self-contained deployment
- Existing API auth: token-based admin authentication via SupportServlet

## Risks
1. **Thread tracking**: TaskPool renames threads dynamically; we need reliable mapping from TaskInfo to Thread. Mitigation: use ThreadLocal in TaskLaunch + thread name matching as fallback.
2. **async-profiler attachment**: Requires same-user or ptrace permissions on remote server. Mitigation: run as same user as BioUML process; document requirements.
3. **Profiling overhead**: Continuous profiling adds CPU/memory overhead. Mitigation: configurable intervals, profile only specific threads, limit profile duration.
4. **OSGi compatibility**: New plugin must wire correctly with existing bundles. Mitigation: follow existing plugin patterns (MANIFEST.MF, plugin.xml extensions).

## Dependencies
- Phase 2 depends on Phase 1 (plugin skeleton)
- Phase 3 depends on Phase 2 (profiler wrapper)
- Phase 4 depends on Phase 3 (monitoring service needs profiler)
- Phase 5 depends on Phase 4 (API needs monitoring service)
- Phase 6 depends on all previous

## Open Questions (assumed)
- async-profiler binary: download to a configurable local directory on first run
- Profile storage: `$BIOUML_HOME/profiling/` with per-task subdirectories
- Profile format: HTML flamegraph (most useful for analysis) + collapsed text
- Periodic profiling interval: configurable, default 30 minutes
- Threshold for "slow task": configurable, default 3600 seconds (1 hour)
- Max concurrent profiles: 1 (to limit overhead)

## Memory hits applied
- None (clean run, no prior memory)

## Tools/skills relied on
- async-profiler CLI (profiler.sh / profiler.jar)
- Java Thread API (enumerateThreads, getThreadGroup)
- BioUML OSGi plugin system (plugin.xml extensions)
- Existing SupportServlet dispatch mechanism

## Best Practices Applied
- Follow existing BioUML plugin patterns (src/ + plugconfig/ dual directories)
- Use existing SupportServlet for API (no new servlet needed)
- Configuration via DynamicPropertySet (consistent with BioUML preferences)
- Thread-safe monitoring service (synchronized access to shared state)
- Graceful degradation (monitor works even without async-profiler installed)
