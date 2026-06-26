SUPERGOAL_PHASE_START
Phase: 6 of 6 — Polish & Harden
Task: Error handling, documentation, edge cases, cleanup

## Work Description

Final polish pass covering error handling, documentation, edge cases, and code quality:

### 1. Error handling
- All file I/O wrapped in try-catch with proper logging (java.util.logging, not System.out.println)
- Profile directory auto-created if missing (mkdirs with error handling)
- async-profiler download handles network failures (catch IOException, log warning, set profilerAvailable=false)
- Profile output directory permissions checked before writing
- JSON parsing errors caught and logged (malformed metadata files)
- Thread enumeration errors handled (SecurityManager restrictions)

### 2. Edge cases
- Concurrent profiling attempts: max 1 active profile enforced (check in profileTask())
- Empty task list: checkSlowTasks() handles empty runningTasks list gracefully
- Missing profiler binary: init() returns false, monitoring continues without profiling
- Profile file already exists: buildProfilePath() uses unique timestamp suffix
- Very long task names: sanitize for filename (replace non-alphanumeric with underscore)
- Disk full: catch IOException on profile write, log error, mark profile as failed
- Server shutdown during profiling: stop() interrupts profiler and waits for completion

### 3. Old profile cleanup
- Configurable max profile age (default: 7 days = 604800 seconds)
- Configurable max profile count (default: 50)
- Cleanup runs every check interval in cleanupOldProfiles()
- Both age-based AND count-based cleanup
- Deleted files logged

### 4. Documentation
Create `src/biouml/plugins/servermonitor/README.md`:
```markdown
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

### Get profile
```
GET /biouml/support/profile?action=get&id=<filename>
```

### Stop profiling
```
GET /biouml/support/profile?action=stop&id=<taskId>
```

### Monitor status
```
GET /biouml/support/profile?action=status
```

### Get/Set configuration
```
GET /biouml/support/profile?action=config
POST /biouml/support/profile?action=setConfig&config=<json>
```

## Installation

1. Ensure async-profiler is installed:
   ```bash
   mkdir -p profiler
   cd profiler
   wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
   tar xzf async-profiler-3.0-linux-x64.tar.gz
   chmod +x profiler.sh
   ```

2. The plugin will auto-download async-profiler on first run if not found.

## Requirements
- Linux x86_64
- Java 11+ (BioUML uses Java 21)
- async-profiler must run as the same user as the BioUML JVM
- ptrace permissions (usually satisfied when running as same user)

## Profiling Output
- HTML flamegraph: interactive, clickable, zoomable
- Collapsed stacks: for further processing
- Flat profile: top functions by CPU time
```

### 5. Code quality
- No `System.out.println` in production code (grep for it)
- No `TODO` or `FIXME` comments (grep for them)
- All public methods have Javadoc comments
- Consistent logging level usage (INFO for normal, WARNING for recoverable errors, SEVERE for failures)
- No unused imports
- No dead code
- Consistent naming conventions (camelCase, descriptive names)

### 6. Security
- Filename sanitization: replace all non-alphanumeric characters (except `.`, `_`, `-`) with `_`
- Path traversal prevention: use `new File(profilerDir, sanitizedName)` not string concatenation
- Admin authentication required for all API endpoints
- No sensitive data in profile metadata (no passwords, tokens, etc.)

### 7. Performance
- ThreadLocal cleanup: onTaskEnd() calls currentTask.remove() to prevent memory leaks
- ConcurrentHashMap for all shared maps
- Synchronized blocks for critical sections
- Monitor thread is daemon (doesn't prevent JVM shutdown)
- Profile write uses buffered I/O

## Acceptance Criteria
1. All file I/O wrapped in try-catch with logging (grep confirms no bare file operations)
2. Profile directory auto-created with error handling (mkdirs in try-catch)
3. Old profiles auto-cleaned: age-based (default 7 days) AND count-based (default 50)
4. Concurrent profiling limited to 1 active profile (check in profileTask)
5. README.md exists in `src/biouml/plugins/servermonitor/` with setup instructions
6. async-profiler download handles network failures (catch IOException, log warning)
7. No `System.out.println` in any source file (grep confirms)
8. No `TODO` or `FIXME` comments in any source file (grep confirms)
9. No `console.log` or debug prints (grep confirms)
10. All public methods have Javadoc comments
11. Filename sanitization present (replaceAll for non-alphanumeric)
12. Path traversal prevention (File constructor with directory + sanitized name)
13. ThreadLocal cleanup in onTaskEnd() (currentTask.remove())
14. Daemon thread for monitor (setDaemon(true))
15. Compilation succeeds without warnings or errors

## Evidence Required
- grep output: no System.out.println in src/biouml/plugins/servermonitor/
- grep output: no TODO/FIXME in src/biouml/plugins/servermonitor/
- README.md content visible
- Filename sanitization visible (replaceAll pattern)
- ThreadLocal cleanup visible (currentTask.remove())
- setDaemon(true) visible
- Javadoc comments on public methods
- try-catch blocks around file I/O visible

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
1, 2, 3, 4, 5

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
