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
        suite.addTest(new TestSubProcessLog("testPurgeByAgeAndCount"));
        suite.addTest(new TestSubProcessLog("testExtractTimestamp"));
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

    /** Call private purgeSubProcessLog(File, File, long). */
    private boolean purge(File logFile, File tmp, long now) throws Exception
    {
        Method m = MonitoringService.class.getDeclaredMethod("purgeSubProcessLog", File.class, File.class, long.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(service, logFile, tmp, now);
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

    public void testPurgeByAgeAndCount() throws Exception
    {
        File logFile = new File(tmpDir, MonitoringService.SUB_PROCESS_LOG_FILE);
        File tmp = new File(tmpDir, "tmp.jsonl");

        // Write 5 lines: 2 old (past 7-day age), 3 recent.
        long now = 5_000_000_000L;
        long weekMs = 7L * 24 * 60 * 60 * 1000;
        String old = "{\"timestamp\":" + (now - weekMs - 1000) + ",\"subProcesses\":[]}";
        String recent = "{\"timestamp\":" + (now - 1000) + ",\"subProcesses\":[]}";
        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8))
        {
            for (int i = 0; i < 5; i++)
            {
                w.write(i < 2 ? old : recent);
                w.write("\n");
            }
        }

        boolean changed = purge(logFile, tmp, now);
        assertTrue("purge should drop the 2 old lines", changed);

        // After rename, 3 recent lines remain.
        assertTrue(logFile.delete());
        assertTrue(tmp.renameTo(logFile));
        List<String> remaining = service.readSubProcessLog(0, 0);
        assertEquals(3, remaining.size());
    }

    public void testExtractTimestamp() throws Exception
    {
        assertEquals(1234567890L, extractTimestamp("{\"timestamp\":1234567890,\"count\":1}"));
        assertEquals(1234567890L, extractTimestamp("  garbage {\"timestamp\":1234567890} trailing"));
        assertEquals(-1, extractTimestamp("{\"count\":1}"));
        assertEquals(-1, extractTimestamp("no timestamp here"));
    }
}
