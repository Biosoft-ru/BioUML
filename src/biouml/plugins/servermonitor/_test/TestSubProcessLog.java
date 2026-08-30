package biouml.plugins.servermonitor._test;

import biouml.plugins.servermonitor.MonitoringService;
import biouml.plugins.servermonitor.ServerMonitorConfig;
import biouml.plugins.servermonitor.SubProcessMonitor;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests the persistent sub-process observation log in {@link MonitoringService}:
 * appending records, reading them back (with time-range filtering), counting,
 * and purging old / excess lines.
 *
 * <p>Constructs a real {@link MonitoringService} but never calls {@code start()},
 * so no monitor thread is spawned and no profiler is touched. The private log
 * methods are exercised via reflection.
 */
public class TestSubProcessLog extends junit.framework.TestCase
{
    private MonitoringService service;
    private File tmpDir;

    public TestSubProcessLog(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestSubProcessLog.class.getName());
        suite.addTest(new TestSubProcessLog("testAppendAndRead"));
        suite.addTest(new TestSubProcessLog("testReadSinceUntil"));
        suite.addTest(new TestSubProcessLog("testPurgeByAge"));
        suite.addTest(new TestSubProcessLog("testPurgeByCountOverCap"));
        suite.addTest(new TestSubProcessLog("testExtractTimestamp"));
        suite.addTest(new TestSubProcessLog("testRedactCommand"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tmpDir = createTempDir();
        ServerMonitorConfig config = new ServerMonitorConfig();
        config.set(ServerMonitorConfig.PROFILER_DIR, tmpDir.getAbsolutePath());
        // A fresh service with no monitor thread (start() is never called).
        service = new MonitoringService(config);
    }

    @Override
    protected void tearDown() throws Exception
    {
        deleteRecursively(tmpDir);
        super.tearDown();
    }

    // ---------- helpers ----------

    private static File createTempDir() throws Exception
    {
        File dir = File.createTempFile("subproclog", "");
        dir.delete();
        dir.mkdirs();
        return dir;
    }

    private static void deleteRecursively(File f)
    {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null)
            for (File c : children)
                deleteRecursively(c);
        f.delete();
    }

