package ru.biosoft.access._test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.Environment;
import ru.biosoft.access.security.BiosoftClassLoading;

/**
 * Regression test for the batched-rows optimization in
 * {@link SqlDataCollection#getSortedIterator}.
 *
 * <p>The batch path used to build each row directly via
 * {@code transformer.create(...)} and return it, which bypassed any subclass
 * override of {@code doGet(name)}. That override is what lets a caller serve a
 * <em>live</em> in-memory element for a row that is currently being mutated —
 * the canonical case being {@code TaskManager.getTasksInfo()}, which returns the
 * running task from memory instead of the stale DB row that
 * {@code TasksSqlTransformer.create()} would render as "terminated due to
 * server shutdown".
 *
 * <p>After the fix, {@code getSortedIterator} resolves each element through
 * {@code doGet(name, batch)} — passing the <em>iterator-local</em> batch as a
 * parameter (never stored on the collection) — so a subclass override can
 * intercept the element first while the base implementation still serves the
 * row from the batch without a per-row SELECT.
 *
 * <p>These tests drive the real {@code getSortedIterator().next()} path against
 * a stub transformer and dynamic-proxy JDBC objects (no real DB). They cover:
 * <ol>
 *   <li>an element intercepted by the {@code doGet} override is returned
 *       verbatim (not the stale batched row);</li>
 *   <li>normal elements come from the batch without a per-row DB query;</li>
 *   <li>two concurrent iterators do not interfere with each other's batch;</li>
 *   <li>an override that returns {@code null} means "no element" (the batched
 *       value is not used as a fallback — the element is not resurrected);</li>
 *   <li>a batch-query failure falls back to per-row {@code doGet}, verified
 *       by asserting that the base implementation performs a per-row SELECT.</li>
 * </ol>
 */
public class TestSqlDataCollectionBatchedDoGet extends TestCase
{
    @Override
    protected void setUp() throws Exception
    {
        // Ensure the access environment (class loading) is available so a
        // DataCollection can be constructed in the bare test JVM.
        if( Environment.getClassLoading() == null )
            Environment.setClassLoading( new BiosoftClassLoading() );
    }

    /** Minimal {@link DataElement} used as the element type in the test. */
    static class StubElement implements DataElement
    {
        private final String name;
        private final boolean live;

        StubElement( String name, boolean live )
        {
            this.name = name;
            this.live = live;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return null;
        }

        /** Marks an element produced by the {@code doGet} override rather than the batch. */
        boolean isLive()
        {
            return live;
        }
    }

    /** Stub transformer that reads the in-memory "table" supplied via {@link #rows}. */
    static class StubTransformer implements SqlTransformer<StubElement>
    {
        /** In-memory table: name -> row element (the value {@code create} returns). */
        Map<String, StubElement> rows = new HashMap<>();

        @Override
        public boolean init( SqlDataCollection<StubElement> owner )
        {
            return true;
        }

        @Override
        public Class<StubElement> getTemplateClass()
        {
            return StubElement.class;
        }

        @Override
        public String getIdField()
        {
            return "name";
        }

        @Override
        public StubElement create( ResultSet resultSet, Connection connection ) throws Exception
        {
            return rows.get( resultSet.getString( "name" ) );
        }

        @Override
        public String getSelectQuery()
        {
            return "SELECT name FROM test_table";
        }

        @Override
        public String getCountQuery()
        {
            return "SELECT COUNT(*) FROM test_table";
        }

        @Override
        public String getNameListQuery()
        {
            return "SELECT name FROM test_table";
        }

        @Override
        public boolean isNameListSorted()
        {
            return false;
        }

        @Override
        public String getElementQuery( String name )
        {
            return "SELECT name FROM test_table WHERE name='" + name + "'";
        }

        @Override
        public String getElementExistsQuery( String name )
        {
            return getElementQuery( name );
        }

        @Override
        public String[] getUsedTables()
        {
            return null;
        }

        @Override
        public String getCreateTableQuery( String tableName )
        {
            return null;
        }

        @Override
        public void addInsertCommands( Statement st, StubElement de )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addUpdateCommands( Statement st, StubElement de )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addDeleteCommands( Statement st, String name )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSortingSupported()
        {
            return true;
        }

        @Override
        public String[] getSortableFields()
        {
            return new String[] { "name" };
        }

        @Override
        public String getSortedNameListQuery( String field, boolean direction )
        {
            return "SELECT name FROM test_table ORDER BY name";
        }
    }

    // --- JDBC stubs built with dynamic proxies (no real driver) ---

