# Server Monitor Plugin

Monitors BioUML server tasks and profiles slow ones using async-profiler.

## Features

- Automatic detection of long-running tasks
- CPU profiling via async-profiler (HTML flamegraph output)
- Periodic profiling of random tasks (proactive bottleneck detection)
- REST API for profile management
- Automatic old profile cleanup

## Configuration

Edit `preferences.xml` (in the server root or its `conf/` directory) or use the API `setConfig` action:

| Parameter | Default | Description |
|-----------|---------|-------------|
| slowTaskThreshold | 3600 | Seconds before a task is considered slow |
| checkInterval | 60 | Seconds between task checks |
| profilerPath | ./profiling/async-profiler-3.0-linux-x64/bin/asprof | Path to async-profiler binary |
| profilerDir | ./profiling | Directory for profile output |
| maxProfiles | 50 | Maximum number of profiles to keep |
| profileDuration | 30 | Seconds to profile each task |
| periodicInterval | 1800 | Seconds between periodic profiling (0=disabled) |
| periodicMode | random | Mode: random, sample, or all |
| maxProfileAge | 604800 | Seconds before profiles are cleaned up (7 days) |

## API Endpoints

All endpoints require admin authentication via `user` and `pass` parameters.

### List profiles

```
GET /biouml/support/profile?action=list
```

Returns a JSON array of profile metadata files, sorted by timestamp (newest first).

### Get profile

```
GET /biouml/support/profile?action=get&id=<filename>&format=html|collapsed|txt
```

Returns the profile file content as base64-encoded JSON with metadata. Supports `format` parameter to select output format (default: `html`).

### AI Agent Profile Summary

```
GET /biouml/support/profile?action=summary&id=<filename>
```

Returns a text-based profile summary optimized for AI agent consumption. The response includes:
- Task metadata (ID, user, type, source, duration)
- Collapsed stacks (primary output, top 100 call chains by sample count, sorted)
- Tree profile (hierarchical call chains with CPU time)
- Traces (secondary format, top 50 call chains)
- Slow sub-processes observed during the profile's time window (see below)
- Instructions for AI agents on how to analyze the profile

This format is designed to be easily parsed by AI agents to suggest code changes.

### Sub-process log (external scripts)

```
GET /biouml/support/profile?action=subProcessLog
GET /biouml/support/profile?action=subProcessLog&slowOnly=true
GET /biouml/support/profile?action=subProcessLog&since=<epochMillis>&until=<epochMillis>
```

async-profiler only samples the JVM, so a task whose time is spent in an
external script (perl / R / nextflow / git) produces a near-empty CPU profile.
The `SubProcessMonitor` walks the OS process tree under the JVM on every check
cycle and **persists every non-empty scan** to a JSON-lines log
(`subprocesses.jsonl` in the profiler directory). This endpoint returns that
history, so a slow external process can be inspected long after it exits.

Each record is one scan:
```json
{"version":1,"timestamp":…,"checkIntervalSec":…,"thresholdSec":…,"minAgeSec":…,"count":…,
 "subProcesses":[{"pid":…,"ageSeconds":…,"slow":…,"command":"perl script.pl --opt1"}, …]}
```

- `slowOnly=true` — keep only scans that contained an over-threshold process.
- `since` / `until` — filter by record timestamp (epoch millis, inclusive). Both
  must be `>= 0` and `until >= since`, otherwise the endpoint returns an error.
- The `status` endpoint also reports `subProcessLogPath` and `subProcessLogCount`
  (served from an in-memory count, not a file scan).
- **Command-line redaction** — because the log is persisted (up to 7 days) and
  readable through this endpoint, command lines are redacted before writing: any
  `--key=value` argument whose key looks like a credential (password, token,
  secret, api/access key, etc.) has its value replaced with `***`. This prevents
  credentials passed on an external script's command line from leaking into the
  log.
- The log is self-bounding (7-day age cap, 5000-line cap) and is excluded from
  profile cleanup. Purging (a full scan) runs only when the line cap is
  exceeded or at most every 10 minutes — the hot append path never rewrites the
  file, and rewrites are applied atomically (temp file + atomic move).

### Stop profiling

```
GET /biouml/support/profile?action=stop
```

Stops all active profiling sessions.

### Force immediate profiling

```
GET /biouml/support/profile?action=profileNow&taskId=<taskName>
GET /biouml/support/profile?action=profileNow
```

