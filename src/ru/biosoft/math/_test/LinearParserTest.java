
package ru.biosoft.math._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.parser.Parser;


public class LinearParserTest extends TestCase
{
    public LinearParserTest(String name)
    {
        super(name);
        File configFile = new File( "./ru/biosoft/math/_test/test.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(LinearParserTest.class.getName());
        return suite;
    }

    static StringBuffer buf = new StringBuffer();
    static Parser parser = new Parser();

    public static void main(String[] args) throws Exception
    {

        if( args != null && args.length > 0 )
        {
            // concat all expressions
            for(int i=0; i<args.length; i++)
                buf.append(args[i]);

            parse();
            return;
        }

        System.out.println("Reading from standard input...");
        while(true)
        {
            char c = (char)System.in.read();
            buf.append(c);

            if( c == '\r' || c == '\n')
                 parse();
        }
    }

    public static void parse()
    {
        try
        {
            int status = parser.parse(buf.toString());

            AstStart start = parser.getStartNode();
            String[] result =  (new LinearFormatter()).format(start);
            System.out.println("result=" + result[1]);
            start.dump("");
        }
        catch(Exception e)
        {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        buf = new StringBuffer();
    }

}