    /**
     * Builds a {@link ResultSet} proxy whose {@code next()} walks a fixed list of
     * row names. {@code getString(...)} returns the current row name regardless of
     * whether the argument is a column label (String) or a column index (int), so
     * both the {@code transformer.create} path (which calls
     * {@code getString("name")}) and the {@code getSortedNameList} /
     * {@code SqlUtil.queryStrings} path (which calls {@code getString(1)}) work.
     */
    private static ResultSet makeResultSet( StubTransformer t, List<String> names )
    {
        final int[] idx = { -1 };
        InvocationHandler h = ( proxy, method, args ) ->
        {
            switch( method.getName() )
            {
                case "next":
                    idx[0]++;
                    return idx[0] < names.size();
                case "getString":
                    // args[0] is either a String label or an int column index;
                    // return the current row name in both cases.
                    return names.get( idx[0] );
                case "close":
                    return null;
                case "isWrapperFor":
                    return false;
                case "unwrap":
                    throw new UnsupportedOperationException( "unwrap not stubbed" );
                case "hashCode":
                    return System.identityHashCode( proxy );
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "StubResultSet";
                default:
                    // Return a sensible default for any other accessor; throw for
                    // accessors the code under test should never reach.
                    Class<?> rt = method.getReturnType();
                    if( rt == boolean.class ) return false;
                    if( rt == int.class ) return 0;
                    if( rt == long.class ) return 0L;
                    if( rt == double.class ) return 0.0d;
                    if( rt == float.class ) return 0.0f;
                    if( rt == void.class ) return null;
                    return null;
            }
        };
        return ( ResultSet ) Proxy.newProxyInstance( ResultSet.class.getClassLoader(), new Class<?>[]{ ResultSet.class }, h );
    }

