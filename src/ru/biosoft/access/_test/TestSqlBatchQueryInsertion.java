package ru.biosoft.access._test;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import ru.biosoft.access.SqlDataCollection;

/**
 * Tests for the private static helpers indexOfWholeWord,
 * findTrailingClauseIndex, and buildBatchQuery used by
 * SqlDataCollection.getSortedIterator to safely insert an IN (...)
 * filter into a SELECT query.
 *
 * These are exercised via reflection because the methods are private
 * in ru.biosoft.access and the test lives in ru.biosoft.access._test.
 */
public class TestSqlBatchQueryInsertion extends TestCase
{
    private Method findTrailingClauseIndex;
    private Method hasWholeWord;
    private Method indexOfWholeWord;
    private Method buildBatchQuery;

    @Override
    protected void setUp() throws Exception
    {
        findTrailingClauseIndex = SqlDataCollection.class.getDeclaredMethod(
                "findTrailingClauseIndex", String.class, String.class );
        findTrailingClauseIndex.setAccessible( true );
        hasWholeWord = SqlDataCollection.class.getDeclaredMethod(
                "hasWholeWord", String.class, String.class );
        hasWholeWord.setAccessible( true );
        indexOfWholeWord = SqlDataCollection.class.getDeclaredMethod(
                "indexOfWholeWord", String.class, String.class );
        indexOfWholeWord.setAccessible( true );
        buildBatchQuery = SqlDataCollection.class.getDeclaredMethod(
                "buildBatchQuery", String.class, String.class );
        buildBatchQuery.setAccessible( true );
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

    // --- findTrailingClauseIndex: stricter right boundary ---

    public void testOrderByNoSpaceBeforeParen() throws Exception
    {
        // "GROUP BY(x)" — no whitespace after "BY".  Both ORDER BY and
        // GROUP BY must be rejected by the stricter scan because the
        // non-whitespace '(' after the clause means it is not a complete
        // clause token (it's part of a larger expression, not a trailing
        // ORDER BY / GROUP BY clause).
        String sql = "SELECT * FROM t GROUP BY(x)";
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "ORDER BY" ) );
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "GROUP BY" ) );
        // Normal "GROUP BY name" (with space) should still be found
        String sql2 = "SELECT * FROM t GROUP BY name";
        assertTrue( "GROUP BY with space should be found",
                (int) findTrailingClauseIndex.invoke( null, sql2, "GROUP BY" ) >= 0 );
    }

    public void testLimitNoSpaceBeforeDigit() throws Exception
    {
        // "LIMIT5" — no whitespace after LIMIT.  The stricter scan
        // must not match because '5' is not whitespace.
        String sql = "SELECT * FROM t LIMIT5";
        assertEquals( -1, (int) findTrailingClauseIndex.invoke( null, sql, "LIMIT" ) );
    }

    public void testLimitWithSpaceStillFound() throws Exception
    {
        // "LIMIT 10" — normal case, still found with stricter boundary
        String sql = "SELECT * FROM t LIMIT 10";
        int idx = (int) findTrailingClauseIndex.invoke( null, sql, "LIMIT" );
        assertTrue( "LIMIT with space should be found", idx >= 0 );
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

    // --- indexOfWholeWord: the core primitive ---

    public void testIndexOfWholeWordBasic() throws Exception
    {
        int idx = (int) indexOfWholeWord.invoke( null, "SELECT * FROM t WHERE x=1", "WHERE" );
        assertEquals( 16, idx );
    }

    public void testIndexOfWholeWordNotFound() throws Exception
    {
        assertEquals( -1, (int) indexOfWholeWord.invoke( null, "SELECT * FROM t", "WHERE" ) );
    }

    public void testIndexOfWholeWordCaseInsensitive() throws Exception
    {
        int idx = (int) indexOfWholeWord.invoke( null, "select * from t where x=1", "WHERE" );
        assertEquals( 16, idx );
    }

    public void testIndexOfWholeWordInsideLiteralSkipped() throws Exception
    {
        // Only "WHERE" is inside a literal — should return -1
        assertEquals( -1, (int) indexOfWholeWord.invoke( null, "SELECT * FROM t note='WHERE'", "WHERE" ) );
        // Real WHERE at position 16, literal WHERE at 30 — should find the real one
        int idx = (int) indexOfWholeWord.invoke( null, "SELECT * FROM t WHERE x=1 AND note='WHERE'", "WHERE" );
        assertEquals( 16, idx );
    }

    public void testIndexOfWholeWordLeftBoundary() throws Exception
    {
        assertEquals( -1, (int) indexOfWholeWord.invoke( null, "SELECT * FROM t WHEREX x=1", "WHERE" ) );
    }

    public void testIndexOfWholeWordRightBoundary() throws Exception
    {
        assertEquals( -1, (int) indexOfWholeWord.invoke( null, "SELECT * FROM t XWHERE x=1", "WHERE" ) );
    }

    // --- buildBatchQuery: the full assembly logic ---

    public void testBuildBatchQueryNoWhere() throws Exception
    {
        String sql = "SELECT name, start, end FROM tasks";
        String result = (String) buildBatchQuery.invoke( null, sql, "name IN ('a','b')" );
        assertEquals( "SELECT name, start, end FROM tasks WHERE name IN ('a','b')", result );
    }

    public void testBuildBatchQueryWithWhere() throws Exception
    {
        String sql = "SELECT * FROM tasks WHERE user='x'";
        String result = (String) buildBatchQuery.invoke( null, sql, "name IN ('a','b')" );
        assertEquals( "SELECT * FROM tasks WHERE (user='x') AND name IN ('a','b')", result );
    }

    public void testBuildBatchQueryWithWhereAndOrderBy() throws Exception
    {
        // DataElementsSqlTransformer pattern: WHERE + trailing ORDER BY
        String sql = "SELECT id,name FROM data_element WHERE parent='X' ORDER BY name";
        String result = (String) buildBatchQuery.invoke( null, sql, "id IN ('1','2')" );
        assertEquals( "SELECT id,name FROM data_element WHERE (parent='X') AND id IN ('1','2') ORDER BY name", result );
    }

    public void testBuildBatchQueryWithWhereOrAndOrderBy() throws Exception
    {
        // CRITICAL: top-level OR in the WHERE clause.  Without parenthesization,
        // "WHERE a=1 OR b=2 AND id IN (...)" parses as "a=1 OR (b=2 AND id IN (...))"
        // and silently returns rows matching a=1 that are NOT in the batch.
        String sql = "SELECT * FROM t WHERE a=1 OR b=2 ORDER BY name";
        String result = (String) buildBatchQuery.invoke( null, sql, "id IN ('x','y')" );
        assertEquals(
                "SELECT * FROM t WHERE (a=1 OR b=2) AND id IN ('x','y') ORDER BY name",
                result );
    }

    public void testBuildBatchQueryLowercaseWhere() throws Exception
    {
        // GORelationTransformer uses lowercase keywords
        String sql = "select t2.acc t1,t1.acc t2 from term t1,term t2 where term1_id=t1.id order by t1";
        String result = (String) buildBatchQuery.invoke( null, sql, "t1 IN ('a')" );
        assertEquals(
                "select t2.acc t1,t1.acc t2 from term t1,term t2 where (term1_id=t1.id) AND t1 IN ('a') order by t1",
                result );
    }

    public void testBuildBatchQueryWithLimit() throws Exception
    {
        String sql = "SELECT * FROM t WHERE x=1 LIMIT 10";
        String result = (String) buildBatchQuery.invoke( null, sql, "id IN ('a')" );
        assertEquals( "SELECT * FROM t WHERE (x=1) AND id IN ('a') LIMIT 10", result );
    }

    public void testBuildBatchQueryOrderByAndLimit() throws Exception
    {
        String sql = "SELECT * FROM t WHERE x=1 ORDER BY name LIMIT 5";
        String result = (String) buildBatchQuery.invoke( null, sql, "id IN ('a')" );
        assertEquals( "SELECT * FROM t WHERE (x=1) AND id IN ('a') ORDER BY name LIMIT 5", result );
    }

    public void testBuildBatchQueryNoWhereWithOrderBy() throws Exception
    {
        String sql = "SELECT * FROM t ORDER BY name";
        String result = (String) buildBatchQuery.invoke( null, sql, "id IN ('a')" );
        assertEquals( "SELECT * FROM t WHERE id IN ('a') ORDER BY name", result );
    }
}
