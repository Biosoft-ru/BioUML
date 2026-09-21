SUPERGOAL_PHASE_START
Phase: 5 of 6 — API Endpoint
Task: Add profiling management and retrieval endpoints to the support API

## Work Description

Extend the SupportServlet to handle profiling-related API calls:

### 1. New dispatch in SupportServlet
Add a new command prefix check in the `service()` method:
```java
// In SupportServlet.service():
if (localAddress.endsWith("profile")) {
    return handleProfile(params);
}
```

### 2. handleProfile() method
```java
protected JSONObject handleProfile(Map params) throws Exception {
    try {
        login(params);
        checkAdmin();
        
        String action = getStringParameter(params, "action");
        if (action == null) action = "status";
        
        switch (action) {
            case "list":    return listProfiles(params);
            case "get":     return getProfile(params);
            case "stop":    return stopProfiling(params);
            case "status":  return getMonitorStatus(params);
            case "config":  return getMonitorConfig(params);
            case "setConfig": return setMonitorConfig(params);
            default:        return errorResponse("Unknown action: " + action);
        }
    } finally {
        SecurityManager.commonLogout();
    }
}
```

### 3. listProfiles() — GET /biouml/support/profile?action=list
Returns JSON array of profile metadata:
```java
protected JSONObject listProfiles(Map params) {
    File profileDir = new File(config.getProfilerDir());
    JSONArray array = new JSONArray();
    
    if (!profileDir.exists()) return arrayOkResponse(array);
    
    File[] files = profileDir.listFiles((dir, name) -> 
        name.endsWith(".html") || name.endsWith(".txt") || name.endsWith(".collapsed"));
    
    if (files == null) return arrayOkResponse(array);
    
    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    
    for (File f : files) {
        JSONObject profile = new JSONObject();
        profile.put("id", f.getName());
        profile.put("path", f.getAbsolutePath());
        profile.put("size", f.length());
        profile.put("timestamp", f.lastModified());
        profile.put("format", getFileExtension(f.getName()));
        
        // Try to parse metadata if available
        File metaFile = new File(f.getAbsolutePath().replace("." + getFileExtension(f.getName()), ".json"));
        if (metaFile.exists()) {
            try {
                String meta = new String(Files.readAllBytes(metaFile.toPath()));
                profile.put("metadata", new JSONObject(meta));
            } catch (Exception e) {}
        }
        
        array.put(profile);
    }
    
    return arrayOkResponse(array);
}
```

### 4. getProfile() — GET /biouml/support/profile?action=get&id=<filename>
Returns the profile file content:
```java
protected JSONObject getProfile(Map params) throws Exception {
    String id = getStringParameter(params, "id");
    if (id == null) return errorResponse("Missing 'id' parameter");
    
    File profileFile = new File(config.getProfilerDir(), id);
    if (!profileFile.exists()) return errorResponse("Profile not found: " + id);
    
    // Return file content with appropriate MIME type
    String mimeType = "text/plain";
    if (id.endsWith(".html")) mimeType = "text/html";
    else if (id.endsWith(".collapsed")) mimeType = "text/plain";
    
    byte[] content = Files.readAllBytes(profileFile.toPath());
    return binaryOkResponse(content, mimeType, id);
}
```

### 5. stopProfiling() — GET /biouml/support/profile?action=stop&id=<taskId>
Stops active profiling for a task:
```java
protected JSONObject stopProfiling(Map params) throws Exception {
    String taskId = getStringParameter(params, "id");
    
    if (taskId != null) {
        // Stop specific task profiling
        MonitoringService monitor = getMonitoringService();
        if (monitor != null && monitor.getActiveProfiles().containsKey(taskId)) {
            // Need to track which threads to stop
            // For now, stop all active profiles
            for (String activeId : monitor.getActiveProfiles().keySet()) {
                // Mark for stopping
            }
        }
    } else {
        // Stop all active profiling
        MonitoringService monitor = getMonitoringService();
        if (monitor != null) {
            // Stop all active profiles
        }
    }
    
    return simpleOkResponse();
}
```

