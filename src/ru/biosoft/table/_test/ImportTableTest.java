package ru.biosoft.table._test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.ImportProperties;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

public class ImportTableTest extends AbstractBioUMLTest
{
    public static final String repositoryPath = "../data/test/ru/biosoft/table";
    public static final DataElementPath dePath = DataElementPath.create("data/csv");

    public ImportTableTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ImportTableTest.class.getName());

        suite.addTest(new ImportTableTest("test1"));
        suite.addTest(new ImportTableTest("test2"));
        suite.addTest( new ImportTableTest( "testQuotes" ) );
        suite.addTest( new ImportTableTest( "testEmptyCells1" ) );
        suite.addTest( new ImportTableTest( "testEmptyCells2" ) );

        return suite;
    }


    public void test1() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection(null, "test1");
        CollectionFactory.createRepository(repositoryPath);
        FileDataElement file = (FileDataElement)dePath.getChildPath("test1.csv").optDataElement();
        assertNotNull("Import source not found", file);
        TableCSVImporter importer = new TableCSVImporter();
        NullImportProperties properties = (NullImportProperties)importer.getProperties(table,file.getFile(),"");
        properties.setColumnForID(TableCSVImporter.GENERATE_UNIQUE_ID);
        TableCSVImporter.fillTable(file.getFile(), table, null, properties);
        assertEquals( "Incorrect column count", 3, table.getColumnModel().getColumnCount() );
        assertEquals( "Incorrect column type", Integer.class, table.getColumnModel().getColumn( 0 ).getValueClass() );
        assertEquals( "Incorrect column type", Double.class, table.getColumnModel().getColumn( 1 ).getValueClass() );
        assertEquals( "Incorrect column type", String.class, table.getColumnModel().getColumn( 2 ).getValueClass() );
        assertEquals( "Incorrect row count", 3, table.getSize() );
    }

    public void test2() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection( null, "test2" );
        CollectionFactory.createRepository( repositoryPath );
        FileDataElement file = (FileDataElement)dePath.getChildPath( "test2.csv" ).optDataElement();
        assertNotNull( "Import source not found", file );
        TableCSVImporter importer = new TableCSVImporter();
        ImportProperties properties = (ImportProperties)importer.getProperties( table, file.getFile(), "" );
        properties.setColumnForID( TableCSVImporter.GENERATE_UNIQUE_ID );
        properties.setDataRow( 1 );
        properties.setHeaderRow( 1 );
        TableCSVImporter.fillTable( file.getFile(), table, null, properties );
        assertEquals( "Incorrect column count", 5, table.getColumnModel().getColumnCount() );
        assertEquals( "Incorrect column type", Integer.class, table.getColumnModel().getColumn( 0 ).getValueClass() );
        assertEquals( "Incorrect column type", Double.class, table.getColumnModel().getColumn( 1 ).getValueClass() );
        assertEquals( "Incorrect column type", String.class, table.getColumnModel().getColumn( 2 ).getValueClass() );
        assertEquals( "Incorrect column type", Double.class, table.getColumnModel().getColumn( 3 ).getValueClass() );
        assertEquals( "Incorrect column type", String.class, table.getColumnModel().getColumn( 4 ).getValueClass() );
        assertEquals( "Incorrect row count", 3, table.getSize() );
    }

    public void testEmptyCells1() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection( null, "test4" );
        CollectionFactory.createRepository( repositoryPath );
        FileDataElement file = (FileDataElement)dePath.getChildPath( "test4.csv" ).optDataElement();
        assertNotNull( "Import source not found", file );
        importTable( table, file, 0 );
        checkTable( table );
    }

    public void testEmptyCells2() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection( null, "test5" );
        CollectionFactory.createRepository( repositoryPath );
        FileDataElement file = (FileDataElement)dePath.getChildPath( "test5.csv" ).optDataElement();
        assertNotNull( "Import source not found", file );
        importTable( table, file, 1 );
        checkTable( table );
    }

    private void checkTable(TableDataCollection table)
    {
        ColumnModel columnModel = table.getColumnModel();
        assertEquals( "Incorrect column count", 2, columnModel.getColumnCount() );
        Class<?>[] columnClasses = new Class<?>[] {String.class, String.class};
        String[] columns = new String[] {"Description", "Name"};
        for( int i = 0; i < 2; i++ )
        {
            TableColumn column = columnModel.getColumn( i );
            assertEquals( "Incorrect column type", columnClasses[i], column.getValueClass() );
            assertEquals( "Incorrect column name", columns[i], column.getName() );
        }
        assertEquals( "Incorrect row count", 4, table.getSize() );

        List<Object[]> expectedValues = new ArrayList<>();
        expectedValues.add( new String[] {"i", "i"} );
        expectedValues.add( new String[] {"", "ii"} );
        expectedValues.add( new String[] {"iii", ""} );
        expectedValues.add( new String[] {"", "iv"} );
        for( RowDataElement rde : table )
            checkRow( rde, expectedValues );
    }

    private void importTable(TableDataCollection table, FileDataElement file, int delimiterType) throws IOException
    {
        TableCSVImporter importer = new TableCSVImporter();
        ImportProperties properties = (ImportProperties)importer.getProperties( table, file.getFile(), "" );
        properties.setDelimiterType( delimiterType );
        properties.setDataRow( 2 );
        properties.setHeaderRow( 1 );
        properties.setProcessQuotes( true );
        properties.setColumnForID( "ID" );
        TableCSVImporter.fillTable( file.getFile(), table, null, properties );
    }

    public void testQuotes() throws Exception
    {
        TableDataCollection table = new StandardTableDataCollection( null, "test3" );
        CollectionFactory.createRepository( repositoryPath );
        FileDataElement file = (FileDataElement)dePath.getChildPath( "test3.csv" ).optDataElement();
        assertNotNull( "Import source not found", file );
        TableCSVImporter importer = new TableCSVImporter();
        ImportProperties properties = (ImportProperties)importer.getProperties( table, file.getFile(), "" );
        properties.setDelimiterType( 2 );
        properties.setDataRow( 2 );
        properties.setHeaderRow( 1 );
        properties.setProcessQuotes( true );
        properties.setColumnForID( TableCSVImporter.GENERATE_UNIQUE_ID );
        TableCSVImporter.fillTable( file.getFile(), table, null, properties );
        ColumnModel columnModel = table.getColumnModel();
        assertEquals( "Incorrect column count", 6, columnModel.getColumnCount() );
        Class<?>[] columnClasses = new Class<?>[] {String.class, String.class, String.class, String.class, Double.class, String.class};
        String[] columns = new String[] {"Family", "Name", "Address", "City", "Index", "Just String"};
        for( int i = 0; i < 6; i++ )
        {
            TableColumn column = columnModel.getColumn( i );
            assertEquals( "Incorrect column type", columnClasses[i], column.getValueClass() );
            assertEquals( "Incorrect column name", columns[i], column.getName() );
        }
        assertEquals( "Incorrect row count", 9, table.getSize() );

        List<Object[]> expectedValues = new ArrayList<>();
        expectedValues.add( new Object[] {"Ivanov", "Ivan,II", "Lenina 20", "Moscow", Double.valueOf( "08075" ), "1"} );
        expectedValues.add( new Object[] {"Ivanov", "Ivan,III", "Lenina 20", "Moscow", Double.valueOf( "08075" ), "1,2"} );
        expectedValues.add( new Object[] {"Ivanov", "Ivan", "Lenina 20", "Moscow", Double.valueOf( "08075" ), "1/3"} );
        expectedValues.add( new Object[] {"Tyler", "John", "110 terrace", " \n\"PA", Double.valueOf( "20121" ), "1.24\ntt"} );
        expectedValues.add( new Object[] {"Petrov \n\"Cool\"", " Petr", "120 Hambling St.", " NJ", Double.valueOf( "08075" ), " 1,24"} );
        expectedValues.add( new Object[] {"Smirnov", "Vasiliy", "7452 Street \"Near \nthe Square\" road", " York",
                Double.valueOf( " 91234" ), " 3-01"} );
        expectedValues.add( new Object[] {"", "Michael", "", "St.Petersburg", Double.valueOf( " 00123" ), " 03-01"} );
        expectedValues.add( new Object[] {"John \"Black head\", Clod", "Rock", "", " Miami", Double.valueOf( "00111" ), " 0000"} );
        expectedValues.add( new Object[] {"Sergey", "", "", null, Double.NaN, null} );
        for( RowDataElement rde : table )
            checkRow( rde, expectedValues );
    }
    private void checkRow(RowDataElement rde, List<Object[]> expectedValues)
    {
        int index = Integer.parseInt( rde.getName() );
        if( index <= 0 || index > expectedValues.size() )
            throw new IllegalArgumentException( "Unexpected row with id='" + rde.getName() + "'" );
        Object[] expected = expectedValues.get( index - 1 );
        assertArrayEquals( "Incorrect row parsing (row#='" + index + "')", expected, rde.getValues() );
    }
}
