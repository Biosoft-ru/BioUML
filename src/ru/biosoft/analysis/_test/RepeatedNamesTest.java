package ru.biosoft.analysis._test;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;

public class RepeatedNamesTest extends TestCase
{
    public RepeatedNamesTest(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( RepeatedNamesTest.class.getName() );
        suite.addTest( new RepeatedNamesTest( "test" ) );
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        String repositoryPath = "../data_resources";
        CollectionFactory.createRepository( repositoryPath );

        try (BufferedReader br = ApplicationUtils.asciiReader( "E:/shadrin/normalized.txt" );
                BufferedWriter bw = ApplicationUtils.asciiWriter( "E:/shadrin/normalized_2.txt" ))
        {
            String[] array = StreamEx.ofLines( br ).skip( 1 ).map( line -> line.substring( 0, line.indexOf( "\t" ) ) ).sorted()
                    .toArray( String[]::new );

            int ind = 1;
            for( int i = 1; i < array.length; i++ )
            {
                boolean isRepeated = false;
                ind = 2;
                int j = i - 1;
                String val = array[j];
                while( array[i].equalsIgnoreCase( val ) && i < array.length )
                {
                    array[i++] += "_" + ind++;
                    isRepeated = true;
                }

                if( isRepeated )
                    array[j] += "_1";
            }

            for( String val : array )
            {
                bw.write( val + '\n' );
            }
        }
    }
}