### 6. getMonitorStatus() — GET /biouml/support/profile?action=status
Returns monitoring service status:
```java
protected JSONObject getMonitorStatus(Map params) {
    JSONObject status = new JSONObject();
    MonitoringService monitor = getMonitoringService();
    
    if (monitor == null) {
        status.put("running", false);
        status.put("error", "Monitoring service not initialized");
    } else {
        status.put("running", monitor.isRunning());
        status.put("profilerAvailable", monitor.isProfilerAvailable());
        status.put("slowTaskCount", monitor.getSlowTaskCount());
        status.put("slowTasks", new JSONArray(monitor.getSlowTaskIds()));
        status.put("activeProfiles", monitor.getActiveProfiles().keySet().size());
        status.put("lastCheck", monitor.getLastCheckTime());
    }
    
    return arrayOkResponse(new JSONArray().put(status));
}
```

### 7. getMonitorConfig() / setMonitorConfig()
Get and set monitoring configuration:
```java
protected JSONObject getMonitorConfig(Map params) {
    ServerMonitorConfig cfg = getMonitoringService().getConfig();
    JSONObject config = new JSONObject();
    config.put("slowTaskThreshold", cfg.getSlowTaskThreshold());
    config.put("checkInterval", cfg.getCheckInterval());
    config.put("profilerPath", cfg.getProfilerPath());
    config.put("profilerDir", cfg.getProfilerDir());
    config.put("maxProfiles", cfg.getMaxProfiles());
    config.put("profileDuration", cfg.getProfileDuration());
    config.put("periodicInterval", cfg.getPeriodicInterval());
    config.put("periodicMode", cfg.getPeriodicMode());
    config.put("maxProfileAge", cfg.getMaxProfileAge());
    return arrayOkResponse(new JSONArray().put(config));
}

protected JSONObject setMonitorConfig(Map params) throws Exception {
    String configJson = getStringParameter(params, "config");
    if (configJson == null) return errorResponse("Missing 'config' parameter");
    
    JSONObject cfg = new JSONObject(configJson);
    ServerMonitorConfig config = getMonitoringService().getConfig();
    
    if (cfg.has("slowTaskThreshold")) config.set("slowTaskThreshold", cfg.getInt("slowTaskThreshold"));
    if (cfg.has("checkInterval")) config.set("checkInterval", cfg.getInt("checkInterval"));
    // ... other fields
    
    return simpleOkResponse();
}
```

### 8. Profile metadata JSON
When saving a profile, also save a `.json` sidecar file:
```json
{
    "taskId": "2026.06.19 17:27:44 9",
    "username": "pashak1232@gmail.com",
    "taskType": "scriptDocument",
    "source": "data/Collaboration/.../Model_processing.js",
    "startTime": 1781890064967,
    "endTime": 1781890124967,
    "duration": 60,
    "threadIds": [42, 43],
    "format": "html",
    "profilerVersion": "3.0"
}
```

## Acceptance Criteria
1. SupportServlet.handleProfile() method exists and dispatches to sub-actions
2. `action=list` returns JSON array of profile metadata files
3. `action=get&id=<file>` returns the profile file content with correct MIME type
4. `action=stop` stops active profiling
5. `action=status` returns monitoring service status as JSON object
6. `action=config` returns current configuration
7. `action=setConfig` updates configuration
8. All endpoints require admin authentication (checkAdmin() called)
9. Profile metadata JSON sidecar files are created alongside profiles
10. File listing sorted by timestamp (newest first)
11. Filename sanitization prevents path traversal attacks
12. Compilation succeeds without errors

## Evidence Required
- handleProfile() dispatch method visible
- listProfiles() returning JSONArray of profile metadata
- getProfile() returning file content with MIME type
- getMonitorStatus() returning status JSON
- checkAdmin() called in all profile endpoints
- Filename sanitization visible

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
4

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
