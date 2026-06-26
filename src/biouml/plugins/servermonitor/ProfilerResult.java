package biouml.plugins.servermonitor;

/**
 * Result of an async-profiler profiling session.
 * Contains the output path, timing, and success/failure status.
 * Also carries extra format output paths for AI agent consumption.
 */
public class ProfilerResult {

    private final String outputPath;
    private final long startTime;
    private final long endTime;
    private final int threadCount;
    private final String[] threadIds;
    private final String format;
    private final String error;
    private final boolean success;

    // Extra format output paths (e.g., collapsed, flat, traces)
    private final String[] extraPaths;

    /**
     * Create a successful profiling result.
     * @param outputPath path to the profile output file
     * @param startTime profiling start time (millis)
     * @param endTime profiling end time (millis)
     * @param threadCount number of threads profiled
     * @param threadIds thread IDs as strings
     * @param format output format (tree, html, txt, collapsed)
     */
    public ProfilerResult(String outputPath, long startTime, long endTime,
                          int threadCount, String[] threadIds, String format) {
        this(outputPath, startTime, endTime, threadCount, threadIds, format, null);
    }

    /**
     * Create a successful profiling result with extra format paths.
     * @param outputPath path to the profile output file
     * @param startTime profiling start time (millis)
     * @param endTime profiling end time (millis)
     * @param threadCount number of threads profiled
     * @param threadIds thread IDs as strings
     * @param format output format (tree, html, txt, collapsed)
     * @param extraPaths additional output file paths (e.g., collapsed, flat)
     */
    public ProfilerResult(String outputPath, long startTime, long endTime,
                          int threadCount, String[] threadIds, String format,
                          String[] extraPaths) {
        this.outputPath = outputPath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadCount = threadCount;
        this.threadIds = threadIds;
        this.format = format;
        this.error = null;
        this.success = true;
        this.extraPaths = extraPaths;
    }

    /**
     * Create a failed profiling result.
     * @param error error message describing the failure
     */
    public ProfilerResult(String error) {
        this.outputPath = null;
        this.startTime = 0;
        this.endTime = 0;
        this.threadCount = 0;
        this.threadIds = new String[0];
        this.format = null;
        this.error = error;
        this.success = false;
        this.extraPaths = null;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getDuration() {
        return endTime - startTime;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String[] getThreadIds() {
        return threadIds;
    }

    public String getFormat() {
        return format;
    }

    public String getError() {
        return error;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Get extra format output paths (e.g., collapsed, flat, traces).
     */
    public String[] getExtraPaths() {
        return extraPaths;
    }

    /**
     * Get the path to the first extra format file, or null if none.
     */
    public String getFirstExtraPath() {
        return (extraPaths != null && extraPaths.length > 0) ? extraPaths[0] : null;
    }

    @Override
    public String toString() {
        if (success) {
            return "ProfilerResult{outputPath='" + outputPath + "', duration=" + getDuration() +
                    "ms, threads=" + threadCount + ", format='" + format +
                    (extraPaths != null && extraPaths.length > 0 ? ", extra=" + extraPaths.length : "") +
                    '}';
        } else {
            return "ProfilerResult{error='" + error + "'}";
        }
    }
}
