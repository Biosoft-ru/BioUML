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
        suite.addTest(new TestSubProcessLog("testPurgeDropsMalformedLines"));
        suite.addTest(new TestSubProcessLog("testAppendSeedsCountCorrectly"));
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

    public void testPurgeDropsMalformedLines() throws Exception
    {
        File logFile = new File(tmpDir, MonitoringService.SUB_PROCESS_LOG_FILE);

        // Mix valid and malformed lines (no parseable timestamp). The malformed
        // lines must be dropped on compact, not retained forever.
        long now = 5_000_000_000L;
        String valid = "{\"timestamp\":" + (now - 1000) + ",\"subProcesses\":[]}";
        String noTs = "{\"count\":1}";
        String garbage = "this is not even json";
        String blank = "";
        writeLines(logFile, valid, noTs, valid, garbage, blank, valid);

        purgeAndCompact(logFile, now);

        List<String> remaining = service.readSubProcessLog(0, 0);
        assertEquals("only the 3 valid lines survive", 3, remaining.size());
        assertEquals(3, service.getSubProcessLogCount());
    }

    /**
     * Regression test: when the service starts with an existing log file and
     * {@code subProcessLogCount == -1}, the first append must seed the count
     * to the pre-append line count, then increment — NOT count the post-append
     * file and then increment (which would give N+1).
     */
    public void testAppendSeedsCountCorrectly() throws Exception
    {
        File logFile = new File(tmpDir, MonitoringService.SUB_PROCESS_LOG_FILE);

        // Pre-existing file with 3 lines.
        long now = System.currentTimeMillis();
        writeLines(logFile,
                "{\"timestamp\":" + (now - 3000) + ",\"subProcesses\":[]}",
                "{\"timestamp\":" + (now - 2000) + ",\"subProcesses\":[]}",
                "{\"timestamp\":" + (now - 1000) + ",\"subProcesses\":[]}");

        // Append one more record. subProcessLogCount is -1 (never seeded).
        java.util.ArrayList<SubProcessMonitor.SubProcess> subs = new java.util.ArrayList<>();
        subs.add(makeSub(42, 500, true, "perl test.pl"));
        append(subs, now);

        // The count must be exactly 4 (3 pre-existing + 1 appended), not 5.
        assertEquals("count must be 4, not 5 (off-by-one)", 4, service.getSubProcessLogCount());
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
        // --- key=value form ---
        assertEquals(
                "perl run.pl --host=db1 --password=*** --input=in.csv",
                redactCommand("perl run.pl --host=db1 --password=hunter2 --input=in.csv"));
        assertEquals(
                "python fetch.py --token=*** --retry=3",
                redactCommand("python fetch.py --token=abc123 --retry=3"));

        // --- separate-token form (--secret value) ---
        assertEquals(
                "perl run.pl --host=db1 --password *** --input=in.csv",
                redactCommand("perl run.pl --host=db1 --password hunter2 --input=in.csv"));
        assertEquals(
                "python fetch.py --token *** --retry=3",
                redactCommand("python fetch.py --token abc123 --retry=3"));

        // --- quoted forms (tokenizer strips quotes, so re-joined output has none) ---
        assertEquals(
                "perl run.pl --password=*** --input=in.csv",
                redactCommand("perl run.pl --password=\"hunter2\" --input=in.csv"));
        assertEquals(
                "perl run.pl --password *** --input=in.csv",
                redactCommand("perl run.pl --password \"hunter 2\" --input=in.csv"));

        // --- non-secret keys are NOT redacted (no length heuristic) ---
        assertEquals(
                "perl run.pl --very_long_parameter_name=value --input=in.csv",
                redactCommand("perl run.pl --very_long_parameter_name=value --input=in.csv"));

        // --- boolean flag values on strong credentials ARE still redacted ---
        // (a password of "true" or a token of "1" is still a credential)
        assertEquals(
                "app --password=***",
                redactCommand("app --password=true"));
        assertEquals(
                "app --token=***",
                redactCommand("app --token=1"));
        assertEquals(
                "app --secret=***",
                redactCommand("app --secret=no"));

        // --- weak credential stems with boolean values are NOT redacted ---
        // (--authentication-mode=basic, --authorize=true are flags, not secrets)
        assertEquals(
                "app --authentication-mode=basic --verbose",
                redactCommand("app --authentication-mode=basic --verbose"));
        assertEquals(
                "app --authorize=true",
                redactCommand("app --authorize=true"));

        // --- component-based: --auth-token is strong (exact sequence) ---
        assertEquals(
                "app --auth-token=*** --verbose",
                redactCommand("app --auth-token=abc123 --verbose"));

        // --- exact-sequence: these are NOT credentials ---
        // (extra components or wrong order disqualify the match)
        assertEquals(
                "app --api-key-mode=debug",
                redactCommand("app --api-key-mode=debug"));
        assertEquals(
                "app --foo-api-key-bar=value",
                redactCommand("app --foo-api-key-bar=value"));
        assertEquals(
                "app --auth-token-count=10",
                redactCommand("app --auth-token-count=10"));
        assertEquals(
                "app --key-api=value",
                redactCommand("app --key-api=value"));
        assertEquals(
                "app --token-count=10 --verbose",
                redactCommand("app --token-count=10 --verbose"));
        assertEquals(
                "app --secret-mode=debug",
                redactCommand("app --secret-mode=debug"));
        assertEquals(
                "app --password-policy=strict",
                redactCommand("app --password-policy=strict"));

        // --- weak credential: exact single-component match ---
        // (--authentication-mode, --authorize are NOT "auth")
        assertEquals(
                "app --authentication-mode=basic --verbose",
                redactCommand("app --authentication-mode=basic --verbose"));
        assertEquals(
                "app --authorize=true",
                redactCommand("app --authorize=true"));
        assertEquals(
                "app --auth=*** --verbose",
                redactCommand("app --auth=eyJhbGciOi --verbose"));
        assertEquals(
                "app --bearer=*** --verbose",
                redactCommand("app --bearer=eyJhbGciOi --verbose"));

        // --- camelCase: apiToken, clientSecret, APIKey, OAuthToken, HTTPToken ---
        assertEquals(
                "app --apiToken=***",
                redactCommand("app --apiToken=secret"));
        assertEquals(
                "app --apiKey=***",
                redactCommand("app --apiKey=secret"));
        assertEquals(
                "app --clientSecret=***",
                redactCommand("app --clientSecret=secret"));
        assertEquals(
                "app --APIKey=***",
                redactCommand("app --APIKey=secret"));
        assertEquals(
                "app --OAuthToken=***",
                redactCommand("app --OAuthToken=secret"));
        assertEquals(
                "app --HTTPToken=***",
                redactCommand("app --HTTPToken=secret"));

        // --- separate-token form: next token is an option, not a value ---
        // (--password --verbose must NOT eat --verbose as the password value)
        assertEquals(
                "app --password --verbose",
                redactCommand("app --password --verbose"));

        // --- separate-token form: strong credentials redact regardless of
        //     value appearance (no boolean guard on the separate-token path) ---
        assertEquals(
                "app --password ***",
                redactCommand("app --password true"));
        assertEquals(
                "app --token ***",
                redactCommand("app --token 1"));
        // Compound strong credential in separate-token form: the key is
        // matched (strong), so the value is redacted regardless of appearance.
        assertEquals(
                "app --client-secret ***",
                redactCommand("app --client-secret true"));

        // --- bare flag at end of line (no value) is not redacted ---
        assertEquals(
                "perl run.pl --password",
                redactCommand("perl run.pl --password"));

        // --- api-key / client_secret variants ---
        assertEquals(
                "curl --api-key=*** -u user",
                redactCommand("curl --api-key=secret123 -u user"));
        assertEquals(
                "app --client-secret *** --verbose",
                redactCommand("app --client-secret topsecret --verbose"));

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
