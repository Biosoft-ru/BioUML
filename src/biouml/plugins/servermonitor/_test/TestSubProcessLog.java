package biouml.plugins.servermonitor._test;

import biouml.plugins.servermonitor.MonitoringService;
import biouml.plugins.servermonitor.ServerMonitorConfig;
import biouml.plugins.servermonitor.SubProcessMonitor;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

    /** Thread-local marker: set on threads that are submitting a report for profile A. */
    private static final ThreadLocal<Boolean> A_THREAD_ID = new ThreadLocal<>();

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
        // New: per-pid observation / report semantics
        suite.addTest(new TestSubProcessLog("testEstimatedLifetimeUsesLastAgeOnly"));
        suite.addTest(new TestSubProcessLog("testObservationOverlapFilter"));
        suite.addTest(new TestSubProcessLog("testMergeLifetimeAndCommand"));
        suite.addTest(new TestSubProcessLog("testPersistCursorAtomicAndCopy"));
        suite.addTest(new TestSubProcessLog("testCursorIntervalIsHalfOpen"));
        suite.addTest(new TestSubProcessLog("testCursorNeverMovesBackwards"));
        suite.addTest(new TestSubProcessLog("testConcurrentReportsAreSerializedPerProfile"));
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

    /** Build a SubProcess carrying a first/last-seen observation interval. */
    private SubProcessMonitor.SubProcess makeObserved(long pid, long age, long firstSeenMs,
            long lastSeenMs, long lastAgeSec, boolean slow, String cmd) throws Exception
    {
        Constructor<SubProcessMonitor.SubProcess> ctor =
                SubProcessMonitor.SubProcess.class.getDeclaredConstructor(
                        long.class, long.class, long.class, long.class,
                        long.class, boolean.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(pid, age, firstSeenMs, lastSeenMs, lastAgeSec, slow, cmd);
    }

    /** Invoke private static SupportServlet.mergeSubProcessEntry(...) via reflection. */
    private void merge(Map<Long, ?> target, long pid, long ageSec, boolean slow, String command,
            long firstSeenMs, long lastSeenMs, long lifetimeSec) throws Exception
    {
        Class<?> servlet = Class.forName("ru.biosoft.server.servlets.support.SupportServlet");
        Method m = servlet.getDeclaredMethod("mergeSubProcessEntry",
                Map.class, long.class, long.class, boolean.class, String.class,
                long.class, long.class, long.class);
        m.setAccessible(true);
        m.invoke(null, target, pid, ageSec, slow, command, firstSeenMs, lastSeenMs, lifetimeSec);
    }

    /** Reflect into the private SubProcessEntry fields of a merged entry. */
    private long entryField(Map<Long, ?> byPid, long pid, String field) throws Exception
    {
        Object e = ((Map<?, ?>) byPid).get(pid);
        java.lang.reflect.Field f = e.getClass().getDeclaredField(field);
        f.setAccessible(true);
        Object v = f.get(e);
        if (v instanceof Long) return (Long) v;
        if (v instanceof Boolean) return ((Boolean) v) ? 1 : 0;
        return -999; // sentinel for non-numeric
    }

    /** Reflect a String field off a merged entry. */
    private String entryStringField(Map<Long, ?> byPid, long pid, String field) throws Exception
    {
        Object e = ((Map<?, ?>) byPid).get(pid);
        java.lang.reflect.Field f = e.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return (String) f.get(e);
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

    // ---------- new: per-pid observation / report semantics ----------

    /**
     * The lifetime estimate must NOT add the wall-clock observation interval on
     * top of the age delta (that double-counts the same elapsed time). Since
     * ageSeconds is measured from process start, the total-lifetime estimate is
     * simply the last observed age.
     */
    public void testEstimatedLifetimeUsesLastAgeOnly() throws Exception
    {
        // Process started long ago: at first observation age=100s, at last
        // observation age=160s, observations 60s apart. The true elapsed since
        // creation is ~160s, not 60 + (160-100) = 120s.
        long firstSeen = 1_000_000L;
        long lastSeen = firstSeen + 60_000L; // 60s of wall clock between scans
        SubProcessMonitor.SubProcess sp = makeObserved(
                42, /*ageSeconds*/160, firstSeen, lastSeen,
                /*lastAge*/160, true, "perl x.pl");
        assertEquals("lifetime must be the last observed age, not interval+ageDelta",
                160L, sp.estimatedLifetimeSec());

        // A single-scan snapshot (no ages) has no interval → -1.
        SubProcessMonitor.SubProcess snap = makeSub(7, 5, false, "Rscript a.R");
        assertEquals(-1L, snap.estimatedLifetimeSec());
    }

    /**
     * getObservedSubProcesses filters by interval OVERLAP, not by "firstSeen in
     * interval". This is what keeps the first report from pulling in unrelated
     * processes observed long before the profile, while still catching a process
     * that started before the window but is alive inside it, and one still alive
     * after the window ended.
     */
    public void testObservationOverlapFilter() throws Exception
    {
        // Effective window: [17:00, 17:10] (ms offsets from a base).
        long base = 10_000_000L;
        long ws = base;                 // 17:00
        long we = base + 10 * 60_000L;  // 17:10

        // Populate the in-memory observation map directly (bypassing the
        // update/prune cycle, which would remove "exited" pids) with four
        // observations of distinct [firstSeen, lastSeen] intervals:
        //   A: 16:58..17:05  (started before, alive in window)  -> overlaps
        //   B: 16:40..16:50  (ended before window)              -> no overlap
        //   C: 17:12..17:15  (started after window)             -> no overlap
        //   D: 16:50..18:50  (spans across the window)          -> overlaps
        seedObservation(100, base - 120_000L, base + 5 * 60_000L, 100L);
        seedObservation(200, base - 300_000L, base - 60_000L, 60L);
        seedObservation(300, base + 12 * 60_000L, base + 15 * 60_000L, 20L);
        seedObservation(400, base - 60_000L, base + 100 * 60_000L, 500L);

        List<SubProcessMonitor.SubProcess> res = service.getObservedSubProcesses(ws, we);
        java.util.Set<Long> pids = new java.util.HashSet<>();
        for (SubProcessMonitor.SubProcess s : res) pids.add(s.pid);
        assertTrue("A (started before, alive in window) must overlap", pids.contains(100L));
        assertTrue("D (spans the window) must overlap", pids.contains(400L));
        assertFalse("B (ended before window) must not overlap", pids.contains(200L));
        assertFalse("C (started after window) must not overlap", pids.contains(300L));
    }

    /**
     * Insert one observation directly into the private subProcessObservations map
     * with the given [firstSeenMs, lastSeenMs] interval, bypassing the update /
     * prune cycle (which is what this test does NOT want to exercise).
     */
    @SuppressWarnings("unchecked")
    private void seedObservation(long pid, long firstSeenMs, long lastSeenMs, long ageSec) throws Exception
    {
        java.lang.reflect.Field f = MonitoringService.class.getDeclaredField("subProcessObservations");
        f.setAccessible(true);
        java.util.Map<Long, Object> map = (java.util.Map<Long, Object>) f.get(service);
        // SubProcessObservation is a private static nested class; build one via
        // its no-arg constructor and set its public fields.
        Class<?> obsClass = Class.forName(
                "biouml.plugins.servermonitor.MonitoringService$SubProcessObservation");
        java.lang.reflect.Constructor<?> ctor = obsClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object obs = ctor.newInstance();
        setObsField(obs, "firstSeenMs", firstSeenMs);
        setObsField(obs, "lastSeenMs", lastSeenMs);
        setObsField(obs, "lastAgeSec", ageSec);
        setObsField(obs, "everSlow", true);
        setObsField(obs, "missedScans", 0);
        java.lang.reflect.Field cmd = obsClass.getDeclaredField("command");
        cmd.setAccessible(true);
        cmd.set(obs, "perl x.pl");
        map.put(pid, obs);
    }

    private void setObsField(Object obs, String name, Object value) throws Exception
    {
        java.lang.reflect.Field fld = obs.getClass().getDeclaredField(name);
        fld.setAccessible(true);
        fld.set(obs, value);
    }

    /**
     * Merging log snapshots (each a single ts with its age) must yield a real
     * lifetime (the max observed age) rather than -1, and must keep the earliest
     * firstSeen / latest lastSeen / first non-empty command.
     */
    public void testMergeLifetimeAndCommand() throws Exception
    {
        Map<Long, Object> byPid = new TreeMap<>(java.util.Comparator.reverseOrder());
        // First scan: age 100s.
        merge(byPid, 55, 100L, false, "perl a.pl", 1_000L, 1_000L, 100L);
        // Second scan (later ts): age 160s.
        merge(byPid, 55, 160L, true, "perl a.pl", 60_000L, 60_000L, 160L);

        assertEquals("firstSeen keeps the earliest", 1_000L, entryField(byPid, 55, "firstSeenMs"));
        assertEquals("lastSeen keeps the latest", 60_000L, entryField(byPid, 55, "lastSeenMs"));
        assertEquals("maxAge is the max", 160L, entryField(byPid, 55, "maxAgeSec"));
        assertEquals("lifetime is the max observed age, not -1", 160L, entryField(byPid, 55, "lifetimeSec"));
        assertEquals("slow (everSlow) is sticky", 1L, entryField(byPid, 55, "slow"));
        assertEquals("command is kept", "perl a.pl", entryStringField(byPid, 55, "command"));
    }

    /**
     * The cursor sidecar must be written atomically (temp file + rename) and must
     * not mutate the caller's JSONObject. A failed/missing target directory must
     * leave no partial file behind and not throw.
     */
    public void testPersistCursorAtomicAndCopy() throws Exception
    {
        Class<?> servlet = Class.forName("ru.biosoft.server.servlets.support.SupportServlet");
        // Build a minimal instance with no-arg ctor if available; otherwise use
        // an instance via Unsafe-free reflective constructor of the nearest
        // accessible one. SupportServlet extends AbstractJSONServlet; use the
        // declared constructor with the fewest params we can no-op.
        java.lang.reflect.Constructor<?> ctor = null;
        for (java.lang.reflect.Constructor<?> c : servlet.getDeclaredConstructors()) {
            if (c.getParameterCount() == 0) { ctor = c; break; }
        }
        Object inst;
        if (ctor != null) { ctor.setAccessible(true); inst = ctor.newInstance(); }
        else {
            // Fall back: allocate without running the constructor.
            sun.misc.Unsafe unsafe = getUnsafe();
            inst = unsafe.allocateInstance(servlet);
        }

        Method m = servlet.getDeclaredMethod("persistSubProcessCursor",
                File.class, org.json.JSONObject.class, long.class, long.class, String.class, File.class);
        m.setAccessible(true);

        File metaFile = new File(tmpDir, "profile.json");
        org.json.JSONObject meta = new org.json.JSONObject();
        meta.put("startTime", 1L);
        meta.put("endTime", 2L);

        // 1) Normal write: cursor 0 -> 1000. File created, meta NOT mutated.
        m.invoke(inst, metaFile, meta, 0L, 1000L, "profile", tmpDir);
        assertTrue("cursor file written", metaFile.exists());
        org.json.JSONObject read = new org.json.JSONObject(readAll(metaFile));
        assertEquals(1000L, read.optLong("subProcessReportCursor", 0));
        assertEquals("original startTime preserved", 1L, read.optLong("startTime", 0));
        assertFalse("caller's meta object must not be mutated",
                meta.has("subProcessReportCursor"));
        // No leftover temp files in the dir.
        String[] leftovers = tmpDir.list((d, n) -> n.endsWith(".tmp"));
        assertEquals("no temp files left behind", 0, leftovers == null ? 0 : leftovers.length);

        // 2) Non-advancing cursor (new <= old) is a no-op.
        long before = metaFile.lastModified();
        m.invoke(inst, metaFile, meta, 1000L, 1000L, "profile", tmpDir);
        assertEquals("no-op when cursor does not advance", 1000L,
                new org.json.JSONObject(readAll(metaFile)).optLong("subProcessReportCursor", 0));

        // 3) Missing parent dir → no throw, no file created.
        File missingParent = new File(tmpDir, "does-not-exist/profile.json");
        m.invoke(inst, missingParent, meta, 0L, 5L, "profile", tmpDir);
        assertFalse("no file created when parent dir missing", missingParent.exists());

        // 4) A sidecar that exists but cannot be parsed must NOT be overwritten:
        //    the cursor is skipped so the malformed metadata is left intact.
        File malformed = new File(tmpDir, "malformed.json");
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(malformed), StandardCharsets.UTF_8)) {
            w.write("this is not json");
        }
        m.invoke(inst, malformed, /*meta*/null, 0L, 2000L, "malformed", tmpDir);
        assertEquals("malformed sidecar left untouched",
                "this is not json", readAll(malformed));
    }

    /**
     * The follow-up report interval is the half-open range (cursor, now]: a scan
     * recorded exactly at the cursor was already covered by the previous report
     * and must be excluded, while one strictly after it is included. This is what
     * makes the cumulative cursor not re-report the boundary observation (it only
     * works because the summary now advances the cursor even when the entry list
     * is truncated — see appendSubProcessSummary).
     */
    public void testCursorIntervalIsHalfOpen() throws Exception
    {
        long base = System.currentTimeMillis();
        java.util.ArrayList<SubProcessMonitor.SubProcess> subs = new java.util.ArrayList<>();
        subs.add(makeSub(1, 300, true, "perl a.pl"));
        append(subs, base);
        append(subs, base + 60_000L);
        append(subs, base + 120_000L);

        long cursor = base + 60_000L; // the middle scan, already "reported"

        // The summary reads the follow-up log with lower bound cursor+1 so that a
        // scan exactly at the cursor is excluded and the later one kept.
        List<String> after = service.readSubProcessLog(cursor + 1, 0);
        assertEquals("scan exactly at the cursor is excluded", 1, after.size());
        assertEquals(base + 120_000L,
                new org.json.JSONObject(after.get(0)).getLong("timestamp"));

        // The log reader itself is still inclusive on both ends (contract unchanged
        // for other callers): a record at the lower bound is included.
        List<String> inclusive = service.readSubProcessLog(cursor, 0);
        assertEquals("log reader lower bound is inclusive", 2, inclusive.size());

        // Upper bound is inclusive.
        List<String> atUpper = service.readSubProcessLog(base, base + 120_000L);
        assertEquals("upper bound is inclusive", 3, atUpper.size());

        // In-memory filter: an observation whose lastSeen is exactly at the query
        // start is excluded (lastSeen > qStart required).
        long qStart = 10_000_000L;
        seedObservation(500, qStart - 5_000L, qStart, 100L);          // lastSeen == qStart
        seedObservation(501, qStart - 5_000L, qStart + 1L, 100L);     // lastSeen > qStart
        java.util.Set<Long> pids = new java.util.HashSet<>();
        for (SubProcessMonitor.SubProcess s : service.getObservedSubProcesses(qStart, 0))
            pids.add(s.pid);
        assertFalse("obs ending exactly at the query start is excluded", pids.contains(500L));
        assertTrue("obs still alive strictly after the query start is included", pids.contains(501L));
    }

    /**
     * Two reports can be generated from the same read cursor and then persist
     * their (different) report-time values out of order. The high-water guard in
     * persistSubProcessCursor must prevent the later-written (older) cursor from
     * clobbering the newer one, so the persisted cursor never moves backwards.
     */
    public void testCursorNeverMovesBackwards() throws Exception
    {
        Class<?> servlet = Class.forName("ru.biosoft.server.servlets.support.SupportServlet");
        Object inst = newSupportServlet();
        Method persist = servlet.getDeclaredMethod("persistSubProcessCursor",
                File.class, org.json.JSONObject.class, long.class, long.class, String.class, File.class);
        persist.setAccessible(true);

        File metaFile = new File(tmpDir, "profile.json");
        org.json.JSONObject meta = new org.json.JSONObject();
        meta.put("startTime", 1L);
        meta.put("endTime", 2L);

        // Report B persists the newer high-water mark first (cursor 0 -> 300).
        persist.invoke(inst, metaFile, meta, 0L, 300L, "profile", tmpDir);
        assertEquals(300L, new org.json.JSONObject(readAll(metaFile)).optLong("subProcessReportCursor", 0));

        // Report A (still working off the cursor it read before B) tries to persist
        // its older value 200. It must NOT move the cursor backwards.
        persist.invoke(inst, metaFile, meta, 0L, 200L, "profile", tmpDir);
        assertEquals("cursor must not regress", 300L,
                new org.json.JSONObject(readAll(metaFile)).optLong("subProcessReportCursor", 0));

        // A genuinely newer value still advances it.
        persist.invoke(inst, metaFile, meta, 300L, 400L, "profile", tmpDir);
        assertEquals("newer cursor advances", 400L,
                new org.json.JSONObject(readAll(metaFile)).optLong("subProcessReportCursor", 0));
    }

    /**
     * Regression test for the per-profile serialization of
     * read -> generate -> persist. The monotonic guard alone would let two reports
     * both advance the cursor (no regression, but overlapping reports); only the
     * per-profile lock makes concurrent same-profile reports run to completion in
     * turn. This drives that path via the real {@code appendSubProcessSummary}
     * (plugin returns null -> early return, which still acquires/releases the
     * profile lock) and asserts two same-profile reports never overlap, while a
     * different-profile report can run concurrently. Also asserts the registry does
     * not retain locks after the calls return.
     */
    public void testConcurrentReportsAreSerializedPerProfile() throws Exception
    {
        Class<?> servlet = Class.forName("ru.biosoft.server.servlets.support.SupportServlet");
        Object inst = newSupportServlet();
        Method append = servlet.getDeclaredMethod("appendSubProcessSummary",
                StringBuilder.class, File.class, String.class, File.class);
        append.setAccessible(true);

        File profileDir = new File(tmpDir, "prof");
        profileDir.mkdirs();
        final File profileA = new File(profileDir, "a.json");
        final File profileB = new File(profileDir, "b.json");

        // Measure lock hold precisely via the in-lock test hook, scoped to profile A
        // by its canonical path. Count how many threads are simultaneously inside
        // profile A's lock and assert it never exceeds 1 (the point of the
        // per-profile serialization). Profile B threads run concurrently on a
        // different lock and are simply not counted.
        final String aKey = profileA.getCanonicalPath();
        final java.util.concurrent.atomic.AtomicInteger inLockA = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger maxConcurrentA = new java.util.concurrent.atomic.AtomicInteger();
        java.lang.reflect.Field hookField = servlet.getDeclaredField("subProcessReportLockTestHook");
        hookField.setAccessible(true);
        hookField.set(null, (Runnable) () -> {
            // The hook runs inside appendSubProcessSummary, which knows the profile;
            // it does not receive the key, so we approximate by checking the current
            // thread is working on profile A. Since the hook is global, we instead
            // only count when the in-lock sleep is for A — done by having each A
            // thread signal its identity via a thread-local before invoking.
            Object id = A_THREAD_ID.get();
            if (id == null) return; // a profile-B thread; not counted
            int cur = inLockA.incrementAndGet();
            maxConcurrentA.accumulateAndGet(cur, Math::max);
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            inLockA.decrementAndGet();
        });

        final int N = 8;
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < N; i++)
        {
            final boolean isA = (i % 2 == 0);
            final File p = isA ? profileA : profileB;
            final String name = isA ? "a" : "b";
            threads.add(new Thread(() -> {
                if (isA) A_THREAD_ID.set(Boolean.TRUE);
                try
                {
                    append.invoke(inst, new StringBuilder(), p, name, profileDir);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                finally
                {
                    A_THREAD_ID.remove();
                }
            }));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        hookField.set(null, null); // clear the hook

        // Same-profile (A) reports must never have overlapped in the lock.
        assertTrue("concurrent same-profile reports must be serialized (maxConcurrentA="
                + maxConcurrentA.get() + ")", maxConcurrentA.get() <= 1);

        // The per-profile lock registry must not retain entries after reports return.
        java.lang.reflect.Field f = servlet.getDeclaredField("subProcessReportLocks");
        f.setAccessible(true);
        Object registry = f.get(null);
        assertTrue("lock registry must be empty after reports complete",
                ((java.util.Map<?, ?>) registry).isEmpty());
    }

    /** Build a SupportServlet instance the same way the persistence test does. */
    private static Object newSupportServlet() throws Exception
    {
        Class<?> servlet = Class.forName("ru.biosoft.server.servlets.support.SupportServlet");
        for (java.lang.reflect.Constructor<?> c : servlet.getDeclaredConstructors()) {
            if (c.getParameterCount() == 0) { c.setAccessible(true); return c.newInstance(); }
        }
        return getUnsafe().allocateInstance(servlet);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception
    {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    private static String readAll(File f) throws Exception
    {
        return new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
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
