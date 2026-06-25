package biouml.plugins.servermonitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

/**
 * Core monitoring service that runs as a background daemon thread.
 * Periodically checks for slow tasks and triggers async-profiler.
 */
public class MonitoringService {

    private static final Logger log = Logger.getLogger(MonitoringService.class.getName());

    private final ServerMonitorConfig config;
    private final AsyncProfilerWrapper profiler;
    private volatile boolean running = false;
    private volatile Thread monitorThread = null;
    private volatile long lastCheckTime = 0;
    private volatile long lastPeriodicTime = 0;
    private volatile int slowTaskCount = 0;
    private volatile List<String> slowTaskIds = new ArrayList<>();
    private final Map<String, ProfilerResult> activeProfiles = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public MonitoringService(ServerMonitorConfig config) {
        this.config = config;
        this.profiler = new AsyncProfilerWrapper(config);
    }

    /**
     * Start the monitoring service.
     * Creates a daemon thread and begins the monitoring loop.
     */
    public void start() {
        if (running) return;
        running = true;

        // Initialize profiler
        profiler.init();

        monitorThread = new Thread(this::monitorLoop, "ServerMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        log.info("Monitoring service started (threshold=" + config.getSlowTaskThreshold() +
                "s, interval=" + config.getCheckInterval() + "s)");
    }

    /**
     * Stop the monitoring service.
     * Interrupts the monitor thread and stops any active profiling.
     */
    public void stop() {
        running = false;

        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Stop any active profiling
        for (String taskId : activeProfiles.keySet()) {
            try {
                profiler.stop();
            } catch (Exception e) {
                log.log(Level.WARNING, "Error stopping profiler for task: " + taskId, e);
            }
        }
        activeProfiles.clear();

        log.info("Monitoring service stopped");
    }

    /**
     * Main monitoring loop.
     * Runs periodically, checking for slow tasks and triggering profiling.
     */
    private void monitorLoop() {
        while (running) {
            try {
                checkSlowTasks();
                checkPeriodicProfiling();
                cleanupOldProfiles();

                lastCheckTime = System.currentTimeMillis();

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

    /**
     * Check for tasks exceeding the slow threshold.
     * Falls back to TaskThreadTracker if admin access is not available.
     */
    private void checkSlowTasks() {
        long thresholdMillis = config.getSlowTaskThreshold() * 1000L;
        long now = System.currentTimeMillis();

        List<TaskInfo> runningTasks;
        boolean hasAdminAccess = false;
        try {
            runningTasks = TaskManager.getInstance().getAllRunningTasks();
            hasAdminAccess = true;
        } catch (SecurityException e) {
            log.info("Monitoring service: admin access not available, using TaskThreadTracker");
            runningTasks = TaskThreadTracker.getAllTrackedTasks();
        }

        if (runningTasks.isEmpty()) return;

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

        // Refresh thread mapping
        if (hasAdminAccess) {
            TaskThreadTracker.refreshMapping(runningTasks);
        }
    }

    /**
     * Profile a specific task using async-profiler.
     */
    private void profileTask(TaskInfo taskInfo) {
        // Check max concurrent profiles
        if (activeProfiles.size() >= 1) {
            log.warning("Max concurrent profiles reached, skipping: " + taskInfo.getName());
            return;
        }

        // Get thread IDs for this task
        Set<Long> threadIds = TaskThreadTracker.getTaskThreads(taskInfo.getName());
        if (threadIds.isEmpty()) {
            // Fallback: try name matching
            List<TaskInfo> runningTasks;
            try {
                runningTasks = TaskManager.getInstance().getAllRunningTasks();
            } catch (SecurityException e) {
                runningTasks = TaskThreadTracker.getAllTrackedTasks();
            }
            TaskThreadTracker.refreshMapping(runningTasks);
            threadIds = TaskThreadTracker.getTaskThreads(taskInfo.getName());
        }

        long[] ids = threadIds.stream().mapToLong(Long::longValue).toArray();

        if (ids.length > 0) {
            String outputPath = buildProfilePath(taskInfo.getName(), "html");
            ProfilerResult result = profiler.start(ids, "html");

            if (result.isSuccess()) {
                activeProfiles.put(taskInfo.getName(), result);
                log.info("Started profiling task: " + taskInfo.getName() +
                        " threads: " + ids.length + " output: " + result.getOutputPath());

                // Save metadata JSON
                saveProfileMetadata(taskInfo, result, ids);
            } else {
                log.warning("Profiling failed for task " + taskInfo.getName() + ": " + result.getError());
            }
        } else {
            log.warning("No threads found for task: " + taskInfo.getName());
        }
    }

    /**
     * Check for periodic profiling of random/sample tasks.
     * Falls back to TaskThreadTracker if admin access is not available.
     */
    private void checkPeriodicProfiling() {
        long periodicInterval = config.getPeriodicInterval() * 1000L;
        long now = System.currentTimeMillis();

        if (periodicInterval <= 0) return; // Disabled
        if (now - lastPeriodicTime < periodicInterval) return;

        lastPeriodicTime = now;

        List<TaskInfo> runningTasks;
        try {
            runningTasks = TaskManager.getInstance().getAllRunningTasks();
        } catch (SecurityException e) {
            runningTasks = TaskThreadTracker.getAllTrackedTasks();
        }

        if (runningTasks.isEmpty()) return;

        String mode = config.getPeriodicMode();
        if ("random".equals(mode)) {
            TaskInfo target = runningTasks.get(random.nextInt(runningTasks.size()));
            if (target != null && !activeProfiles.containsKey(target.getName())) {
                profileTask(target);
            }
        } else if ("sample".equals(mode)) {
            // Profile the longest-running non-slow task
            long nowMs = System.currentTimeMillis();
            TaskInfo target = null;
            long maxElapsed = 0;
            for (TaskInfo ti : runningTasks) {
                long elapsed = nowMs - ti.getStartTime();
                if (elapsed > maxElapsed && !activeProfiles.containsKey(ti.getName())) {
                    maxElapsed = elapsed;
                    target = ti;
                }
            }
            if (target != null) {
                profileTask(target);
            }
        } else if ("all".equals(mode)) {
            for (TaskInfo ti : runningTasks) {
                if (!activeProfiles.containsKey(ti.getName())) {
                    profileTask(ti);
                }
            }
        }
    }

    /**
     * Clean up old profiles based on age and count limits.
     */
    private void cleanupOldProfiles() {
        long maxAgeMillis = config.getMaxProfileAge() * 1000L;
        long now = System.currentTimeMillis();
        File profileDir = new File(config.getProfilerDir());

        if (!profileDir.exists()) return;

        File[] files = profileDir.listFiles();
        if (files == null) return;

        // Age-based cleanup
        for (File f : files) {
            if (now - f.lastModified() > maxAgeMillis) {
                try {
                    f.delete();
                    log.info("Cleaned old profile: " + f.getName());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error deleting old profile: " + f.getName(), e);
                }
            }
        }

        // Count-based cleanup
        files = profileDir.listFiles();
        if (files != null && files.length > config.getMaxProfiles()) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            int toDelete = files.length - config.getMaxProfiles();
            for (int i = 0; i < toDelete; i++) {
                try {
                    files[i].delete();
                    log.info("Cleaned excess profile: " + files[i].getName());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error deleting excess profile: " + files[i].getName(), e);
                }
            }
        }
    }

    /**
     * Build a sanitized output path for a profile.
     */
    private String buildProfilePath(String taskId, String format) {
        File dir = new File(config.getProfilerDir());
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                log.log(Level.WARNING, "Error creating profile directory: " + dir.getAbsolutePath(), e);
            }
        }

        // Sanitize task ID for filename
        String safeName = taskId.replaceAll("[^a-zA-Z0-9._-]", "_");
        long timestamp = System.currentTimeMillis();
        return dir.getAbsolutePath() + "/" + safeName + "_" + timestamp + "." + format;
    }

    /**
     * Save profile metadata as a JSON sidecar file.
     */
    private void saveProfileMetadata(TaskInfo taskInfo, ProfilerResult result, long[] threadIds) {
        File metaFile = new File(result.getOutputPath().replace("." + getExtension(result.getOutputPath()), ".json"));

        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write("{\n");
            writer.write("  \"taskId\": \"" + escapeJson(taskInfo.getName()) + "\",\n");
            writer.write("  \"username\": \"" + escapeJson(taskInfo.getUser()) + "\",\n");
            writer.write("  \"taskType\": \"" + escapeJson(taskInfo.getType()) + "\",\n");
            writer.write("  \"source\": \"" + escapeJson(taskInfo.getSource() == null ? "" : taskInfo.getSource().toString()) + "\",\n");
            writer.write("  \"startTime\": " + taskInfo.getStartTime() + ",\n");
            writer.write("  \"endTime\": " + result.getEndTime() + ",\n");
            writer.write("  \"duration\": " + result.getDuration() + ",\n");
            writer.write("  \"threadIds\": [" + joinLongs(threadIds) + "],\n");
            writer.write("  \"format\": \"" + result.getFormat() + "\"\n");
            writer.write("}");
            log.fine("Saved profile metadata: " + metaFile.getAbsolutePath());
        } catch (IOException e) {
            log.log(Level.WARNING, "Error saving profile metadata", e);
        }
    }

    private String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot > 0 ? path.substring(dot + 1) : "";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String joinLongs(long[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(ids[i]);
        }
        return sb.toString();
    }

    // --- Status getters for API ---

    public boolean isRunning() {
        return running;
    }

    public int getSlowTaskCount() {
        return slowTaskCount;
    }

    public List<String> getSlowTaskIds() {
        return new ArrayList<>(slowTaskIds);
    }

    public Map<String, ProfilerResult> getActiveProfiles() {
        return new ConcurrentHashMap<>(activeProfiles);
    }

    public boolean isProfilerAvailable() {
        return profiler.isAvailable();
    }

    public ServerMonitorConfig getConfig() {
        return config;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }
}
