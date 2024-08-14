package biouml.plugins.enrichment._test;

import java.io.File;
import java.io.PrintWriter;
import java.util.Random;

import biouml.plugins.enrichment.GroupInfo;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GroupInfoTest extends TestCase
{
    public GroupInfoTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(GroupInfoTest.class.getName());
        suite.addTest(new GroupInfoTest("test"));
        return suite;
    }
    
    public void test() throws Exception
    {
        int nGroups = 100000;
        int maxRank = 2000;
        int size = 400;
        Random rnd = new Random();
        try (PrintWriter writer = new PrintWriter( new File( "GroupInfo.2000.400.txt" ), "UTF-8" ))
        {
            System.out.println( GroupInfo.getAverageES( maxRank, size ) );
            for( int i = 0; i < nGroups; i++ )
            {
                GroupInfo group = new GroupInfo( maxRank );
                do
                {
                    group.put( rnd.nextInt( maxRank ) );
                }
                while( group.getSize() < size );
                group.calculateScore();
                writer.println( Math.abs( group.getES() ) );
            }
        }
    }
    
    /**
     * @throws Exception
     */
    public void test1() throws Exception
    {
        int maxRank = 2000;
        int size = 200;
        int nGroups = 10000;
        Random rnd = new Random();
        
        // AvgScore = 0.792/Math.pow((double)size*(maxRank-size)/maxRank, 0.486)
        try( PrintWriter writer = new PrintWriter( new File( "GroupInfo.Rand.txt" ), "UTF-8" ) )
        {
            //writer.println("MaxRank\tSize\tESAvg(10000)\tESAvg(1000)\tP(1000)\tESAvg(calc)\tP(calc)");

            /*        for(maxRank = 7; maxRank < 80; maxRank++)
            {
            writer.print(maxRank);
            for(size = 3; size<=6; size++)
            {
                double ESsum = 0;
                for(int i=0; i<nGroups; i++)
                {
                    GroupInfo group = new GroupInfo(maxRank);
                    do
                    {
                        group.put(rnd.nextInt(maxRank));
                    }
                    while( group.getSize() < size );
                    group.calculateScore();
                    ESsum+=Math.abs(group.getES());
                }
                double ESavg = ESsum/nGroups;
                //double ESavgCalc = GroupInfo.getAverageES(maxRank, size);
                double ESavgCalc = 0.779/Math.pow((double)size*(maxRank-size)/maxRank, 0.483);
                writer.print("\t"+String.format("%.4f", ESavg)+":"+String.format("%.4f", ESavgCalc)+":"+String.format("%2.2f%%", Math.abs(ESavgCalc-ESavg)/ESavg*100));
            }
            writer.println();
            writer.flush();
            }
            writer.close();*/

            for( int trial = 0; trial < 200; trial++ )
            {
                maxRank = (int) ( rnd.nextDouble() * rnd.nextDouble() * rnd.nextDouble() * 999 ) + 2;
                size = rnd.nextInt( maxRank - 1 ) + 1;
                double ESsum = 0;
                for( int i = 0; i < nGroups; i++ )
                {
                    GroupInfo group = new GroupInfo( maxRank );
                    do
                    {
                        group.put( rnd.nextInt( maxRank ) );
                    }
                    while( group.getSize() < size );
                    group.calculateScore();
                    ESsum += Math.abs( group.getES() );
                }
                double ESavg = ESsum / nGroups;
                double ESavgCalc = GroupInfo.getAverageES( maxRank, size );
                writer.println( maxRank + "\t" + size + "\t" + String.format( "%.4f", ESavg ) + "\t" + String.format( "%.4f", ESavgCalc )
                        + "\t" + String.format( "%2.2f%%", Math.abs( ESavgCalc - ESavg ) / ESavg * 100 ) );
                writer.flush();
            }
            for( int trial = 0; trial < 200; trial++ )
            {
                maxRank = rnd.nextInt( 49700 ) + 300;
                size = (int) ( rnd.nextDouble() * rnd.nextDouble() * 300 ) + 1;
                double ESsum = 0;
                for( int i = 0; i < nGroups; i++ )
                {
                    GroupInfo group = new GroupInfo( maxRank );
                    do
                    {
                        group.put( rnd.nextInt( maxRank ) );
                    }
                    while( group.getSize() < size );
                    group.calculateScore();
                    ESsum += Math.abs( group.getES() );
                }
                double ESavg = ESsum / nGroups;
                double ESavgCalc = GroupInfo.getAverageES( maxRank, size );
                writer.println( maxRank + "\t" + size + "\t" + String.format( "%.4f", ESavg ) + "\t" + String.format( "%.4f", ESavgCalc )
                        + "\t" + String.format( "%2.2f%%", Math.abs( ESavgCalc - ESavg ) / ESavg * 100 ) );
                writer.flush();
            }
            /*        for(int trial = 0; trial < 5; trial++)
            {
            maxRank = rnd.nextInt(50000)+50;
            size = rnd.nextInt(10)+maxRank/2;
            double ESsum = 0;
            for(int i=0; i<nGroups; i++)
            {
                GroupInfo group = new GroupInfo(maxRank);
                do
                {
                    group.put(rnd.nextInt(maxRank));
                }
                while( group.getSize() < size );
                group.calculateScore();
                ESsum+=Math.abs(group.getES());
            }
            double ESavg = ESsum/nGroups;
            double ESavgCalc = GroupInfo.getAverageES(maxRank, size);
            writer.println(maxRank+"\t"+size+"\t"+String.format("%.4f", ESavg)+"\t"+String.format("%.4f", ESavgCalc)+"\t"+String.format("%2.2f%%", Math.abs(ESavgCalc-ESavg)/ESavg*100));
            writer.flush();
            }*/
            for( int trial = 0; trial < 200; trial++ )
            {
                maxRank = (int) ( rnd.nextDouble() * rnd.nextDouble() * 50000 ) + 2;
                size = (int) ( rnd.nextDouble() * rnd.nextDouble() * ( maxRank / 2 - 1 ) + 1 );
                double ESsum = 0;
                for( int i = 0; i < nGroups; i++ )
                {
                    GroupInfo group = new GroupInfo( maxRank );
                    do
                    {
                        group.put( rnd.nextInt( maxRank ) );
                    }
                    while( group.getSize() < size );
                    group.calculateScore();
                    ESsum += Math.abs( group.getES() );
                }
                double ESavg = ESsum / nGroups;
                double ESavgCalc = GroupInfo.getAverageES( maxRank, size );
                writer.println( maxRank + "\t" + size + "\t" + String.format( "%.4f", ESavg ) + "\t" + String.format( "%.4f", ESavgCalc )
                        + "\t" + String.format( "%2.2f%%", Math.abs( ESavgCalc - ESavg ) / ESavg * 100 ) );
                writer.flush();
            }
        }
    }
}
