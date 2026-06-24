# Server Monitor Plugin

Monitors BioUML server tasks and profiles slow ones using async-profiler.

## Features

- Automatic detection of long-running tasks
- CPU profiling via async-profiler (HTML flamegraph output)
- Periodic profiling of random tasks (proactive bottleneck detection)
- REST API for profile management
- Automatic old profile cleanup

## Configuration

Edit `preferences_server.xml` or use the API `setConfig` action:

| Parameter | Default | Description |
|-----------|---------|-------------|
| slowTaskThreshold | 3600 | Seconds before a task is considered slow |
| checkInterval | 60 | Seconds between task checks |
| profilerPath | ./profiler/profiler.sh | Path to async-profiler binary |
| profilerDir | ./profiling | Directory for profile output |
| maxProfiles | 50 | Maximum number of profiles to keep |
| profileDuration | 30 | Seconds to profile each task |
| periodicInterval | 1800 | Seconds between periodic profiling (0=disabled) |
| periodicMode | random | Mode: random, sample, or all |
| maxProfileAge | 604800 | Seconds before profiles are cleaned up (7 days) |

## API Endpoints

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
- Flat profile (top functions by CPU time)
- Collapsed stacks (top 100 call chains by sample count, sorted)
- Instructions for AI agents on how to analyze the profile

This format is designed to be easily parsed by AI agents to suggest code changes.

### Stop profiling

```
GET /biouml/support/profile?action=stop
```

Stops all active profiling sessions.

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

### 1. Install async-profiler

The plugin will auto-download async-profiler on first run if not found. To install manually:

```bash
mkdir -p profiler
cd profiler
wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
tar xzf async-profiler-3.0-linux-x64.tar.gz
chmod +x profiler.sh
```

### 2. Configure

Add to `preferences_server.xml` under the `serverMonitor` preference node:

```xml
<serverMonitor>
    <slowTaskThreshold>3600</slowTaskThreshold>
    <checkInterval>60</checkInterval>
    <profilerPath>./profiler/profiler.sh</profilerPath>
    <profilerDir>./profiling</profilerDir>
    <periodicInterval>1800</periodicInterval>
    <periodicMode>random</periodicMode>
</serverMonitor>
```

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

## Troubleshooting

- **Profiler not available**: Check that async-profiler binary exists and is executable, or that the download succeeded
- **No threads found**: The thread tracking may need a refresh; the monitor auto-refreshes on each check cycle
- **Permission denied**: Ensure async-profiler runs as the same user as the BioUML JVM