Forces immediate profiling of a specific task (by `taskId`) or the entire JVM process (if `taskId` is omitted).

When profiling the entire JVM, async-profiler attaches to the JVM process (PID 1 in Docker) and profiles all threads.

**Response for single task:**
```json
{
  "type": "ok",
  "value": {
    "taskId": "my-task",
    "outputPath": "/path/to/profile_123456.html",
    "duration": 30000,
    "threadCount": 3
  }
}
```

**Response for entire JVM:**
```json
{
  "type": "ok",
  "value": {
    "taskId": "jvm",
    "outputPath": "/path/to/profile_123456.html",
    "duration": 30000,
    "threadCount": 0,
    "description": "Entire JVM process profiled (all threads)"
  }
}
```

### Monitor status

```
GET /biouml/support/profile?action=status
```

Returns monitoring service status including running state, profiler availability, slow task count, and active profiles.

### Get/Set configuration

```
GET /biouml/support/profile?action=config
GET /biouml/support/profile?action=setConfig&config=<json>
```

Get or set monitoring configuration parameters.

## Installation

### 1. Build and deploy the plugin JAR

Build the plugin JAR using Ant:

```bash
cd src
ant plugin.servermonitor
```

The JAR is produced in `target/` (or the project's plugin output directory). Copy it to the server's `plugins/` folder alongside the other BioUML plugin JARs, then restart the server.

The plugin is automatically included in `ant plugin.all` and `mvn package`.

### 2. Install async-profiler

The plugin will auto-download async-profiler on first run if not found. To install manually:

```bash
mkdir -p profiling
cd profiling
wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
tar xzf async-profiler-3.0-linux-x64.tar.gz
chmod +x async-profiler-3.0-linux-x64/bin/asprof
```

### 2. Configure

Configuration is read from the `serverMonitor` node of the BioUML preferences
(`ServerMonitorConfig.load()` calls `prefs.getPreferencesValue("serverMonitor")`
and iterates its child `DynamicProperty`s). Add a `serverMonitor` property to
`preferences.xml`, using the standard `Preferences`/`dynamicPropertySet` XML
format (the same structure as the `Global` and `GridOptions` blocks):

```xml
<property name="serverMonitor" type="com.developmentontheedge.beans.Preferences"
          display-name="Server monitor" short-description="Server monitor plugin settings">
    <dynamicPropertySet>
        <property name="slowTaskThreshold" type="java.lang.Integer" value="3600"
                  display-name="Slow task threshold" short-description="Seconds before a task is considered slow"/>
        <property name="checkInterval" type="java.lang.Integer" value="60"
                  display-name="Check interval" short-description="Seconds between task checks"/>
        <property name="profilerPath" type="java.lang.String"
                  value="/var/lib/tomcat9/profiling/async-profiler-3.0-linux-x64/bin/asprof"
                  display-name="Profiler path" short-description="Absolute path to the async-profiler binary"/>
        <property name="profilerDir" type="java.lang.String" value="/var/lib/tomcat9/profiling"
                  display-name="Profiler directory" short-description="Absolute directory for profile output"/>
        <property name="periodicInterval" type="java.lang.Integer" value="1800"
                  display-name="Periodic interval" short-description="Seconds between periodic profiling (0=disabled)"/>
        <property name="periodicMode" type="java.lang.String" value="random"
                  display-name="Periodic mode" short-description="random, sample, or all"/>
    </dynamicPropertySet>
</property>
```

Notes:

- Use **absolute** paths for `profilerDir` and `profilerPath`. The defaults are
  relative (`./profiling`), resolved against the JVM working directory
  (`user.dir`), which is not a stable location on a server (e.g. it may be `/`,
  which a non-root server user cannot write to). Prefer a directory the server
  user owns, such as under `catalina.base`.
- The `Preferences` XML loader only understands `<property>` and
  `<dynamicPropertySet>` elements. A bare `<serverMonitor>…</serverMonitor>`
  block (or any other tag) is rejected with `SEVERE: Error: Unknown tag …` and
  silently falls back to defaults — so the nested `<dynamicPropertySet>` form
  above is required.
- All keys are optional; any key you omit keeps its default (see the table
  above). You can also change values at runtime via the `setConfig` API action,
  which persists them the same way.

## Requirements

- Linux x86_64
- Java 11+ (BioUML uses Java 21)
- async-profiler must run as the same user as the BioUML JVM
- ptrace permissions (usually satisfied when running as same user)

## Profiling Output

- **HTML flamegraph**: Interactive, clickable, zoomable CPU flame graph
- **Collapsed stacks**: For further processing with external tools
- **Flat profile**: Top functions by CPU time

Profile files are stored in the `profilerDir` directory with metadata JSON sidecar files.

## Architecture

The plugin consists of:

- **ServerMonitorConfig**: Configuration management with defaults and preference loading
- **AsyncProfilerWrapper**: async-profiler integration with auto-download
- **TaskThreadTracker**: Thread-to-task mapping for profiler targeting
- **MonitoringService**: Background daemon thread with periodic checks
- **ServerMonitorPlugin**: Plugin lifecycle (init/stop)

## Testing

### Prerequisites on the test server

1. **Java 21** running (BioUML server)
2. **Internet access** — the plugin auto-downloads async-profiler from GitHub on first `init()`, unless installed manually
3. **ptrace permissions** — async-profiler needs `ptrace` access to attach to Java threads. Check with:
   ```bash
   cat /proc/sys/kernel/yama/ptrace_scope
   ```
   (0 = allowed, 1 = restricted)

### Build

```sh
cd src
ant plugin.servermonitor
```

Or build all plugins:

```sh
cd src
ant plugin.all
```

### Deploy

Copy the resulting JAR to the server's `plugins/` folder (alongside the other BioUML plugin JARs), then restart the server.

### Verify the plugin loaded

Check the API status endpoint:

```bash
curl "https://<server>/biouml/support/profile?action=status&user=<ADMIN>&pass=<PASS>"
```

Expected response (JSON):
```json
{"running": true, "profilerAvailable": true, "slowTasks": 0, "activeProfiles": 0, "lastCheck": "..."}
```

If `profilerAvailable` is `false`, check:
- async-profiler downloaded successfully (look at server logs)
- The binary is executable (`chmod +x`)
- ptrace is allowed

### Test the workflow

**a) List profiles** (should be empty initially):
```bash
curl "https://<server>/biouml/support/profile?action=list&user=<ADMIN>&pass=<PASS>"
```