    /** Build one SubProcess via its package-private constructor. */
    private SubProcessMonitor.SubProcess makeSub(long pid, long age, boolean slow, String cmd) throws Exception
    {
        java.lang.reflect.Constructor<SubProcessMonitor.SubProcess> ctor =
                SubProcessMonitor.SubProcess.class.getDeclaredConstructor(long.class, long.class, String.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(pid, age, cmd, slow);
    }

    /** Call private appendSubProcessLog(List, long). */
    private void append(List<SubProcessMonitor.SubProcess> subs, long ts) throws Exception
    {
        Method m = MonitoringService.class.getDeclaredMethod("appendSubProcessLog", List.class, long.class);
        m.setAccessible(true);
        m.invoke(service, subs, ts);
    }

    /** Call private extractTimestamp(String). */
    private long extractTimestamp(String line) throws Exception
    {
        Method m = MonitoringService.class.getDeclaredMethod("extractTimestamp", String.class);
        m.setAccessible(true);
        return (Long) m.invoke(service, line);
    }

    /** Call private purgeAndCompactSubProcessLog(File, long) (does the atomic rename itself). */
    private void purgeAndCompact(File logFile, long now) throws Exception
    {
        Method m = MonitoringService.class.getDeclaredMethod("purgeAndCompactSubProcessLog", File.class, long.class);
        m.setAccessible(true);
        m.invoke(service, logFile, now);
    }

    /** Call private static redactCommand(String). */
    private String redactCommand(String command) throws Exception
    {
        Method m = MonitoringService.class.getDeclaredMethod("redactCommand", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, command);
    }

    // ---------- tests ----------

    public void testAppendAndRead() throws Exception
    {
        // Use realistic (wall-clock) timestamps so the age-based purge in
        // appendSubProcessLog does not treat them as ancient.
        long base = System.currentTimeMillis();

        java.util.ArrayList<SubProcessMonitor.SubProcess> subs = new java.util.ArrayList<>();
        subs.add(makeSub(111, 340, true, "perl script.pl --opt1"));
        subs.add(makeSub(222, 90, false, "Rscript analysis.R"));

        long t1 = base;
        append(subs, t1);

        // Second scan, one process gone, one still alive.
        java.util.ArrayList<SubProcessMonitor.SubProcess> subs2 = new java.util.ArrayList<>();
        subs2.add(makeSub(222, 150, false, "Rscript analysis.R"));
        long t2 = t1 + 60_000L;
        append(subs2, t2);

        assertEquals("two records", 2, service.getSubProcessLogCount());
        List<String> lines = service.readSubProcessLog(0, 0);
        assertEquals(2, lines.size());

        // First record round-trips pid/command correctly.
        org.json.JSONObject rec1 = new org.json.JSONObject(lines.get(0));
        assertEquals(t1, rec1.getLong("timestamp"));
        assertEquals(2, rec1.getInt("count"));
        org.json.JSONArray arr = rec1.getJSONArray("subProcesses");
        assertEquals(111, arr.getJSONObject(0).getLong("pid"));
        assertEquals(true, arr.getJSONObject(0).getBoolean("slow"));
        assertEquals("perl script.pl --opt1", arr.getJSONObject(0).getString("command"));
    }

    public void testReadSinceUntil() throws Exception
    {
        long base = System.currentTimeMillis();
        java.util.ArrayList<SubProcessMonitor.SubProcess> subs = new java.util.ArrayList<>();
        subs.add(makeSub(1, 300, true, "perl a.pl"));
        append(subs, base);
        append(subs, base + 60_000L);
        append(subs, base + 120_000L);

        // Only the middle record falls in [base+30s, base+90s].
        List<String> mid = service.readSubProcessLog(base + 30_000L, base + 90_000L);
        assertEquals(1, mid.size());
        assertEquals(base + 60_000L, new org.json.JSONObject(mid.get(0)).getLong("timestamp"));

        // Unbounded on one side.
        assertEquals(2, service.readSubProcessLog(base + 60_000L, 0).size());
        assertEquals(2, service.readSubProcessLog(0, base + 60_000L).size());
        assertEquals(0, service.readSubProcessLog(base + 200_000L, 0).size());
    }

    public void testPurgeByAge() throws Exception
    {
        File logFile = new File(tmpDir, MonitoringService.SUB_PROCESS_LOG_FILE);

        // Write 5 lines: 2 old (past 7-day age), 3 recent.
        long now = 5_000_000_000L;
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        String old = "{\"timestamp\":" + (now - weekMs - 1000) + ",\"subProcesses\":[]}";
        String recent = "{\"timestamp\":" + (now - 1000) + ",\"subProcesses\":[]}";
        writeLines(logFile, old, old, recent, recent, recent);

        purgeAndCompact(logFile, now);

        // Atomic replacement done; 3 recent lines remain and the count matches.
        List<String> remaining = service.readSubProcessLog(0, 0);
        assertEquals(3, remaining.size());
        assertEquals(3, service.getSubProcessLogCount());
    }

    public void testPurgeByCountOverCap() throws Exception
    {
        File logFile = new File(tmpDir, MonitoringService.SUB_PROCESS_LOG_FILE);

        // 5002 recent lines (within the 7-day age window, so only the line-cap
        // should trim). The cap keeps the most recent 5000, dropping the 2 oldest.
        int n = 5002;
        long now = 5_000_000_000L;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++)
        {
            sb.append("{\"timestamp\":").append(now - (n - i) * 1000L)
              .append(",\"subProcesses\":[]}").append("\n");
        }
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))
        {
            w.write(sb.toString());
        }

        purgeAndCompact(logFile, now);

        List<String> remaining = service.readSubProcessLog(0, 0);
        assertEquals("cap keeps exactly 5000 lines", 5000, remaining.size());
        assertEquals(5000, service.getSubProcessLogCount());
        // The two oldest records were dropped: the earliest surviving timestamp
        // is the 3rd one written.
        long earliest = new org.json.JSONObject(remaining.get(0)).getLong("timestamp");
        assertEquals(now - (n - 2) * 1000L, earliest);
    }

    public void testExtractTimestamp() throws Exception
    {
        assertEquals(1234567890L, extractTimestamp("{\"timestamp\":1234567890,\"count\":1}"));
        assertEquals(1234567890L, extractTimestamp("  garbage {\"timestamp\":1234567890} trailing"));
        assertEquals(-1, extractTimestamp("{\"count\":1}"));
        assertEquals(-1, extractTimestamp("no timestamp here"));
    }

    public void testRedactCommand() throws Exception
    {
        // Secret-looking arguments are masked, non-secrets kept.
        assertEquals(
                "perl run.pl --host=db1 --password=*** --input=in.csv",
                redactCommand("perl run.pl --host=db1 --password=hunter2 --input=in.csv"));
        assertEquals(
                "python fetch.py --token=*** --retry=3",
                redactCommand("python fetch.py --token=abc123 --retry=3"));
        // No secret -> unchanged (exact string, not re-joined).
        assertEquals(
                "perl run.pl --host=db1 --input=in.csv",
                redactCommand("perl run.pl --host=db1 --input=in.csv"));
        // Null / empty pass through.
        assertNull(redactCommand(null));
        assertEquals("", redactCommand(""));
    }

    /** Write a fixed set of lines to the log file (one per line). */
    private static void writeLines(File logFile, String... lines) throws Exception
    {
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))
        {
            for (String l : lines)
            {
                w.write(l);
                w.write("\n");
            }
        }
    }
}
