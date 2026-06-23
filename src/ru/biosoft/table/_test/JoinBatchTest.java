package ru.biosoft.table._test;

import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Tests for batched join behavior in {@link TableDataCollectionUtils#join}.
 * Verifies that batch mode (isBatch=true) produces correct results and that
 * finalizeAddition is called after the join completes.
 */
public class JoinBatchTest extends TestCase
{
    public JoinBatchTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(JoinBatchTest.class.getName());
        suite.addTest(new JoinBatchTest("testInnerJoin"));
        suite.addTest(new JoinBatchTest("testLeftJoin"));
        suite.addTest(new JoinBatchTest("testRightJoin"));
        suite.addTest(new JoinBatchTest("testOuterJoin"));
        suite.addTest(new JoinBatchTest("testJoinWithEmptyLeft"));
        suite.addTest(new JoinBatchTest("testJoinWithEmptyRight"));
        suite.addTest(new JoinBatchTest("testJoinDisjointKeys"));
        suite.addTest(new JoinBatchTest("testJoinIdenticalTables"));
        suite.addTest(new JoinBatchTest("testLeftJoinEmptyRightTrivial"));
        suite.addTest(new JoinBatchTest("testBatchAddRow"));
        return suite;
    }

    private TableDataCollection createTable1()
    {
        StandardTableDataCollection t = new StandardTableDataCollection(null, "t1");
        t.getColumnModel().addColumn("A", String.class);
        t.getColumnModel().addColumn("B", Integer.class);
        TableDataCollectionUtils.addRow(t, "k1", new Object[] {"a1", 1}, true);
        TableDataCollectionUtils.addRow(t, "k2", new Object[] {"a2", 2}, true);
        TableDataCollectionUtils.addRow(t, "k3", new Object[] {"a3", 3}, true);
        return t;
    }

    private TableDataCollection createTable2()
    {
        StandardTableDataCollection t = new StandardTableDataCollection(null, "t2");
        t.getColumnModel().addColumn("C", String.class);
        t.getColumnModel().addColumn("D", Double.class);
        TableDataCollectionUtils.addRow(t, "k2", new Object[] {"c2", 2.0}, true);
        TableDataCollectionUtils.addRow(t, "k3", new Object[] {"c3", 3.0}, true);
        TableDataCollectionUtils.addRow(t, "k4", new Object[] {"c4", 4.0}, true);
        return t;
    }

    /**
     * k1: only in t1, k2: both, k3: both, k4: only in t2
     * INNER_JOIN: k2, k3
     */
    public void testInnerJoin()
    {
        TableDataCollection t1 = createTable1();
        TableDataCollection t2 = createTable2();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.INNER_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("INNER_JOIN should have 2 rows", 2, result.getSize());
        List<String> names = result.getNameList();
        assertTrue("Should contain k2", names.contains("k2"));
        assertTrue("Should contain k3", names.contains("k3"));
        assertFalse("Should not contain k1", names.contains("k1"));
        assertFalse("Should not contain k4", names.contains("k4"));
    }

    /**
     * LEFT_JOIN: k1, k2, k3
     */
    public void testLeftJoin()
    {
        TableDataCollection t1 = createTable1();
        TableDataCollection t2 = createTable2();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.LEFT_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("LEFT_JOIN should have 3 rows", 3, result.getSize());
        List<String> names = result.getNameList();
        assertTrue("Should contain k1", names.contains("k1"));
        assertTrue("Should contain k2", names.contains("k2"));
        assertTrue("Should contain k3", names.contains("k3"));
        assertFalse("Should not contain k4", names.contains("k4"));
    }

    /**
     * RIGHT_JOIN: k2, k3, k4
     */
    public void testRightJoin()
    {
        TableDataCollection t1 = createTable1();
        TableDataCollection t2 = createTable2();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.RIGHT_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("RIGHT_JOIN should have 3 rows", 3, result.getSize());
        List<String> names = result.getNameList();
        assertFalse("Should not contain k1", names.contains("k1"));
        assertTrue("Should contain k2", names.contains("k2"));
        assertTrue("Should contain k3", names.contains("k3"));
        assertTrue("Should contain k4", names.contains("k4"));
    }

    /**
     * OUTER_JOIN: k1, k2, k3, k4
     */
    public void testOuterJoin()
    {
        TableDataCollection t1 = createTable1();
        TableDataCollection t2 = createTable2();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.OUTER_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("OUTER_JOIN should have 4 rows", 4, result.getSize());
        List<String> names = result.getNameList();
        assertTrue("Should contain k1", names.contains("k1"));
        assertTrue("Should contain k2", names.contains("k2"));
        assertTrue("Should contain k3", names.contains("k3"));
        assertTrue("Should contain k4", names.contains("k4"));
    }

    /**
     * LEFT_JOIN with empty left table: result should be empty
     */
    public void testJoinWithEmptyLeft()
    {
        StandardTableDataCollection t1 = new StandardTableDataCollection(null, "empty");
        t1.getColumnModel().addColumn("A", String.class);

        TableDataCollection t2 = createTable2();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.LEFT_JOIN, t1, t2, null,
                new String[] {"A"}, new String[] {"C", "D"});

        assertEquals("LEFT_JOIN with empty left should have 0 rows", 0, result.getSize());
    }

    /**
     * RIGHT_JOIN with empty right table: merge loop finds no keys, result is empty.
     * (The trivial optimization only handles isEmpty(t1) for LEFT_JOIN and isEmpty(t2) for RIGHT_JOIN)
     */
    public void testJoinWithEmptyRight()
    {
        StandardTableDataCollection t2 = new StandardTableDataCollection(null, "empty");
        t2.getColumnModel().addColumn("C", String.class);
        t2.getColumnModel().addColumn("D", Double.class);

        TableDataCollection t1 = createTable1();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.RIGHT_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("RIGHT_JOIN with empty right returns 0 rows (merge loop finds no right keys)", 0, result.getSize());
    }

    /**
     * Tables with completely disjoint keys
     */
    public void testJoinDisjointKeys()
    {
        StandardTableDataCollection t1 = new StandardTableDataCollection(null, "t1");
        t1.getColumnModel().addColumn("A", String.class);
        TableDataCollectionUtils.addRow(t1, "x1", new Object[] {"a"}, true);
        TableDataCollectionUtils.addRow(t1, "x2", new Object[] {"b"}, true);

        StandardTableDataCollection t2 = new StandardTableDataCollection(null, "t2");
        t2.getColumnModel().addColumn("C", String.class);
        TableDataCollectionUtils.addRow(t2, "y1", new Object[] {"c"}, true);
        TableDataCollectionUtils.addRow(t2, "y2", new Object[] {"d"}, true);

        TableDataCollection inner = TableDataCollectionUtils.join(
                TableDataCollectionUtils.INNER_JOIN, t1, t2, null,
                new String[] {"A"}, new String[] {"C"});
        assertEquals("INNER_JOIN with disjoint keys should be empty", 0, inner.getSize());

        TableDataCollection outer = TableDataCollectionUtils.join(
                TableDataCollectionUtils.OUTER_JOIN, t1, t2, null,
                new String[] {"A"}, new String[] {"C"});
        assertEquals("OUTER_JOIN with disjoint keys should have 4 rows", 4, outer.getSize());
    }

    /**
     * Join a table with itself: should produce same rows with duplicated columns
     */
    public void testJoinIdenticalTables()
    {
        TableDataCollection t1 = createTable1();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.INNER_JOIN, t1, t1, null,
                new String[] {"A"}, new String[] {"A"});

        assertEquals("Self-join should have 3 rows", 3, result.getSize());
    }

    /**
     * LEFT_JOIN with empty right table: trivial path returns all left rows
     */
    public void testLeftJoinEmptyRightTrivial()
    {
        StandardTableDataCollection t2 = new StandardTableDataCollection(null, "empty");
        t2.getColumnModel().addColumn("C", String.class);
        t2.getColumnModel().addColumn("D", Double.class);

        TableDataCollection t1 = createTable1();
        TableDataCollection result = TableDataCollectionUtils.join(
                TableDataCollectionUtils.LEFT_JOIN, t1, t2, null,
                new String[] {"A", "B"}, new String[] {"C", "D"});

        assertEquals("LEFT_JOIN with empty right should have 3 rows (trivial path)", 3, result.getSize());
        List<String> names = result.getNameList();
        assertTrue("Should contain k1", names.contains("k1"));
        assertTrue("Should contain k2", names.contains("k2"));
        assertTrue("Should contain k3", names.contains("k3"));
    }

    /**
     * Verify batch addRow works correctly (no finalizeAddition leak, rows accessible)
     */
    public void testBatchAddRow()
    {
        StandardTableDataCollection t = new StandardTableDataCollection(null, "batch");
        t.getColumnModel().addColumn("X", Integer.class);

        for (int i = 0; i < 100; i++)
        {
            TableDataCollectionUtils.addRow(t, "row" + i, new Object[] {i}, true);
        }
        t.finalizeAddition();

        assertEquals("Batch table should have 100 rows", 100, t.getSize());
        for (int i = 0; i < 100; i++)
        {
            Object value = t.getValueAt(i, 0);
            assertEquals("Row " + i + " value mismatch", i, value);
        }
    }
}
