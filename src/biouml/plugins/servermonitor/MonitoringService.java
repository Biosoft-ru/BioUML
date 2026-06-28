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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

/**
 * Core monitoring service that runs as a background daemon thread.
 * Periodically checks for slow tasks and triggers JVM-wide profiling.
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
        for (String profileId : activeProfiles.keySet()) {
            try {
                profiler.stop();
            } catch (Exception e) {
                log.log(Level.WARNING, "Error stopping profiler for profile: " + profileId, e);
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
     * Triggers JVM-wide profiling when a slow task is detected.
     */
    private void checkSlowTasks() {
        long thresholdMillis = config.getSlowTaskThreshold() * 1000L;
        long now = System.currentTimeMillis();

        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();
        if (runningTasks.isEmpty()) return;

        List<String> currentSlow = new ArrayList<>();

        for (TaskInfo ti : runningTasks) {
            long elapsed = now - ti.getStartTime();
            if (elapsed > thresholdMillis) {
                currentSlow.add(ti.getName());

                // Trigger JVM-wide profiling if not already profiling
                if (!activeProfiles.containsKey("jvm")) {
                    profileJvm(ti.getName());
                }
            }
        }

        slowTaskCount = currentSlow.size();
        slowTaskIds = currentSlow;
    }

    /**
     * Force immediate profiling of the entire JVM.
     * @param taskId the task that triggered profiling, or null
     * @return ProfilerResult
     */
    public ProfilerResult profileNow(String taskId) {
        return profileJvm(taskId);
    }

    /**
     * Force immediate profiling of all running tasks (profiles entire JVM).
     * @return list with a single ProfilerResult
     */
    public List<ProfilerResult> profileNowAll() {
        ProfilerResult result = profileJvm(null);
        List<ProfilerResult> results = new ArrayList<>();
        if (result != null) {
            results.add(result);
        }
        return results;
    }

    /**
     * Check for periodic profiling of the JVM.
     */
    private void checkPeriodicProfiling() {
        long periodicInterval = config.getPeriodicInterval() * 1000L;
        long now = System.currentTimeMillis();

        if (periodicInterval <= 0) return; // Disabled
        if (now - lastPeriodicTime < periodicInterval) return;

        lastPeriodicTime = now;

        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();

        if (runningTasks.isEmpty()) {
            // No running tasks - profile the entire JVM to capture idle CPU usage
            log.fine("No running tasks, profiling JVM");
            profileJvm(null);
            return;
        }

        // Profile the JVM if not already profiling
        if (!activeProfiles.containsKey("jvm")) {
            // Pick a representative task for metadata
            TaskInfo target;
            if ("random".equals(config.getPeriodicMode())) {
                target = runningTasks.get(random.nextInt(runningTasks.size()));
            } else {
                // Default: profile the longest-running task
                long nowMs = System.currentTimeMillis();
                long maxElapsed = 0;
                target = null;
                for (TaskInfo ti : runningTasks) {
                    long elapsed = nowMs - ti.getStartTime();
                    if (elapsed > maxElapsed) {
                        maxElapsed = elapsed;
                        target = ti;
                    }
                }
            }
            profileJvm(target != null ? target.getName() : null);
        }
    }

    /**
     * Profile the entire JVM process (all threads).
     * @param triggeredTask the task name that triggered profiling, or null
     * @return ProfilerResult
     */
    private ProfilerResult profileJvm(String triggeredTask) {
        // Check max concurrent profiles
        if (activeProfiles.size() >= 1) {
            log.warning("Max concurrent profiles reached, skipping JVM profiling");
            return new ProfilerResult("Max concurrent profiles reached");
        }

        ProfilerResult result = profiler.start(new long[0], "collapsed");

        if (result.isSuccess()) {
            activeProfiles.put("jvm", result);
            log.info("Started profiling JVM (all threads): " + result.getOutputPath());

            // Save metadata JSON
            saveProfileMetadata(triggeredTask, result);

            // Profiler.start() runs synchronously and blocks until the duration expires.
            // Once it returns the profiler process has exited, so remove "jvm" from
            // activeProfiles to allow the next periodic check to start a new profile.
            activeProfiles.remove("jvm");
        } else {
            log.warning("JVM profiling failed: " + result.getError());
        }

        return result;
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
        String safeName = taskId != null ? taskId.replaceAll("[^a-zA-Z0-9._-]", "_") : "jvm";
        long timestamp = System.currentTimeMillis();
        return dir.getAbsolutePath() + "/" + safeName + "_" + timestamp + "." + format;
    }

    /**
     * Save profile metadata as a JSON sidecar file.
     */
    private void saveProfileMetadata(String triggeredTask, ProfilerResult result) {
        File metaFile = new File(result.getOutputPath().replace("." + getExtension(result.getOutputPath()), ".json"));

        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write("{\n");
            writer.write("  \"triggeredTask\": \"" + escapeJson(triggeredTask) + "\",\n");
            writer.write("  \"startTime\": " + result.getStartTime() + ",\n");
            writer.write("  \"endTime\": " + result.getEndTime() + ",\n");
            writer.write("  \"duration\": " + result.getDuration() + "\n");
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
