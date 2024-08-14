package biouml.plugins.stochastic._test;

import java.util.Date;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.TestUtils;

import biouml.plugins.stochastic.Stochastic;

public class StochasticTest
{
    public static void main(String ... args) throws Exception
    {
       Stochastic stochastic = new Stochastic(new Date());
       stochastic.setSeed(1);
       
       Stochastic stochastic2 = new Stochastic(new Date());
       stochastic2.setSeed(1);
       
//       System.out.println(stochastic.getUniform());
////       System.out.println(stochastic.getPoisson(1));
//       System.out.println(stochastic.getUniform());
       int randomCount = 100;
       double[] arg1 = new double[randomCount];
       double[] arg2 = new double[randomCount];
       
       for (int i=0; i<randomCount; i++)
       {
           arg1[i] = stochastic.getUniform();
           arg2[i] = stochastic2.getUniform();
       }
       
       int intervals = 10;
       
       long[] count1 = new long[intervals];
       long[] count2 = new long[intervals];
       long[] count12 = new long[intervals];
       
        for( int i = 0; i < intervals; i++ )
        {
            for( int j = 0; j < randomCount; j++ )
            {
                boolean firstFit = arg1[j] < (double)(i+1) / intervals && arg1[j] > (double)i / intervals;
                boolean secondFit = arg2[j] < (double)(i+1) / intervals && arg2[j] > (double)i / intervals;
                if( firstFit )
                    count1[i]++;

                if( secondFit )
                    count2[i]++;
                
                if (firstFit && secondFit)
                    count12[i]++;
            }
       }
        
        long v1 =0, v2 = 0;
        for (int i=0; i<count1.length;i++)
        {
            v1+= count1[i];
            v2 += count2[i];
        }
        System.out.println(v1+"___"+v2);
        
        
        double[] countMultiplied = new double[intervals];
        for( int i = 0; i < intervals; i++ )
            countMultiplied[i] = count1[i]* count2[i];
        
       ChiSquareTest test = new ChiSquareTest();
       System.out.println(TestUtils.chiSquare(countMultiplied, count12));
       System.out.println(test.chiSquareTest(countMultiplied, count12, 0.5));
       
//       MersenneTwister twister = new MersenneTwister(1);
//       System.out.println(twister.raw());
//       System.out.println(twister.nextDouble());
//       System.out.println(twister.raw());
    }

}
