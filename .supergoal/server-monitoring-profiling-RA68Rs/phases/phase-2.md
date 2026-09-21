SUPERGOAL_PHASE_START
Phase: 2 of 6 — Async-Profiler Wrapper
Task: Implement the async-profiler integration layer

## Work Description

Create the profiler wrapper that handles async-profiler lifecycle:

### 1. ProfilerResult class
```java
public class ProfilerResult {
    private final String outputPath;      // Path to the profile output file
    private final long startTime;          // Profiling start time (millis)
    private final long endTime;            // Profiling end time (millis)
    private final int threadCount;         // Number of threads profiled
    private final String[] threadIds;      // Thread IDs as strings
    private final String format;           // Output format (html, txt, etc.)
    private final String error;            // Error message if profiling failed
    private final boolean success;
    
    // getters, isSuccess(), getError()
}
```

### 2. AsyncProfilerWrapper class
```java
public class AsyncProfilerWrapper {
    private final ServerMonitorConfig config;
    private String profilerPath;           // Resolved path to profiler.sh
    private volatile boolean profilerAvailable;
    
    // Constructor takes ServerMonitorConfig
    
    public boolean init() // Check if profiler exists, auto-download if not
    public boolean isAvailable() // Returns profilerAvailable
    
    public ProfilerResult start(long[] threadIds, String format) 
        // Start profiling specified threads
        // Returns ProfilerResult with outputPath
    
    public ProfilerResult start(String taskId) 
        // Convenience: start profiling all threads for a task
        // Uses TaskThreadTracker to find thread IDs
    
    public void stop() // Stop current profiling
    
    public String getProfileStatus() // "stopped", "profiling", "error"
    
    private boolean downloadProfiler() // Download from GitHub releases
    private String resolveProfilerPath() // Check config path, then common locations
    private String executeProfiler(String[] args) // Execute profiler.sh and capture output
}
```

### 3. Profiler download
- Download from GitHub releases: `https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz`
- Extract to configurable directory (default: `./profiler/`)
- Make `profiler.sh` executable
- Cache the resolved path
- Handle network failures gracefully (log error, set profilerAvailable=false)

### 4. Profiler invocation
Use async-profiler CLI:
```bash
./profiler.sh -d <duration> -f <outputPath> -e cpu -t <threadId1>,<threadId2> <jpid>
```
- `-d`: duration in seconds (from config, default 30)
- `-f`: output file path
- `-e cpu`: CPU profiling mode
- `-t`: thread IDs to profile (comma-separated)
- `<jpid>`: the BioUML JVM PID (obtained from `jps` or runtime)

### 5. Output formats
- `html` (default): HTML flamegraph, most useful for visual analysis
- `collapsed`: collapsed stack format, useful for further processing
- `txt`: flat profile, useful for quick inspection

## Acceptance Criteria
1. AsyncProfilerWrapper class exists with init(), start(), stop(), getProfileStatus() methods
2. ProfilerResult class exists with outputPath, startTime, endTime, success, error fields
3. init() checks for profiler.sh existence and returns true/false
4. downloadProfiler() constructs correct URL for linux-x64 and extracts tarball
5. start() builds correct profiler.sh command with thread IDs and duration
6. start() returns ProfilerResult with success=true and valid outputPath on success
7. start() returns ProfilerResult with success=false and error message on failure
8. stop() sends appropriate signal to running profiler
9. resolveProfilerPath() checks config path, then ./profiler/, then /usr/local/bin/
10. All file I/O wrapped in try-catch with logging
11. No hardcoded paths — all configurable via ServerMonitorConfig

## Evidence Required
- Method signatures for AsyncProfilerWrapper visible in compilation output
- ProfilerResult field list visible
- downloadProfiler() URL construction visible
- executeProfiler() command construction visible

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
1

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
