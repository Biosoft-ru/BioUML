package biouml.plugins.enrichment._test;

import biouml.plugins.enrichment.EnrichmentAnalysis;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class EnrichmentUtilsTest extends TestCase
{
    public void testGetPos()
    {
        double[] arr = new double[] {-10, -5, -5, -4, -3, 2, 3, 3, 3, 5, 7, 10};
        assertEquals(0, EnrichmentAnalysis.getPos(arr, -10));
        assertEquals(2, EnrichmentAnalysis.getPos(arr, -5));
        assertEquals(4, EnrichmentAnalysis.getPos(arr, -3));
        assertEquals(5, EnrichmentAnalysis.getPos(arr, 2));
        assertEquals(6, EnrichmentAnalysis.getPos(arr, 3));
        assertEquals(9, EnrichmentAnalysis.getPos(arr, 5));
        assertEquals(arr.length-1, EnrichmentAnalysis.getPos(arr, 10));
    }
}
