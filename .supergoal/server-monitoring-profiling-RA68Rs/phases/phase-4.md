SUPERGOAL_PHASE_START
Phase: 4 of 6 — Monitoring Service
Task: Implement the core monitoring loop with slow task detection and profiler triggering

## Work Description

Build the main monitoring service that runs as a background daemon thread:

### 1. MonitoringService class
```java
public class MonitoringService {
    private final ServerMonitorConfig config;
    private final AsyncProfilerWrapper profiler;
    private volatile boolean running = false;
    private volatile Thread monitorThread = null;
    private volatile long lastCheckTime = 0;
    private volatile long lastPeriodicTime = 0;
    private volatile int slowTaskCount = 0;
    private volatile List<String> slowTaskIds = new ArrayList<>();
    
    // Profile storage
    private final Map<String, ProfilerResult> activeProfiles = new ConcurrentHashMap<>();
    
    public MonitoringService(ServerMonitorConfig config) {
        this.config = config;
        this.profiler = new AsyncProfilerWrapper(config);
    }
    
    public void start() {
        if (running) return;
        running = true;
        profiler.init();
        monitorThread = new Thread(this::monitorLoop, "ServerMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        log.info("Monitoring service started");
    }
    
    public void stop() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
            try { monitorThread.join(5000); } catch (InterruptedException e) {}
        }
        // Stop any active profiling
        for (String taskId : activeProfiles.keySet()) {
            profiler.stop();
        }
        activeProfiles.clear();
        log.info("Monitoring service stopped");
    }
    
    private void monitorLoop() {
        while (running) {
            try {
                checkSlowTasks();
                checkPeriodicProfiling();
                cleanupOldProfiles();
                
                long interval = config.getCheckInterval() * 1000L;
                Thread.sleep(Math.max(interval, 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Monitoring service error", e);
            }
        }
    }
    
    private void checkSlowTasks() {
        long thresholdMillis = config.getSlowTaskThreshold() * 1000L;
        long now = System.currentTimeMillis();
        
        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();
        List<String> currentSlow = new ArrayList<>();
        
        for (TaskInfo ti : runningTasks) {
            long elapsed = now - ti.getStartTime();
            if (elapsed > thresholdMillis) {
                currentSlow.add(ti.getName());
                
                // Check if we're already profiling this task
                if (!activeProfiles.containsKey(ti.getName())) {
                    profileTask(ti);
                }
            }
        }
        
        slowTaskCount = currentSlow.size();
        slowTaskIds = currentSlow;
    }
    
    private void profileTask(TaskInfo taskInfo) {
        if (activeProfiles.size() >= 1) { // Max 1 concurrent profile
            log.warning("Max concurrent profiles reached, skipping: " + taskInfo.getName());
            return;
        }
        
        // Get thread IDs for this task
        Set<Long> threadIds = TaskThreadTracker.getTaskThreads(taskInfo.getName());
        if (threadIds.isEmpty()) {
            // Fallback: try name matching
            TaskThreadTracker.refreshMapping(
                TaskManager.getInstance().getAllRunningTasks());
            threadIds = TaskThreadTracker.getTaskThreads(taskInfo.getName());
        }
        
        long[] ids = threadIds.stream().mapToLong(Long::longValue).toArray();
        
        if (ids.length > 0) {
            String outputPath = buildProfilePath(taskInfo.getName(), "html");
            ProfilerResult result = profiler.start(ids, "html");
            if (result.isSuccess()) {
                activeProfiles.put(taskInfo.getName(), result);
                log.info("Started profiling task: " + taskInfo.getName() + 
                         " threads: " + ids.length);
            }
        } else {
            log.warning("No threads found for task: " + taskInfo.getName());
        }
    }
    
    private void checkPeriodicProfiling() {
        long periodicInterval = config.getPeriodicInterval() * 1000L;
        long now = System.currentTimeMillis();
        
        if (periodicInterval <= 0) return; // Disabled
        if (now - lastPeriodicTime < periodicInterval) return;
        
        lastPeriodicTime = now;
        
        // Select a task to profile periodically
        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();
        if (runningTasks.isEmpty()) return;
        
        TaskInfo target;
        String mode = config.getPeriodicMode();
        if ("random".equals(mode)) {
            target = runningTasks.get(new Random().nextInt(runningTasks.size()));
        } else if ("sample".equals(mode)) {
            // Profile the longest-running non-slow task
            target = runningTasks.stream()
                .max(Comparator.comparingLong(ti -> now - ti.getStartTime()))
                .orElse(null);
        } else {
            // "all" — profile all running tasks
            for (TaskInfo ti : runningTasks) {
                if (!activeProfiles.containsKey(ti.getName())) {
                    profileTask(ti);
                }
            }
            return;
        }
        
        if (target != null && !activeProfiles.containsKey(target.getName())) {
            profileTask(target);
        }
    }
    
    private void cleanupOldProfiles() {
        long maxAgeMillis = config.getMaxProfileAge() * 1000L;
        long now = System.currentTimeMillis();
        File profileDir = new File(config.getProfilerDir());
        
        if (!profileDir.exists()) return;
        
        File[] files = profileDir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            if (now - f.lastModified() > maxAgeMillis) {
                f.delete();
                log.info("Cleaned old profile: " + f.getName());
            }
        }
        
        // Also enforce maxProfiles count
        File[] allFiles = profileDir.listFiles();
        if (allFiles != null && allFiles.length > config.getMaxProfiles()) {
            Arrays.sort(allFiles, Comparator.comparingLong(File::lastModified));
            int toDelete = allFiles.length - config.getMaxProfiles();
            for (int i = 0; i < toDelete; i++) {
                allFiles[i].delete();
            }
        }
    }
    
    private String buildProfilePath(String taskId, String format) {
        File dir = new File(config.getProfilerDir());
        if (!dir.exists()) dir.mkdirs();
        
        // Sanitize task ID for filename
        String safeName = taskId.replaceAll("[^a-zA-Z0-9._-]", "_");
        long timestamp = System.currentTimeMillis();
        return dir.getAbsolutePath() + "/" + safeName + "_" + timestamp + "." + format;
    }
    
    // Status getters for API
    public boolean isRunning() { return running; }
    public int getSlowTaskCount() { return slowTaskCount; }
    public List<String> getSlowTaskIds() { return new ArrayList<>(slowTaskIds); }
    public Map<String, ProfilerResult> getActiveProfiles() { return new ConcurrentHashMap<>(activeProfiles); }
    public boolean isProfilerAvailable() { return profiler.isAvailable(); }
}
```

