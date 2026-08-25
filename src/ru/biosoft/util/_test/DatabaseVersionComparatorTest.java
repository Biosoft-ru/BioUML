package ru.biosoft.util._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.biosoft.util.DatabaseVersionComparator;
import junit.framework.TestCase;

/**
 * Tests for {@link DatabaseVersionComparator}. The comparator is used to order database
 * versions (like "52.36n" or "2020.3"): numerical parts are compared as numbers, and if
 * they are equal (or one version has no numerical part) the plain string comparison is used.
 */
public class DatabaseVersionComparatorTest extends TestCase
{
    private final DatabaseVersionComparator comparator = new DatabaseVersionComparator();

    private void assertOrder(String lower, String higher)
    {
        assertTrue( "Expected " + lower + " < " + higher, comparator.compare( lower, higher ) < 0 );
        assertTrue( "Expected " + higher + " > " + lower, comparator.compare( higher, lower ) > 0 );
    }

    public void testIntegerParts()
    {
        assertOrder( "52.36", "107.1" );
        assertOrder( "75.13", "100.1" );
        assertOrder( "999.1", "1000.1" );
        assertOrder( "3.0", "10.0" );
    }

    public void testFractionalParts()
    {
        assertOrder( "52.36", "52.4" );
        assertOrder( "75.13", "75.14" );
        // trailing zeros in the fractional part do not change the value
        assertEquals( 0, Integer.signum( comparator.compare( "52.30", "52.3" ) ) );
    }

    public void testNoDecimalPartFallsBackToString()
    {
        // A version without a decimal part has no numerical match and is compared as a string
        assertOrder( "3", "52.36" );
        assertOrder( "52", "999" );
        assertOrder( "107", "2020" );
    }

    public void testTrailingSymbols()
    {
        // Equal numerical parts with trailing symbols -> string comparison
        assertOrder( "52.36", "52.361" );
        assertOrder( "52.36n", "52.361" );
        // ' ' (0x20, the char after "75.13") sorts before 'a' (0x61) in the string fallback
        assertOrder( "75.13 (homo_sapiens)", "75.13a" );
        // A version with trailing symbols compares to another version by its numerical part
        assertOrder( "52.36n", "107.1" );
    }

    public void testNonNumericVersions()
    {
        // no numerical part at all on both sides -> plain (case-sensitive) string comparison
        assertOrder( "Ensembl 75", "abc" );
        assertOrder( "52abc", "75 Ensembl" );
    }

    public void testLongDigitRunsDoNotOverflow()
    {
        // Fractional and integer digit runs longer than the tracked width must not throw and
        // must not reverse the comparison direction (extra digits are dropped while parsing).
        String longFrac = "1." + "1234567890123456789012345";
        String longInt = "999999999999999999999999.1";
        // a very large integer part still compares greater than a small one, in both directions
        assertTrue( comparator.compare( longInt, "1.0" ) > 0 );
        assertTrue( comparator.compare( "1.0", longInt ) < 0 );
        // a very long fractional part compares without throwing
        comparator.compare( longFrac, "1.0" );
        comparator.compare( "1.0", longFrac );
    }

    public void testFullSorting()
    {
        List<String> versions = Arrays.asList(
                "0", "0.0", "0.1", "1.1", "1.10", "1.2", "100.1", "1000.1", "101.1",
                "106.9", "107.1", "112.1", "113.1", "2.0", "2020.3", "3.0", "52.3",
                "52.30", "52.36", "52.36n", "52.361", "106.1", "75.13",
                "75.13 (homo_sapiens)", "75.1", "999.1" );
        List<String> sorted = new ArrayList<>( versions );
        Collections.sort( sorted, comparator );
        List<String> expected = Arrays.asList(
                "0", "0.0", "0.1", "1.1", "1.10", "1.2", "2.0", "3.0", "52.3",
                "52.30", "52.36", "52.36n", "52.361", "75.1", "75.13",
                "75.13 (homo_sapiens)", "100.1", "101.1", "106.1", "106.9",
                "107.1", "112.1", "113.1", "999.1", "1000.1", "2020.3" );
        assertEquals( expected, sorted );
    }
}
