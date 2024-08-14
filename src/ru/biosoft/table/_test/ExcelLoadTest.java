package ru.biosoft.table._test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ru.biosoft.table.XLSConverterFactory;
import ru.biosoft.table.XLSandXLSXConverters;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ExcelLoadTest extends TestCase
{
    public ExcelLoadTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ExcelLoadTest.class.getName());

        suite.addTest(new ExcelLoadTest("testLoadXLS"));
        suite.addTest(new ExcelLoadTest("testLoadXLSX"));
        suite.addTest( new ExcelLoadTest( "testLoadXLSNew" ) );
        suite.addTest( new ExcelLoadTest( "testLoadXLSXNew" ) );
        suite.addTest( new ExcelLoadTest( "testLoadXLSXNewEmptyCell" ) );

        return suite;
    }

    public void testLoadXLS() throws Exception
    {
        String result = convert( "ru/biosoft/table/_test/test.xls", false );
        compare( "XLS file load", "ru/biosoft/table/_test/test.txt", result );
    }

    public void testLoadXLSNew() throws Exception
    {
        String result = convert( "ru/biosoft/table/_test/test.xls", true );
        compare( "XLS file load", "ru/biosoft/table/_test/test.txt", result );
    }

    public void testLoadXLSX() throws Exception
    {
        String result = convert( "ru/biosoft/table/_test/test.xlsx", false );
        compare( "XLSX file load", "ru/biosoft/table/_test/test.txt", result );
    }

    public void testLoadXLSXNew() throws Exception
    {
        String result = convert( "ru/biosoft/table/_test/test.xls", true );
        compare( "XLSX file load", "ru/biosoft/table/_test/test.txt", result );
    }

    public void testLoadXLSXNewEmptyCell() throws Exception
    {
        String result = convert( "ru/biosoft/table/_test/test_new.xlsx", true );
        compare( "XLSX file load", "ru/biosoft/table/_test/test_new.txt", result );
    }

    private void compare(String errorMessage, String expectedFilePath, String result) throws IOException
    {
        try( ByteArrayInputStream bais = new ByteArrayInputStream( result.getBytes() );
                FileInputStream fis = new FileInputStream( new File( expectedFilePath ) ) )
        {
            assertStreamsEqual( errorMessage, fis, bais );
        }
    }

    private String convert(String filePath, boolean tryNewFeatures) throws Exception
    {
        File file = new File( filePath );
        XLSandXLSXConverters converter = XLSConverterFactory.getXLSConverter( file, tryNewFeatures );
        converter.process();
        String result = converter.getSheetData( 0 );
        return result;
    }

    public void assertStreamsEqual(String message, InputStream expected, InputStream actual) throws IOException
    {
        BufferedReader input1 = new BufferedReader( new InputStreamReader( expected ) );
        BufferedReader input2 = new BufferedReader( new InputStreamReader( actual ) );
        int lineNum = 0;
        while( true )
        {
            lineNum++;
            String a = input1.readLine();
            if( a != null )
            {
                a = a.trim();
            }
            String b = input2.readLine();
            if( b != null )
            {
                b = b.trim();
            }
            if( a == null )
            {
                if( b == null )
                {
                    break;
                }
                else
                {
                    fail("Different line count");
                }
            }
            else
            {
                if( b == null )
                {
                    fail("Different line count");
                }
            }
            assertEquals(message + ": line#" + lineNum + " doesn't match", a, b);
        }
    }
}