### 2. Integration with TaskManager.init()
In TaskManager.init(), after currentTasks is populated, start the monitoring service:
```java
// At end of TaskManager.init():
try {
    ServerMonitorConfig cfg = ServerMonitorConfig.load(Application.getPreferences());
    MonitoringService monitor = new MonitoringService(cfg);
    monitor.start();
    // Store reference for shutdown
    Application.getPreferences().getPreferencesValue("serverMonitor")
        .add(new DynamicProperty("monitor", monitor));
} catch (Exception e) {
    log.log(Level.WARNING, "Failed to start monitoring service", e);
}
```

### 3. Integration with server shutdown
In EmptyServerRunner or a ShutdownHook:
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    // Stop monitoring service
    try {
        Object prefs = Application.getPreferences();
        // Find and stop MonitoringService
    } catch (Exception e) {}
}));
```

## Acceptance Criteria
1. MonitoringService class exists with start(), stop(), monitorLoop() methods
2. start() creates a daemon thread and calls monitorLoop()
3. stop() interrupts the thread and joins with timeout
4. checkSlowTasks() correctly identifies tasks exceeding the threshold
5. profileTask() calls AsyncProfilerWrapper.start() with correct thread IDs
6. checkPeriodicProfiling() selects tasks based on mode (random/sample/all)
7. cleanupOldProfiles() removes files older than maxProfileAge
8. cleanupOldProfiles() enforces maxProfiles count limit
9. buildProfilePath() creates sanitized filenames with timestamps
10. MonitoringService starts automatically when TaskManager.init() is called
11. MonitoringService stops on server shutdown (shutdown hook)
12. Concurrent profiling limited to 1 (maxProfiles check)
13. All exceptions caught and logged, never propagate to monitor thread
14. Compilation succeeds without errors

## Evidence Required
- MonitoringService class with all methods visible
- monitorLoop() showing the check/sleep cycle
- checkSlowTasks() showing threshold comparison
- profileTask() showing thread ID lookup and profiler invocation
- cleanupOldProfiles() showing age-based and count-based cleanup
- TaskManager.init() integration visible

## Mandatory commands
mvn compile -pl plugconfig/biouml.plugins.servermonitor -am -DskipTests

## Depends on phases
2, 3

[Agent will print SUPERGOAL_PHASE_VERIFY and SUPERGOAL_PHASE_DONE here during execution]