    /**
     * Builds a {@link Connection} proxy whose {@code createStatement()} returns a
     * statement whose {@code executeQuery} yields {@link #makeResultSet} over the
     * supplied row names — exactly what {@code getSortedIterator}'s batch query
     * needs.
     */
    private static Connection makeConnection( StubTransformer t, List<String> batchNames )
    {
        final Statement stmt = ( Statement ) Proxy.newProxyInstance(
                Statement.class.getClassLoader(), new Class<?>[]{ Statement.class },
                ( proxy, method, args ) ->
                {
                    switch( method.getName() )
                    {
                        case "executeQuery":
                            return makeResultSet( t, batchNames );
                        case "close":
                            return null;
                        case "isClosed":
                            return false;
                        case "isWrapperFor":
                            return false;
                        case "unwrap":
                            throw new UnsupportedOperationException( "unwrap not stubbed" );
                        case "hashCode":
                            return System.identityHashCode( proxy );
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "StubStatement";
                        default:
                            Class<?> rt = method.getReturnType();
                            if( rt == boolean.class ) return false;
                            if( rt == int.class ) return 0;
                            if( rt == long.class ) return 0L;
                            if( rt == double.class ) return 0.0d;
                            if( rt == float.class ) return 0.0f;
                            if( rt == void.class ) return null;
                            return null;
                    }
                } );

        return ( Connection ) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{ Connection.class },
                ( proxy, method, args ) ->
                {
                    switch( method.getName() )
                    {
                        case "createStatement":
                            return stmt;
                        case "close":
                            return null;
                        case "isClosed":
                            return false;
                        case "isWrapperFor":
                            return false;
                        case "unwrap":
                            throw new UnsupportedOperationException( "unwrap not stubbed" );
                        case "hashCode":
                            return System.identityHashCode( proxy );
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "StubConnection";
                        default:
                            Class<?> rt = method.getReturnType();
                            if( rt == boolean.class ) return false;
                            if( rt == int.class ) return 0;
                            if( rt == long.class ) return 0L;
                            if( rt == double.class ) return 0.0d;
                            if( rt == float.class ) return 0.0f;
                            if( rt == void.class ) return null;
                            return null;
                    }
                } );
    }

    /**
     * Test collection: constructed without the real DB-backed {@code init()} and
     * with {@code doGet(name, batch)} overridden to stand in for
     * {@code TaskManager.getTasksInfo()}'s live-task interception.
     */
    static class TestCollection extends SqlDataCollection<StubElement>
    {
        private final List<String> liveOverrides;   // names whose live element is returned by doGet
        private final List<String> filteredOut;     // names doGet returns null for
        int perRowDbQueries = 0;                    // count of per-row DB lookups the base doGet would perform
        Connection connection;                      // injected stub connection

        TestCollection( List<String> liveOverrides, List<String> filteredOut )
        {
            super( null, new java.util.Properties() );
            this.liveOverrides = liveOverrides;
            this.filteredOut = filteredOut;
        }

        @Override
        public Connection getConnection()
        {
            return connection;
        }

        @Override
        protected StubElement doGet( String name, Map<String, StubElement> batch ) throws Exception
        {
            // Mirrors TaskManager.getTasksInfo(): return the live in-memory
            // element for a name the caller knows is currently active.
            if( liveOverrides.contains( name ) )
                return new StubElement( name, true );
            if( filteredOut.contains( name ) )
                return null;   // "no element" — must NOT be resurrected from batch

            // Count whether the base implementation would have to hit the DB for
            // this name (i.e. it is not present in the iterator-local batch).
            if( batch == null || batch.get( name ) == null )
                perRowDbQueries++;

            return super.doGet( name, batch );
        }

        @Override
        protected void init()
        {
            // No-op: transformer and connection are injected directly.
        }
    }

    private static void injectTransformer( SqlDataCollection<?> collection, SqlTransformer<?> transformer ) throws Exception
    {
        Field f = SqlDataCollection.class.getDeclaredField( "transformer" );
        f.setAccessible( true );
        f.set( collection, transformer );
    }

    /** Build a collection whose batch query returns the given row names. */
    private static TestCollection newCollection( StubTransformer t, String[] batchNames,
            List<String> liveOverrides, List<String> filteredOut ) throws Exception
    {
        TestCollection col = new TestCollection( liveOverrides, filteredOut );
        injectTransformer( col, t );
        col.connection = makeConnection( t, Arrays.asList( batchNames ) );
        return col;
    }

    // --- 1. A live (doGet-overridden) element is returned verbatim, not the batched row ---

    public void testDoGetOverrideInterceptsBatchedRow() throws Exception
    {
        StubTransformer t = new StubTransformer();
        t.rows.put( "a", new StubElement( "a", false ) );
        t.rows.put( "b", new StubElement( "b", false ) );   // stale DB row for the live task
        t.rows.put( "c", new StubElement( "c", false ) );

        TestCollection col = newCollection( t, new String[] { "a", "b", "c" },
                Collections.singletonList( "b" ), Collections.<String>emptyList() );

        java.util.Iterator<StubElement> it = col.getSortedIterator( "name", true, 0, 3 );

        StubElement a = it.next();
        assertFalse( "element 'a' should come from the batch, not the override", a.isLive() );

        StubElement b = it.next();
        assertTrue( "element 'b' should be the live override, not the batched row", b.isLive() );

        StubElement c = it.next();
        assertFalse( "element 'c' should come from the batch, not the override", c.isLive() );

        assertEquals( "no per-row DB query should occur for batched names", 0, col.perRowDbQueries );
    }

    // --- 2. Normal elements are served from the batch without a per-row DB query ---

    public void testBatchedRowsAvoidPerRowDbQuery() throws Exception
    {
        StubTransformer t = new StubTransformer();
        t.rows.put( "x", new StubElement( "x", false ) );
        t.rows.put( "y", new StubElement( "y", false ) );

        TestCollection col = newCollection( t, new String[] { "x", "y" },
                Collections.<String>emptyList(), Collections.<String>emptyList() );

        java.util.Iterator<StubElement> it = col.getSortedIterator( "name", true, 0, 2 );
        assertEquals( "x", it.next().getName() );
        assertEquals( "y", it.next().getName() );

        assertEquals( "all names were in the batch, so no DB lookup may occur", 0, col.perRowDbQueries );
    }

    // --- 3. Two concurrent iterators do not interfere with each other's batch ---

    public void testTwoIteratorsDoNotInterfere() throws Exception
    {
        StubTransformer t = new StubTransformer();
        t.rows.put( "A1", new StubElement( "A1", false ) );
        t.rows.put( "A2", new StubElement( "A2", false ) );
        t.rows.put( "B1", new StubElement( "B1", false ) );
        t.rows.put( "B2", new StubElement( "B2", false ) );

        TestCollection col = new TestCollection( Collections.<String>emptyList(), Collections.<String>emptyList() );
        injectTransformer( col, t );

        // Iterator A queries rows A1, A2.
        col.connection = makeConnection( t, Arrays.asList( "A1", "A2" ) );
        java.util.Iterator<StubElement> itA = col.getSortedIterator( "name", true, 0, 2 );

        // Iterator B (created while A is still alive) queries rows B1, B2.
        col.connection = makeConnection( t, Arrays.asList( "B1", "B2" ) );
        java.util.Iterator<StubElement> itB = col.getSortedIterator( "name", true, 0, 2 );

        // Interleave: A1, B1, A2, B2 — each must come from ITS OWN batch.
        assertEquals( "A1", itA.next().getName() );
        assertEquals( "B1", itB.next().getName() );
        assertEquals( "A2", itA.next().getName() );
        assertEquals( "B2", itB.next().getName() );

        assertEquals( "no per-row DB query for any batched name", 0, col.perRowDbQueries );
    }

    // --- 4. An override that returns null means "no element" (batch value not resurrected) ---

    public void testDoGetOverrideReturningNullDropsElement() throws Exception
    {
        StubTransformer t = new StubTransformer();
        t.rows.put( "n1", new StubElement( "n1", false ) );
        t.rows.put( "n2", new StubElement( "n2", false ) );

        // The override filters "n2" out (returns null). The element must be
        // dropped — NOT resurrected from the batch.
        TestCollection col = newCollection( t, new String[] { "n1", "n2" },
                Collections.<String>emptyList(), Collections.singletonList( "n2" ) );

        java.util.Iterator<StubElement> it = col.getSortedIterator( "name", true, 0, 2 );
        assertEquals( "n1", it.next().getName() );

        // 'n2' was filtered out by the override; next() must return null, not the
        // stale batch element.
        assertNull( "filtered-out 'n2' must not be resurrected from the batch", it.next() );
    }

    // --- 5. A batch-query failure falls back to per-row doGet (verifies per-row DB lookup) ---

    public void testBatchFailureFallsBackToPerRowDoGet() throws Exception
    {
        StubTransformer t = new StubTransformer();
        t.rows.put( "p", new StubElement( "p", false ) );

        // The getSortedIterator flow makes THREE executeQuery calls:
        //   1. getSortedNameList → queryStrings → executeQuery  (must SUCCEED, returns ["p"])
        //   2. batch query → executeQuery                       (must FAIL)
        //   3. per-row fallback → doGet("p") → executeQuery     (must SUCCEED, returns row "p")
        // We use a call counter to verify all three happen.
        final int[] queryCallCount = { 0 };

        final Connection mixedConn = ( Connection ) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{ Connection.class },
                ( proxy, method, args ) ->
                {
                    if( method.getName().equals( "createStatement" ) )
                    {
                        return ( Statement ) Proxy.newProxyInstance(
                                Statement.class.getClassLoader(), new Class<?>[]{ Statement.class },
                                ( p2, m2, a2 ) ->
                                {
                                    if( m2.getName().equals( "executeQuery" ) )
                                    {
                                        queryCallCount[0]++;
                                        if( queryCallCount[0] == 1 )
                                        {
                                            // First call: getSortedNameList — return a
                                            // ResultSet with one row "p"
                                            return makeResultSet( t, Arrays.asList( "p" ) );
                                        }
                                        if( queryCallCount[0] == 2 )
                                        {
                                            // Second call: batch query — fail
                                            throw new RuntimeException( "simulated batch failure" );
                                        }
                                        // Third call: per-row fallback — return a
                                        // ResultSet with one row "p"
                                        return makeResultSet( t, Arrays.asList( "p" ) );
                                    }
                                    Class<?> rt = m2.getReturnType();
                                    if( rt == boolean.class ) return false;
                                    if( rt == void.class ) return null;
                                    return null;
                                } );
                    }
                    Class<?> rt = method.getReturnType();
                    if( rt == boolean.class ) return false;
                    if( rt == void.class ) return null;
                    return null;
                } );

        TestCollection col = new TestCollection( Collections.<String>emptyList(), Collections.<String>emptyList() )
        {
            @Override
            public Connection getConnection()
            {
                return mixedConn;
            }

            // NOTE: we do NOT override doGet(name, batch) here — we want the BASE
            // implementation to run so we can verify it performs a per-row DB lookup
            // when the batch is null (i.e. in the per-row fallback path).
        };
        injectTransformer( col, t );

        java.util.Iterator<StubElement> it = col.getSortedIterator( "name", true, 0, 1 );
        StubElement p = it.next();
        assertNotNull( "element should be resolved via per-row fallback", p );
        assertEquals( "p", p.getName() );

        // Verify all three queries happened:
        //   1. getSortedNameList
        //   2. batch query (failed)
        //   3. per-row fallback
        assertEquals( "expected exactly 3 executeQuery calls (nameList, batch, per-row fallback)",
                3, queryCallCount[0] );
    }
}
