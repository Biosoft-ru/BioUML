package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;
import com.developmentontheedge.application.ApplicationUtils;
import junit.framework.TestCase;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

/**
 * @author lan, axec
 *
 */
public class StatTest extends TestCase
{
    public void testMean() throws Exception
    {
        double[] yesPoints = new double[] {10, 10, 10, 10, 12, 12, 12, 13, 13, 13};
        assertEquals( 11.5, Stat.mean( yesPoints ) );
        Double[] noPoints = new Double[] {6.0, 8.0, 8.0, 8.0, 10.0, 10.0, 10.0, 12.0};
        assertEquals( 9.0, Stat.mean( noPoints ) );
    }


    /**
     * Test for CHiSquare quantile
     */
    public void testChiSquare() throws Exception
    {
        InputStream inputStream = StatTest.class.getResourceAsStream( "resources/chi2q.txt" );
        List<String> lines = ApplicationUtils.readAsList( inputStream );
        String[] probabilities = lines.get( 0 ).split( "\t" );
        double maxDiff = Double.NEGATIVE_INFINITY;
        double maxDiffP = Double.NaN;
        double maxDiffDF = Double.NaN;
        double maxDiffExpected = Double.NaN;
        double maxDiffCalculated = Double.NaN;
        for( int i = 1; i < lines.size(); i++ )
        {
            String[] data = lines.get( i ).split( "\t" );
            int df = Integer.valueOf( data[0] );
            for( int j = 1; j < data.length; j++ )
            {
                double p = Double.valueOf( probabilities[j] );
                double calculated = Stat.quantileChiSquare( df, p );
                double expected = Double.valueOf( data[j] );
                double diff = Math.abs( expected - calculated ) / expected;
                if( diff > maxDiff )
                {
                    maxDiff = diff;
                    maxDiffP = p;
                    maxDiffDF = df;
                    maxDiffExpected = expected;
                    maxDiffCalculated = calculated;
                }
                double err = 0.1;
                assertEquals( "df = " + df + "\tp =" + probabilities[j] + ", err =" + err, 0, diff, err );
            }
        }

        System.out.println( "Maximum relative error is at p=" + maxDiffP + ", df=" + maxDiffDF + ": " + maxDiff + ", expected "
                + maxDiffExpected + ", was " + maxDiffCalculated );

        printChi2Quantiles();
    }

    public void printChi2Quantiles() throws Exception
    {
        DecimalFormat formatter = new DecimalFormat( "#.###" );
        InputStream inputStream = StatTest.class.getResourceAsStream( "resources/chi2q.txt" );
        List<String> lines = ApplicationUtils.readAsList( inputStream );
        double[] probabilities = StreamEx.of( lines.get( 0 ).split( "\t" ) ).without( " " ).mapToDouble( v -> Double.valueOf( v ) )
                .toArray();
        System.out.println( lines.get( 0 ) );
        for( int i = 1; i < lines.size(); i++ )
        {
            final int df = Integer.valueOf( lines.get( i ).split( "\t" )[0] );
            String line = DoubleStreamEx.of( probabilities ).mapToObj( v -> {
                try
                {
                    return String.valueOf( formatter.format( Stat.quantileChiSquare( df, v ) ) );
                }
                catch( Exception e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return "-1";
            } ).prepend( String.valueOf( df ) ).joining( "\t" );
            System.out.println( line );
        }
    }
}