**b) Check current config**:
```bash
curl "https://<server>/biouml/support/profile?action=config&user=<ADMIN>&pass=<PASS>"
```

**c) Adjust threshold** (make it shorter to trigger faster for testing):
```bash
curl -X POST "https://<server>/biouml/support/profile?action=setConfig&user=<ADMIN>&pass=<PASS>" \
  -d 'config={"slowTaskThreshold": 30, "checkInterval": 10}'
```

**d) Trigger a slow task** — run something computationally heavy in BioUML that takes longer than the threshold. The monitoring daemon will detect it and auto-profile.

**e) Check status again** — should show `activeProfiles: 1` while profiling, then `slowTasks: 1` after.

**f) Get the profile**:
```bash
curl "https://<server>/biouml/support/profile?action=list&user=<ADMIN>&pass=<PASS>"
```
Note the profile ID from the list, then:
```bash
curl "https://<server>/biouml/support/profile?action=summary&user=<ADMIN>&pass=<PASS>&id=<PROFILE_ID>"
```

**g) Periodic profiling** — enable it to profile all running tasks on a schedule:
```bash
curl -X POST "https://<server>/biouml/support/profile?action=setConfig&user=<ADMIN>&pass=<PASS>" \
  -d 'config={"periodicInterval": 600, "periodicMode": "all"}'
```

### Things to watch for

- **Server logs** — the plugin logs at INFO level; look for "Server Monitor started", "profiler initialized", "slow task detected", etc.
- **`./profiling/` directory** — profile files (HTML + JSON) land here on the server
- **`./profiler/profiler.sh`** — the async-profiler binary, auto-downloaded on first run
- **Admin auth** — all endpoints require admin login; make sure your test credentials have admin rights
- **Concurrent limit** — only 1 profile at a time; if multiple slow tasks exist, they queue

## Troubleshooting

- **Profiler not available**: Check that async-profiler binary exists and is executable, or that the download succeeded
- **No threads found**: The thread tracking may need a refresh; the monitor auto-refreshes on each check cycle
- **Permission denied**: Ensure async-profiler runs as the same user as the BioUML JVM
