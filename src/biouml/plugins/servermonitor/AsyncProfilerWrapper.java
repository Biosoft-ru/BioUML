package biouml.plugins.servermonitor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Wrapper for async-profiler integration.
 * Handles profiler detection, auto-download, and invocation.
 */
public class AsyncProfilerWrapper {

    private static final Logger log = Logger.getLogger(AsyncProfilerWrapper.class.getName());

    // async-profiler release URL for Linux x64
    private static final String PROFILER_VERSION = "v3.0";
    private static final String PROFILER_URL = "https://github.com/async-profiler/async-profiler/releases/download/"
            + PROFILER_VERSION + "/async-profiler-3.0-linux-x64.tar.gz";
    // Directory name inside the tarball (matches tarball name minus .tar.gz)
    private static final String PROFILER_DIR_NAME = "async-profiler-3.0-linux-x64";

    private final ServerMonitorConfig config;
    private String profilerPath;
    private volatile boolean profilerAvailable = false;
    private volatile String activeProfileOutputPath = null;

    public AsyncProfilerWrapper(ServerMonitorConfig config) {
        this.config = config;
        this.profilerPath = resolveProfilerPath();
    }

    /**
     * Initialize the profiler: check for existing binary or download it.
     * @return true if profiler is available, false otherwise
     */
    public boolean init() {
        // First try to resolve the configured path
        profilerPath = resolveProfilerPath();
        if (profilerPath != null && new File(profilerPath).exists()) {
            profilerAvailable = true;
            log.info("AsyncProfilerWrapper: profiler found at " + profilerPath);
            return true;
        }

        // Try to download
        try {
            return downloadProfiler();
        } catch (Exception e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: failed to download profiler", e);
            profilerAvailable = false;
            return false;
        }
    }

    /**
     * Check if the profiler is available.
     * @return true if profiler binary is found and executable
     */
    public boolean isAvailable() {
        return profilerAvailable && profilerPath != null && new File(profilerPath).exists();
    }

    /**
     * Start profiling specified threads.
     * Generates both HTML flamegraph (human-readable) and collapsed stacks (AI-agent-friendly).
     * @param threadIds thread IDs to profile
     * @param format primary output format (html, txt, collapsed)
     * @return ProfilerResult with output paths and timing
     */
    public ProfilerResult start(long[] threadIds, String format) {
        return start(threadIds, format, true);
    }

    /**
     * Start profiling specified threads with control over secondary output generation.
     * @param threadIds thread IDs to profile
     * @param format primary output format (html, txt, collapsed)
     * @param generateSecondary whether to also generate collapsed stacks for AI agent use
     * @return ProfilerResult with output paths and timing
     */
    public ProfilerResult start(long[] threadIds, String format, boolean generateSecondary) {
        if (!isAvailable()) {
            return new ProfilerResult("async-profiler is not available");
        }

        // Stop any existing profiling first
        stop();

        // Get JVM PID
        long jvmPid = getJvmPid();
        if (jvmPid <= 0) {
            return new ProfilerResult("Could not determine JVM PID");
        }

        // Build thread ID string
        String threadIdStr = "";
        if (threadIds != null && threadIds.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < threadIds.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(threadIds[i]);
            }
            threadIdStr = sb.toString();
        }

        long startTime = System.currentTimeMillis();
        int duration = config.getProfileDuration();
        String htmlOutputPath = buildOutputPath("html");
        String collapsedOutputPath = buildOutputPath("collapsed");
        String flatProfilePath = buildOutputPath("txt");

