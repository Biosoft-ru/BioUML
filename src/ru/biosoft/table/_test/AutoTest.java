package ru.biosoft.table._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());
        suite.addTestSuite(TableModelTest.class);
        suite.addTest(StandardTableSimpleTest.suite());
        suite.addTest(CalculatedTableTest.suite());
        suite.addTest(FileTableTest.suite());
        suite.addTest(ImportTableTest.suite());
        suite.addTest(SqlQueryTableTest.suite());
        suite.addTest(ExcelLoadTest.suite());
        suite.addTestSuite( TableDataCollectionUtilsTest.class );
        suite.addTestSuite( TestSqlTableCreateRaceCondition.class );
        return suite;
    }
}