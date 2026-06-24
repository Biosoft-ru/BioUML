package biouml.plugins.servermonitor;

/**
 * Result of an async-profiler profiling session.
 * Contains the output path, timing, and success/failure status.
 * Also carries AI-friendly output (collapsed stacks + flat profile).
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

    // AI-friendly output
    private final String collapsedOutputPath;
    private final String flatProfileText;

    /**
     * Create a successful profiling result.
     * @param outputPath path to the profile output file
     * @param startTime profiling start time (millis)
     * @param endTime profiling end time (millis)
     * @param threadCount number of threads profiled
     * @param threadIds thread IDs as strings
     * @param format output format (html, txt, collapsed)
     */
    public ProfilerResult(String outputPath, long startTime, long endTime,
                          int threadCount, String[] threadIds, String format) {
        this(outputPath, startTime, endTime, threadCount, threadIds, format, null, null);
    }

    /**
     * Create a successful profiling result with AI-friendly output.
     * @param outputPath path to the profile output file
     * @param startTime profiling start time (millis)
     * @param endTime profiling end time (millis)
     * @param threadCount number of threads profiled
     * @param threadIds thread IDs as strings
     * @param format output format (html, txt, collapsed)
     * @param collapsedOutputPath path to the collapsed stacks file (AI-friendly)
     * @param flatProfileText text of the flat profile (AI-friendly)
     */
    public ProfilerResult(String outputPath, long startTime, long endTime,
                          int threadCount, String[] threadIds, String format,
                          String collapsedOutputPath, String flatProfileText) {
        this.outputPath = outputPath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadCount = threadCount;
        this.threadIds = threadIds;
        this.format = format;
        this.error = null;
        this.success = true;
        this.collapsedOutputPath = collapsedOutputPath;
        this.flatProfileText = flatProfileText;
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
        this.collapsedOutputPath = null;
        this.flatProfileText = null;
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
     * Get the path to the collapsed stacks file (AI-friendly format).
     */
    public String getCollapsedOutputPath() {
        return collapsedOutputPath;
    }

    /**
     * Get the flat profile text (AI-friendly format).
     */
    public String getFlatProfileText() {
        return flatProfileText;
    }

    @Override
    public String toString() {
        if (success) {
            return "ProfilerResult{outputPath='" + outputPath + "', duration=" + getDuration() +
                    "ms, threads=" + threadCount + ", format='" + format +
                    (collapsedOutputPath != null ? ", collapsed='" + collapsedOutputPath + "'" : "") +
                    '}';
        } else {
            return "ProfilerResult{error='" + error + "'}";
        }
    }
}
