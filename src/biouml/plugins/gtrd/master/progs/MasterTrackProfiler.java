package biouml.plugins.gtrd.master.progs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.sites.MasterSite;
import biouml.plugins.gtrd.master.utils.SizeOfUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.bsa.track.big.BigBedTrack;

public class MasterTrackProfiler implements IApplication
{

    @Override
    public Object start(IApplicationContext arg0) throws Exception
    {
        String[] commandLineArgs;
        Object arg = arg0.getArguments().get( "application.args" );
        if( arg instanceof String[] )
            commandLineArgs = (String[])arg;
        else
            commandLineArgs = new String[0];

        //System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        //System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        //System.setIn( new FileInputStream( FileDescriptor.in ) );

        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                int status = 0;
                try
                {
                    main( commandLineArgs );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                    status = 1;
                }
                System.exit( status );
            }
        }, "Main" ).start();

        return null;
    }

    @Override
    public void stop()
    {
        // TODO Auto-generated method stub

    }

    public static void main(String[] args) throws Exception
    {

        String bbFile = args[0];

        Properties props = new Properties();
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "test.bb" );
        props.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbFile );
        MasterTrack mt = new MasterTrack( null, props );
        System.out.println( "Reading bigBed" );
        long startTime = System.currentTimeMillis();
        List<MasterSite> result = mt.queryAll();
        System.out.println( "Elapsed: " + ( System.currentTimeMillis() - startTime ) + "ms" );

        System.out.println( "Waiting 3 seconds for GC" );
        System.gc();
        Thread.sleep( 3000 );
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println( "Memory used to store BedEntries in memory: " + usedMemory + ", that is "
                + ( ( (double)usedMemory ) / result.size() ) + " bytes per item" );

        long expected = calculateExpectedSize( result );
        System.out.println( "Expected size is " + expected + ", that is " + ( (double)expected / result.size() ) +  " bytes per item" );

        if( args.length > 1 )
        {
            bbFile = args[1];
            new File( bbFile ).delete();
            props = new Properties();
            props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "test.bb" );
            props.setProperty( BigBedTrack.PROP_BIGBED_PATH, bbFile );
            MasterTrack mt2 = new MasterTrack( null, props, false );

            System.out.println( "Writing bigBed" );
            startTime = System.currentTimeMillis();
            mt2.writeMetadata( mt.getMetadata() );
            mt2.write( result, mt.getChromSizes() );

            System.out.println( "Elapsed: " + ( System.currentTimeMillis() - startTime ) + "ms" );

            mt2.close();
        }
        
        Random rnd = new Random();
        System.out.println("Query 100 intervals from each chromosome, each 10Kbp.");
        startTime = System.currentTimeMillis();
        for(String chr : mt.getChromosomes())
        {
            int chrLength = mt.getChromSizes().get( chr );
            for(int i = 0; i < 100; i++)
            {
                final int QUERY_LENGTH = 10000;
                int from = rnd.nextInt( chrLength - QUERY_LENGTH );
                int to = from + QUERY_LENGTH - 1;
                mt.query( chr, from, to );
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        double millisPerQuery = elapsed / (100*mt.getChromosomes().size());
        System.out.println( "Elapsed: " + elapsed + "ms, that is " + millisPerQuery + "ms per query" );
        
        
        mt.close();
    }

    private static long calculateExpectedSize(List<MasterSite> result)
    {
        return SizeOfUtils.sizeOfArrayList( (ArrayList<MasterSite>)result );
    }

}
