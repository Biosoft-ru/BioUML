package biouml.plugins.simulation_test._test;

import java.io.File;
import java.io.PrintWriter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MakeTestListTest extends TestCase
{
    /** Standart JUnit constructor */
    public MakeTestListTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MakeTestListTest.class.getName());
        suite.addTest(new MakeTestListTest("MakeList"));
        return suite;
    }


    static String testDirectory = "../data_resources/SBML tests/semantic";
    static String outFile = "../data_resources/SBML tests/semantic/testList.txt";

    public void MakeList() throws Exception
    {
        File dir = new File(testDirectory);
        if( dir.exists() && dir.isDirectory() )
        {
            File outf = new File(outFile);
            try (PrintWriter pw = new PrintWriter( outf ))
            {
                pw.write( "CATEGORY All" + "\n" );
                for( File file : dir.listFiles() )
                {
                    if( file.isDirectory() )
                    {
                        String name = file.getName();
                        pw.write( "TEST " + name + "/" + name + "\n" );
                    }
                }
            }
        }
    }
}
