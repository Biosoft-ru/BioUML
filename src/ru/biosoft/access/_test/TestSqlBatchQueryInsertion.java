package ru.biosoft.access._test;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import ru.biosoft.access.SqlDataCollection;

/**
 * Tests for the private static helpers findTrailingClauseIndex and
 * hasWholeWord used by SqlDataCollection.getSortedIterator to safely
 * insert an IN (...) filter into a SELECT query.
 *
 * These are exercised via reflection because the methods are package-private
 * in ru.biosoft.access and the test lives in ru.biosoft.access._test.
 */
public class TestSqlBatchQueryInsertion extends TestCase
{
    private Method findTrailingClauseIndex;
    private Method hasWholeWord;

    @Override
    protected void setUp() throws Exception
    {
        findTrailingClauseIndex = SqlDataCollection.class.getDeclaredMethod(
                "findTrailingClauseIndex", String.class, String.class );
        findTrailingClauseIndex.setAccessible( true );
        hasWholeWord = SqlDataCollection.class.getDeclaredMethod(
                "hasWholeWord", String.class, String.class );
        hasWholeWord.setAccessible( true );
    }

    // --- findTrailingClauseIndex: real transformer queries ---

    public void testDataElementsSqlTransformerWithOrderByName() throws Exception
    {
        // DataElementsSqlTransformer.getSelectQuery() returns:
        // "SELECT id,name FROM data_element WHERE parent='X' ORDER BY name"
        String sql = "SELECT id,name FROM data_element WHERE parent='X' ORDER BY name";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertEquals( "ORDER BY should be found", sql.indexOf( "ORDER BY" ), idx );
    }

    public void testGORelationTransformerWithOrderByName() throws Exception
    {
        // GORelationTransformer.getSelectQuery() returns a query ending with
        // "...order by t1,t2" (lowercase)
        String sql = "select t2.acc t1,t1.acc t2,t3.acc type from term t1,term t2,term t3,term2term "
                + "where term1_id=t1.id and term2_id=t2.id and relationship_type_id=t3.id "
                + "and t1.term_type IN ('biological_process','molecular_function','cellular_component') "
                + "and t2.term_type IN ('biological_process','molecular_function','cellular_component') "
                + "order by t1,t2";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertTrue( "lowercase 'order by' should be found case-insensitively", idx >= 0 );
    }

    public void testNoTrailingClause() throws Exception
    {
        String sql = "SELECT name, start, end, type, source, status, user, message, properties FROM tasks";
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" ) );
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "GROUP BY" ) );
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "LIMIT" ) );
    }

    // --- findTrailingClauseIndex: edge cases ---

    public void testMultiLineOrderingBy() throws Exception
    {
        // ORDER BY preceded by newline — the right-boundary check must
        // accept any whitespace, not just space.
        String sql = "SELECT * FROM t WHERE x=1\nORDER BY name";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertTrue( "newline-separated ORDER BY should be found", idx >= 0 );
    }

    public void testOrderByInsideStringLiteral() throws Exception
    {
        // "ORDER BY" inside a single-quoted literal must be skipped
        String sql = "SELECT * FROM t WHERE note='ORDER BY date' AND x=1";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertEquals( "ORDER BY inside quotes should not match", -1, idx );
    }

    public void testOrderByInsideBacktickIdentifier() throws Exception
    {
        // Backtick-quoted identifier containing "ORDER BY" must be skipped
        String sql = "SELECT * FROM t WHERE `ORDER BY col`=1";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertEquals( "ORDER BY inside backticks should not match", -1, idx );
    }

    public void testIdentifierEndingInOrder() throws Exception
    {
        // A column named "xORDER" followed by " BY" must not false-positive
        String sql = "SELECT xORDER BY col FROM t";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        assertEquals( "xORDER BY should not match as ORDER BY", -1, idx );
    }

    public void testLimitClause() throws Exception
    {
        String sql = "SELECT * FROM t WHERE x=1 LIMIT 10";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "LIMIT" );
        assertTrue( "LIMIT should be found", idx >= 0 );
    }

    public void testGroupByClause() throws Exception
    {
        String sql = "SELECT name, COUNT(*) FROM t GROUP BY name";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "GROUP BY" );
        assertTrue( "GROUP BY should be found", idx >= 0 );
    }

    public void testCombinedOrderByAndLimit() throws Exception
    {
        String sql = "SELECT * FROM t WHERE x=1 ORDER BY name LIMIT 5";
        int orderIdx = (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" );
        int limitIdx = (int) findTrailingClauseIndex.invoke( null, sql, "LIMIT" );
        assertTrue( "ORDER BY should be found", orderIdx >= 0 );
        assertTrue( "LIMIT should be found", limitIdx >= 0 );
        assertTrue( "ORDER BY should come before LIMIT", orderIdx < limitIdx );
    }

    // --- hasWholeWord ---

    public void testHasWholeWordBasic() throws Exception
    {
        assertTrue( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t WHERE x=1", "WHERE" ) );
        assertFalse( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t", "WHERE" ) );
    }

    public void testHasWholeWordCaseInsensitive() throws Exception
    {
        assertTrue( (boolean) hasWholeWord.invoke( null, "select * from t where x=1", "WHERE" ) );
        assertTrue( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t wHERE x=1", "WHERE" ) );
    }

    public void testHasWholeWordNotSubstring() throws Exception
    {
        assertFalse( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t WHEREX x=1", "WHERE" ) );
        assertFalse( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t XWHERE x=1", "WHERE" ) );
    }

    public void testHasWholeWordInsideLiteral() throws Exception
    {
        // "WHERE" only appears inside a single-quoted literal (no real WHERE
        // clause) — should not match.  (Not valid SQL, but exercises the
        // literal-skipping logic.)
        assertFalse( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t note='WHERE'", "WHERE" ) );
        // Real WHERE clause present — should match even though another
        // "WHERE" is also inside a literal
        assertTrue( (boolean) hasWholeWord.invoke( null, "SELECT * FROM t WHERE x=1 AND note='WHERE'", "WHERE" ) );
    }
}
