SUPERGOAL_PHASE_START
Phase: 1 of 6 — Plugin Skeleton & Configuration
Task: Create the biouml.plugins.servermonitor plugin structure with configuration support

## Work Description

Create the full plugin skeleton for the server monitoring feature. This involves:

### 1. Source directory structure
Create `src/biouml/plugins/servermonitor/` with package `biouml.plugins.servermonitor`:
- `ServerMonitorConfig.java` — Configuration class with all parameters
- `ServerMonitorPlugin.java` — Plugin lifecycle class (init/stop)

### 2. OSGi/plugin configuration
Create `plugconfig/biouml.plugins.servermonitor/` with:
- `META-INF/MANIFEST.MF` — Bundle-SymbolicName: `biouml.plugins.servermonitor`, Require-Bundle for core modules (ru.biosoft.server, ru.biosoft.tasks, ru.biosoft.access, biouml.model)
- `plugin.xml` — Extension declaration for the monitor service
- `pom.xml` — Maven POM extending the parent, packaging as jar

### 3. Configuration class (ServerMonitorConfig)
Fields with defaults:
```java
public class ServerMonitorConfig {
    // Task monitoring
    public static final String SLOW_TASK_THRESHOLD = "slowTaskThreshold"; // default: 3600 (seconds)
    public static final String CHECK_INTERVAL = "checkInterval"; // default: 60 (seconds)
    
    // Profiler
    public static final String PROFILER_PATH = "profilerPath"; // default: "./profiler/profiler.sh"
    public static final String PROFILER_DIR = "profilerDir"; // default: "./profiling"
    public static final String MAX_PROFILES = "maxProfiles"; // default: 50
    public static final String PROFILE_DURATION = "profileDuration"; // default: 30 (seconds)
    
    // Periodic profiling
    public static final String PERIODIC_INTERVAL = "periodicInterval"; // default: 1800 (seconds), 0=disabled
    public static final String PERIODIC_MODE = "periodicMode"; // default: "random" (random/sample/all)
    
    // Cleanup
    public static final String MAX_PROFILE_AGE = "maxProfileAge"; // default: 604800 (7 days, seconds)
    
    // Logging
    public static final String LOG_LEVEL = "logLevel"; // default: "INFO"
    
    // Methods
    public static ServerMonitorConfig load(Preferences prefs) // from BioUML preferences
    public static ServerMonitorConfig loadFromFile(String path) // from XML file
    public ServerMonitorConfig get(String key) // getter with default
    public void set(String key, Object value) // setter
}
```

### 4. Plugin lifecycle class (ServerMonitorPlugin)
```java
public class ServerMonitorPlugin {
    public void init(Properties properties) // called at server startup
    public void stop() // called at server shutdown
}
```

### 5. Maven POM
Follow the pattern of existing plugins (e.g., `plugconfig/biouml.plugins.simulation/pom.xml`):
- Parent: `org.biouml:biouml-parent` (or root pom)
- Artifact: `biouml.plugins.servermonitor`
- Dependencies: `org.biouml:src` (provided scope for classes)
- Packaging: `jar`

### 6. MANIFEST.MF
```
Bundle-SymbolicName: biouml.plugins.servermonitor
Bundle-Version: 1.0.0
Require-Bundle: org.biouml.src;visibility:=reexport
Export-Package: biouml.plugins.servermonitor
```

### 7. plugin.xml
```xml
<plugin>
    <extension point="biouml.plugins.servermonitor.service"
               id="serverMonitor"
               class="biouml.plugins.servermonitor.ServerMonitorPlugin"/>
</plugin>
```

## Acceptance Criteria
1. Directory `src/biouml/plugins/servermonitor/` exists with ServerMonitorConfig.java and ServerMonitorPlugin.java
2. Directory `plugconfig/biouml.plugins.servermonitor/` exists with META-INF/MANIFEST.MF, plugin.xml, pom.xml
3. MANIFEST.MF has Bundle-SymbolicName = `biouml.plugins.servermonitor` and correct Require-Bundle
4. plugin.xml is valid XML with a valid extension declaration
5. ServerMonitorConfig has all configurable parameters with correct defaults
6. ServerMonitorConfig.load() returns a valid config with defaults when no preferences provided
7. Maven compiles the plugin source without errors (exit code 0)
8. No TODO or FIXME comments in the code
9. Uses java.util.logging, not System.out.println
10. pom.xml follows existing plugin patterns (artifactId, dependencies, packaging)

## Evidence Required
- Compilation output: "BUILD SUCCESS"
- List of created files with paths
- ServerMonitorConfig field list showing all parameters
- MANIFEST.MF content showing Bundle-SymbolicName and Require-Bundle

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
none

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
