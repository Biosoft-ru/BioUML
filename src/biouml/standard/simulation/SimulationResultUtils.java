package biouml.standard.simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

public class SimulationResultUtils
{
    public static void write(SimulationResult result, String filePath) throws IOException
    {
        File f = new File( filePath );
        try (BufferedWriter bw = ApplicationUtils.utfAppender( f ))
        {
            String[] variables = result.getVariables();
            double[] times = result.getTimes();
            double[][] values = result.getValues();

            bw.write( StreamEx.of( variables ).prepend( "Time" ).joining( "\t" ) );
            bw.write( "\n" );
            for( int i = 0; i < times.length; i++ )
            {
                bw.write( DoubleStreamEx.of( values[i] ).prepend( times[i] ).joining( "\t" ) );
                bw.write( "\n" );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    public static void write(SimulationResult result, String[] variables, String filePath) throws IOException
    {
        File f = new File( filePath );
        try (BufferedWriter bw = ApplicationUtils.utfAppender( f ))
        {
            double[] times = result.getTimes();
            double[][] values = result.getValues( variables );

            bw.write( StreamEx.of( variables ).prepend( "Time" ).joining( "\t" ) );
            bw.write( "\n" );
            for( int i = 0; i < times.length; i++ )
            {
                bw.write( DoubleStreamEx.of( values[i] ).prepend( times[i] ).joining( "\t" ) );
                bw.write( "\n" );
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    public static String toString(SimulationResult result, String[] variables) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        double[] times = result.getTimes();
        double[][] values = result.getValuesTransposed( variables );

        sb.append( StreamEx.of( variables ).prepend( "Time" ).joining( "\t" ) );
        sb.append( "\n" );
        for( int i = 0; i < times.length; i++ )
        {
            sb.append( DoubleStreamEx.of( values[i] ).prepend( times[i] ).joining( "\t" ) );
            sb.append( "\n" );
        }
        return sb.toString();
    }
}
