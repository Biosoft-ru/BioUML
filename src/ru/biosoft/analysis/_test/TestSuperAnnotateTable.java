package ru.biosoft.analysis._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysis.SuperAnnotateTable;
import ru.biosoft.analysis.SuperAnnotateTable.SourceTable;
import ru.biosoft.analysis.SuperAnnotateTable.SuperAnnotateTableParameters;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestSuperAnnotateTable extends AbstractBioUMLTest
{
    private TableDataCollection inputTable;
    private TableDataCollection sourceTable1;
    private TableDataCollection sourceTable2;

    public TestSuperAnnotateTable(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestSuperAnnotateTable.class.getName() );
        suite.addTest( new TestSuperAnnotateTable( "testString" ) );
        suite.addTest( new TestSuperAnnotateTable( "testStringDouble" ) );
        suite.addTest( new TestSuperAnnotateTable( "testStringDoubleSingleId" ) );
        suite.addTest( new TestSuperAnnotateTable( "testStringSet" ) );
        suite.addTest( new TestSuperAnnotateTable( "testStringSetInt" ) );
        suite.addTest( new TestSuperAnnotateTable( "testOneTableTwiceTwoColumns" ) );
        suite.addTest( new TestSuperAnnotateTable( "testOneTableTwiceOneColumn" ) );
        suite.addTest( new TestSuperAnnotateTable( "testTwoTables" ) );
        suite.addTest( new TestSuperAnnotateTable( "testSelf" ) );
        return suite;
    }

    public void testSelf() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID", new STWrapper( inputTable, new String[] {"Annotation"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 5, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (input)", String.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "Annotation 1"} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "Annotation 2"} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "Annotation 3"} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "Annotation 0"} );
    }

    public void testString() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID1", new STWrapper( sourceTable1, new String[] {"Annotation"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 5, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "S101 element,S102 element"} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "S102 element,S103 element"} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "S103 element"} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "S001 element"} );
    }

    public void testStringDouble() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID1", new STWrapper( sourceTable1, new String[] {"Annotation", "Type"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );
        checkColumn( cm, "Type", String.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "S101 element,S102 element", "1.0,2.0"} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "S102 element,S103 element", "2.0,3.0"} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "S103 element", "3.0"} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "S001 element", "0.0"} );
    }

    public void testStringDoubleSingleId() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID3", new STWrapper( sourceTable1, new String[] {"Annotation", "Type"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );
        checkColumn( cm, "Type", Double.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "S101 element", 1.0} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "S102 element", 2.0} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "", null} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "S102 element", 2.0} );
    }

    public void testStringSet() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID2", new STWrapper( sourceTable2, new String[] {"Annotation"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 5, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st2)", StringSet.class );

        checkRow( result, "I01", "Row#1", new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1",
                createSet( createSet( "02", "S202 element" ).toString(), createSet( "03", "S203 element" ).toString() )} );
        checkRow( result, "I02", "Row#2", new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2",
                createSet( createSet( "03", "S203 element" ).toString(), createSet( "01", "S201 element" ).toString() )} );
        checkRow( result, "I03", "Row#3",
                new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", createSet( createSet( "02", "S202 element" ).toString() )} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", createSet()} );
    }

    public void testStringSetInt() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID2", new STWrapper( sourceTable2, new String[] {"Annotation", "Type"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st2)", StringSet.class );
        checkColumn( cm, "Type", StringSet.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1",
                        createSet( createSet( "02", "S202 element" ).toString(), createSet( "03", "S203 element" ).toString() ),
                        createSet( "2", "1" )} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2",
                        createSet( createSet( "03", "S203 element" ).toString(), createSet( "01", "S201 element" ).toString() ),
                        createSet( "1", "1" )} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3",
                createSet( createSet( "02", "S202 element" ).toString() ), createSet( "2" )} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", createSet(), createSet()} );
    }

    public void testOneTableTwiceTwoColumns() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID1", new STWrapper( sourceTable1, new String[] {"Annotation"} ),
                new STWrapper( sourceTable1, new String[] {"Type"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );
        checkColumn( cm, "Type", String.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "S101 element,S102 element", "1.0,2.0"} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "S102 element,S103 element", "2.0,3.0"} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "S103 element", "3.0"} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "S001 element", "0.0"} );
    }

    public void testOneTableTwiceOneColumn() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID1", new STWrapper( sourceTable1, new String[] {"Annotation"} ),
                new STWrapper( sourceTable1, new String[] {"Annotation"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );
        checkColumn( cm, "Annotation (st1)_1", String.class );

        checkRow( result, "I01", "Row#1", new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1",
                "S101 element,S102 element", "S101 element,S102 element"} );
        checkRow( result, "I02", "Row#2", new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2",
                "S102 element,S103 element", "S102 element,S103 element"} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "S103 element", "S103 element"} );
        checkRow( result, "I04", "Row#4", new Object[] {"S001", createSet(), "S102", "Annotation 0", "S001 element", "S001 element"} );
    }

    public void testTwoTables() throws Exception
    {
        SuperAnnotateTable sat = initAnalysis( "ID1", new STWrapper( sourceTable1, new String[] {"Annotation"} ),
                new STWrapper( sourceTable2, new String[] {"Annotation"} ) );

        TableDataCollection result = sat.justAnalyzeAndPut();
        assertEquals( "Incorrect size", 4, result.getSize() );
        ColumnModel cm = result.getColumnModel();
        assertEquals( "Incorrect column number", 6, cm.getColumnCount() );

        checkColumn( cm, "ID1", String.class );
        checkColumn( cm, "ID2", StringSet.class );
        checkColumn( cm, "ID3", String.class );
        checkColumn( cm, "Annotation", String.class );
        checkColumn( cm, "Annotation (st1)", String.class );
        checkColumn( cm, "Annotation (st2)", String.class );

        checkRow( result, "I01", "Row#1",
                new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1", "S101 element,S102 element", ""} );
        checkRow( result, "I02", "Row#2",
                new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2", "S102 element,S103 element", ""} );
        checkRow( result, "I03", "Row#3", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3", "S103 element", ""} );
        checkRow( result, "I04", "Row#4",
                new Object[] {"S001", createSet(), "S102", "Annotation 0", "S001 element", createSet( "00", "S001 element" ).toString()} );
    }

    private void checkColumn(ColumnModel cm, String name, Class<?> type)
    {
        assertTrue( "Missed column '" + name + "'", cm.hasColumn( name ) );
        assertEquals( "Incorrect column type '" + name + "'", type, cm.getColumn( name ).getType().getType() );
    }

    private void checkRow(@Nonnull TableDataCollection result, @Nonnull String rdeName, @Nonnull String prefix,
            @Nonnull Object[] expRowValues) throws Exception
    {
        RowDataElement rde = result.get( rdeName );
        assertNotNull( prefix + " is absent", rde );
        assertArrayEquals( prefix + " incorrect values", expRowValues, rde.getValues() );
    }

    private SuperAnnotateTable initAnalysis(String idColumn, STWrapper ... sourceTables)
    {
        SuperAnnotateTable sat = AnalysisMethodRegistry.getAnalysisMethod( SuperAnnotateTable.class );
        SuperAnnotateTableParameters parameters = sat.getParameters();
        parameters.setInputTable( inputTable.getCompletePath() );
        parameters.setIdColumn( idColumn );

        List<SourceTable> list = new ArrayList<>();
        for( STWrapper sourceTable : sourceTables )
        {
            SourceTable st = new SourceTable();
            st.setTablePath( sourceTable.sourceTable.getCompletePath() );
            st.setAnnotationColumns( sourceTable.columns );
            list.add( st );
        }
        parameters.setAnnotationTables( list.toArray( new SourceTable[0] ) );

        return sat;
    }

    private static class STWrapper
    {
        TableDataCollection sourceTable;
        String[] columns;
        public STWrapper(TableDataCollection sourceTable, String[] columns)
        {
            this.sourceTable = sourceTable;
            this.columns = columns;
        }
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        FolderVectorCollection fvc = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( fvc );

        inputTable = TableDataCollectionUtils.createTableDataCollection( fvc, "input" );
        fvc.put( inputTable );
        ReferenceTypeRegistry.setCollectionReferenceType( inputTable, ReferenceTypeRegistry.getDefaultReferenceType() );
        inputTable.getColumnModel().addColumn( "ID1", String.class );
        inputTable.getColumnModel().addColumn( "ID2", StringSet.class );
        inputTable.getColumnModel().addColumn( "ID3", String.class );
        inputTable.getColumnModel().addColumn( "Annotation", String.class );
        TableDataCollectionUtils.addRow( inputTable, "I01", new Object[] {"S101, S102", createSet( "S202", "S203" ), "S101", "Annotation 1"} );
        TableDataCollectionUtils.addRow( inputTable, "I02", new Object[] {"S102,S103", createSet( "S203", "S201" ), "S102", "Annotation 2"} );
        TableDataCollectionUtils.addRow( inputTable, "I03", new Object[] {"S103", createSet( "S202" ), "", "Annotation 3"} );
        TableDataCollectionUtils.addRow( inputTable, "I04", new Object[] {"S001", createSet(), "S102", "Annotation 0"} );

        sourceTable1 = TableDataCollectionUtils.createTableDataCollection( fvc, "st1" );
        fvc.put( sourceTable1 );
        ReferenceTypeRegistry.setCollectionReferenceType( sourceTable1, ReferenceTypeRegistry.getDefaultReferenceType() );
        sourceTable1.getColumnModel().addColumn( "Type", Double.class );
        sourceTable1.getColumnModel().addColumn( "Annotation", String.class );
        TableDataCollectionUtils.addRow( sourceTable1, "S101", new Object[] {"1.0", "S101 element"} );
        TableDataCollectionUtils.addRow( sourceTable1, "S102", new Object[] {"2.0", "S102 element"} );
        TableDataCollectionUtils.addRow( sourceTable1, "S103", new Object[] {"3.0", "S103 element"} );
        TableDataCollectionUtils.addRow( sourceTable1, "S001", new Object[] {"0.0", "S001 element"} );

        sourceTable2 = TableDataCollectionUtils.createTableDataCollection( fvc, "st2" );
        fvc.put( sourceTable2 );
        ReferenceTypeRegistry.setCollectionReferenceType( sourceTable2, ReferenceTypeRegistry.getDefaultReferenceType() );
        sourceTable2.getColumnModel().addColumn( "Type", Integer.class );
        sourceTable2.getColumnModel().addColumn( "Annotation", StringSet.class );
        TableDataCollectionUtils.addRow( sourceTable2, "S201", new Object[] {"1", createSet( "01", "S201 element" )} );
        TableDataCollectionUtils.addRow( sourceTable2, "S202", new Object[] {"2", createSet( "02", "S202 element" )} );
        TableDataCollectionUtils.addRow( sourceTable2, "S203", new Object[] {"1", createSet( "03", "S203 element" )} );
        TableDataCollectionUtils.addRow( sourceTable2, "S001", new Object[] {"0", createSet( "00", "S001 element" )} );
    }

    private StringSet createSet(String ... strings)
    {
        return new StringSet( Arrays.asList( strings ) );
    }
}
