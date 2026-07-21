# ROADMAP — Server Monitoring & Profiling

## Overview
A BioUML plugin (`biouml.plugins.servermonitor`) that monitors running tasks, profiles slow ones with async-profiler, and serves results via the existing support API.

## Phases

### 1. Plugin Skeleton & Configuration
Create the plugin structure (source + OSGi + Maven) with configuration support.
- **Deliverables**: Plugin directories, MANIFEST.MF, plugin.xml, pom.xml, ServerMonitorConfig class
- **Acceptance criteria**:
  1. Plugin directory structure exists under `src/biouml/plugins/servermonitor/` and `plugconfig/biouml.plugins.servermonitor/`
  2. MANIFEST.MF has correct Bundle-SymbolicName and Require-Bundle for core modules
  3. plugin.xml declares the monitor as an extension (no runtime effect yet, but valid)
  4. ServerMonitorConfig class with all configurable parameters (threshold, interval, profiler path, profile dir, periodic interval, max profiles)
  5. Default configuration values are sensible (3600s threshold, 60s check interval, 1800s periodic interval)
  6. Maven compiles the plugin source without errors
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: Compilation output showing 0 errors

### 2. Async-Profiler Wrapper
Implement the profiler integration layer: download, locate, and invoke async-profiler.
- **Deliverables**: AsyncProfilerWrapper class, ProfilerResult class, profiler download utility
- **Acceptance criteria**:
  1. AsyncProfilerWrapper class with start/stop/status methods
  2. Auto-detects async-profiler binary (profiler.sh) from configurable path or downloads it
  3. Generates profile in HTML flamegraph format by default
  4. Returns structured ProfilerResult (path to output, duration, thread count)
  5. Handles missing async-profiler gracefully (returns error, doesn't crash)
  6. Supports profiling specific thread IDs
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: Class compiles; method signatures visible in output

### 3. Thread Tracking Integration
Track which TaskInfo runs on which Thread, enabling profiler to target specific tasks.
- **Deliverables**: TaskThreadTracker class, modified TaskLaunch integration
- **Acceptance criteria**:
  1. TaskThreadTracker class maintains Thread ID → TaskInfo mapping
  2. ThreadLocal-based tracking in TaskLaunch (set on task start, clear on completion)
  3. Fallback: thread name matching ("Task: " prefix) when ThreadLocal unavailable
  4. getAllTaskThreads() returns Map<Long, TaskInfo> (threadId → taskInfo)
  5. getTaskThreads(long taskId) returns Set<Long> of thread IDs for a task
  6. Thread tracking is thread-safe (ConcurrentHashMap-based)
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: Map interface visible; thread-safe data structures used

### 4. Monitoring Service
The core monitoring loop: periodic task checks, slow task detection, profiler triggering.
- **Deliverables**: MonitoringService class, integration with TaskManager lifecycle
- **Acceptance criteria**:
  1. MonitoringService runs as a background daemon thread
  2. Periodic check of all running tasks (configurable interval, default 60s)
  3. Detects tasks exceeding the slow threshold (default 3600s)
  4. Triggers async-profiler for slow tasks (via AsyncProfilerWrapper + TaskThreadTracker)
  5. Stores profiles in configurable directory with metadata JSON
  6. Supports periodic profiling of random tasks (configurable interval, default 1800s)
  7. Starts automatically when TaskManager.init() is called
  8. Stops cleanly on server shutdown
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: Service lifecycle methods (start/stop) visible; check loop present

### 5. API Endpoint
New `/biouml/support/profile` endpoint for managing and retrieving profiles.
- **Deliverables**: Extended SupportServlet dispatch, ProfileAPI class
- **Acceptance criteria**:
  1. New `profile` command in SupportServlet dispatch (via `localAddress.endsWith("profile")`)
  2. Sub-commands: `list` (list profiles), `get` (retrieve profile), `stop` (stop current profiling), `status` (monitoring status)
  3. `list` returns JSON array of profile metadata (id, task, user, type, timestamp, duration, path)
  4. `get` returns the profile file (HTML or text) or its metadata
  5. `stop` stops active profiling for a task
  6. `status` returns monitoring service status (running/stopped, last check, slow tasks count)
  7. All endpoints require admin authentication (existing checkAdmin() pattern)
  8. Profiles are served with correct MIME type (text/html for flamegraphs)
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: Endpoint methods compile; JSON response structure visible

### 6. Polish & Harden
Error handling, documentation, edge cases, cleanup.
- **Deliverables**: Error handling, documentation, edge case fixes
- **Acceptance criteria**:
  1. All file I/O wrapped in try-catch with proper logging
  2. Profile directory auto-created if missing
  3. Old profiles auto-cleaned (configurable max age, default 7 days)
  4. Concurrent profiling attempts handled (max 1 active profile)
  5. README.md in plugin directory with setup instructions
  6. async-profiler download handles network failures gracefully
  7. No `System.out.println` in production code (use java.util.logging)
  8. No `TODO` or `FIXME` comments left in code
- **Mandatory commands**: `mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests`
- **Evidence required**: grep shows no TODO/FIXME; logging used throughout

## Key Assumptions
- async-profiler binary is available for Linux x86_64 (will auto-download if not present)
- BioUML server runs as a single JVM process (not containerized with PID namespace issues)
- The monitoring plugin runs on the same server as BioUML (not a remote monitor)
- Profile storage is local disk (not remote/S3)
- HTML flamegraph format is the primary output (most useful for visual analysis)

## Top Risks & Mitigations
1. **Thread tracking reliability** → Dual approach: ThreadLocal (primary) + thread name matching (fallback)
2. **async-profiler permissions** → Document that profiler must run as same user as BioUML JVM
3. **Profiling overhead** → Configurable limits: max concurrent profiles, max profile duration
