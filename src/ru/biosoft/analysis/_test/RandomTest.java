package ru.biosoft.analysis._test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import com.developmentontheedge.application.ApplicationUtils;

import junit.framework.TestCase;
import junit.framework.TestSuite;


public class RandomTest extends TestCase
{
    public RandomTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(RandomTest.class.getName());
        suite.addTest(new RandomTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public static void test() throws Exception
    {
        System.out.println(26.7+5.34);
//        double[] fromRandomOrg = getRandomDataFromRandomOrg();
//        double[] randomFromJava = getRandomDataFromJava();
//        System.out.println("From random.org: "+ Stat.seriesCrit(fromRandomOrg));
//        System.out.println("From java: "+ Stat.seriesCrit((randomFromJava)));
    }


    public double[] getRandomDataFromRandomOrg() throws IOException
    {
        try(BufferedReader br = ApplicationUtils.asciiReader( "C:/random_test.txt"))
        {
            double[] result = new double[1000];
            for(int i=0; i<1000;i++ )
            {
                String val = br.readLine();
                if( val == null )
                    break;
                result[i] = Integer.parseInt(val);
            }
            return result;
        }
    }
    
    public double[] getRandomDataFromJava()
    {
        Random random = new Random();
        double[] result = new double[50000];
        for (int i =0; i<50000; i++)
        {
            result[i] = random.nextInt(1000);
        }
        return result;
    }
}