        try {
            // Run 1: HTML flamegraph (primary, human-readable)
            String primaryPath = htmlOutputPath;
            if ("collapsed".equals(format)) {
                primaryPath = collapsedOutputPath;
            } else if ("txt".equals(format)) {
                primaryPath = flatProfilePath;
            }

            boolean primaryOk = runProfiler(jvmPid, threadIdStr, duration, primaryPath);

            // Run 2: Generate secondary format for AI agent use
            String secondaryPath = null;
            String flatText = null;
            if (generateSecondary && primaryOk) {
                if (!"collapsed".equals(format)) {
                    // Generate collapsed stacks for AI agent
                    secondaryPath = collapsedOutputPath;
                    runProfiler(jvmPid, threadIdStr, duration, collapsedOutputPath);
                }
                if (!"txt".equals(format)) {
                    // Generate flat profile for AI agent
                    runProfiler(jvmPid, threadIdStr, duration, flatProfilePath);
                    // Read flat profile text
                    try {
                        flatText = readFileContent(new File(flatProfilePath));
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Error reading flat profile", e);
                    }
                }
            }

            long endTime = System.currentTimeMillis();

            if (primaryOk && new File(primaryPath).exists()) {
                activeProfileOutputPath = primaryPath;
                String[] tidStrs = threadIdStr.isEmpty() ? new String[0] : threadIdStr.split(",");
                return new ProfilerResult(primaryPath, startTime, endTime,
                        tidStrs.length, tidStrs, format,
                        secondaryPath, flatText);
            } else {
                return new ProfilerResult("Profiler exited with code " + (primaryOk ? 0 : 1));
            }
        } catch (IOException | InterruptedException e) {
            long endTime = System.currentTimeMillis();
            log.log(Level.SEVERE, "AsyncProfilerWrapper: profiling error", e);
            return new ProfilerResult("Profiling error: " + e.getMessage());
        }
    }

    /**
     * Run a single profiler invocation.
     * @return true if profiler exited successfully
     */
    private boolean runProfiler(long jvmPid, String threadIdStr, int duration, String outputPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(profilerPath);
        command.add("-d");
        command.add(String.valueOf(duration));
        command.add("-f");
        command.add(outputPath);
        command.add("-e");
        command.add("cpu");
        command.add("-j");
        command.add(String.valueOf(jvmPid));

        if (!threadIdStr.isEmpty()) {
            command.add("-t");
            command.add(threadIdStr);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        // Set LD_LIBRARY_PATH to include the profiler's lib directory
        String profilerDir = new File(profilerPath).getParent();
        String libPath = profilerDir + "/lib";
        Map<String, String> env = pb.environment();
        String existingLibPath = env.get("LD_LIBRARY_PATH");
        if (existingLibPath != null) {
            env.put("LD_LIBRARY_PATH", libPath + ":" + existingLibPath);
        } else {
            env.put("LD_LIBRARY_PATH", libPath);
        }
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture stderr for debugging
        StringBuilder stderr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderr.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: profiler exited with code " + exitCode + ". stderr: " + stderr);
        }
        return exitCode == 0;
    }

    /**
     * Start profiling with a specific task ID (convenience method).
     * Uses TaskThreadTracker to find thread IDs for the task.
     * @param taskId the task name
     * @return ProfilerResult
     */
    public ProfilerResult start(String taskId) {
        // Look up thread IDs from TaskThreadTracker
        java.util.Set<Long> threadIds = TaskThreadTracker.getTaskThreads(taskId);
        long[] ids = threadIds.stream().mapToLong(Long::longValue).toArray();
        return start(ids, "html");
    }

    /**
     * Stop the current profiling session.
     */
    public void stop() {
        if (profilerPath == null || !isAvailable()) {
            return;
        }

        long jvmPid = getJvmPid();
        if (jvmPid <= 0) {
            return;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(profilerPath);
            command.add("-p");
            command.add(String.valueOf(jvmPid));
            command.add("stop");

            Process process = executeCommand(command);
            process.waitFor();
            log.info("AsyncProfilerWrapper: profiling stopped");
        } catch (IOException | InterruptedException e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: error stopping profiler", e);
        }

        activeProfileOutputPath = null;
    }

    /**
     * Get the current profiling status.
     * @return "stopped", "profiling", or "error"
     */
    public String getProfileStatus() {
        if (!isAvailable()) {
            return "error";
        }
        return activeProfileOutputPath != null ? "profiling" : "stopped";
    }

    /**
     * Resolve the path to the profiler binary.
     * Checks config path, then common locations.
     * @return resolved path or null
     */
    private String resolveProfilerPath() {
        // Check configured path first
        String configPath = config.getProfilerPath();
        if (configPath != null && !configPath.isEmpty()) {
            File f = new File(configPath);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
            // Also try if it exists but isn't executable (make it so later)
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        // Check common locations
        String[] commonPaths = {
            "./profiling/async-profiler-3.0-linux-x64/bin/asprof",
            "./profiling/async-profiler-3.0-linux-x64/bin/profiler.sh",
            "./profiler/profiler.sh",
            "./profiler/bin/profiler.sh",
            "/usr/local/bin/profiler.sh",
            "/opt/profiler/profiler.sh"
        };

        for (String path : commonPaths) {
            File f = new File(path);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Download async-profiler from GitHub releases.
     * @return true if download and extraction succeeded
     */
    private boolean downloadProfiler() {
        String downloadDir = config.getProfilerDir();
        File dir = new File(downloadDir);

        try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.warning("AsyncProfilerWrapper: failed to create profiler directory: " + downloadDir);
                    return false;
                }
            }

            // Download tarball
            File tarball = new File(dir, "async-profiler-" + PROFILER_VERSION + "-linux-x64.tar.gz");
            log.info("AsyncProfilerWrapper: downloading profiler from " + PROFILER_URL);

            downloadFile(PROFILER_URL, tarball);

            // Extract tarball
            extractTarball(tarball, dir);

            // Clean up tarball
            tarball.delete();

            // Find profiler binary (tarball extracts to async-profiler-3.0-linux-x64/bin/asprof)
            File profilerBin = new File(dir, PROFILER_DIR_NAME + "/bin/asprof");
            if (!profilerBin.exists()) {
                // Try alternate extraction locations
                profilerBin = new File(dir, PROFILER_DIR_NAME + "/bin/profiler.sh");
                if (!profilerBin.exists()) {
                    profilerBin = new File(dir, PROFILER_DIR_NAME + "/profiler.sh");
                    if (!profilerBin.exists()) {
                        profilerBin = new File(dir, "profiler.sh");
                    }
                }
            }

            if (profilerBin.exists()) {
                profilerBin.setExecutable(true);
                profilerPath = profilerBin.getAbsolutePath();
                profilerAvailable = true;
                log.info("AsyncProfilerWrapper: profiler downloaded to " + profilerPath);
                return true;
            } else {
                log.warning("AsyncProfilerWrapper: profiler binary not found after extraction");
                return false;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "AsyncProfilerWrapper: download error", e);
            return false;
        }
    }

    /**
     * Download a file from a URL.
     */
    private void downloadFile(String urlString, File destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("GET");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extract a tar.gz archive using Apache Commons Compress.
     */
    private void extractTarball(File tarball, File destDir) throws IOException {
        log.info("AsyncProfilerWrapper: extracting tarball with Apache Commons Compress");
        try (InputStream fis = Files.newInputStream(tarball.toPath());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            int entryCount = 0;
            while ((entry = tais.getNextTarEntry()) != null) {
                String name = entry.getName();
                // Sanitize: strip leading ./ or /
                while (name.startsWith("./") || name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.isEmpty()) continue;

                File outFile = new File(destDir, name);
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    log.fine("AsyncProfilerWrapper: extracted dir: " + outFile.getPath());
                } else {
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = tais.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                        }
                    }
                    log.fine("AsyncProfilerWrapper: extracted file: " + outFile.getPath() + " (" + entry.getSize() + " bytes)");
                }
                entryCount++;
            }
            log.info("AsyncProfilerWrapper: extracted " + entryCount + " entries");
        }
    }

    /**
     * Build an output file path for the profile.
     */
    private String buildOutputPath(String format) {
        File dir = new File(config.getProfilerDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String safeName = "profile_" + timestamp;
        return dir.getAbsolutePath() + "/" + safeName + "." + format;
    }

    /**
     * Get the current JVM's PID.
     */
    private long getJvmPid() {
        String pidStr = ManagementFactory.getRuntimeMXBean().getName();
        int idx = pidStr.indexOf('@');
        if (idx > 0) {
            try {
                return Long.parseLong(pidStr.substring(0, idx));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Execute a command and return the process.
     */
    private Process executeCommand(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * Read file content as UTF-8 string.
     */
    private String readFileContent(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }
}
