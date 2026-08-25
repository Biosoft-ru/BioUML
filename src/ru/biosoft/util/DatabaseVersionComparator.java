package ru.biosoft.util;

import java.util.Comparator;

/**
 * Comparator for database versions.
 * Version is supposed to be <number>[.<number>[some symbols]], for example: 52.36n or 2020.3
 * Compare numerical parts as doubles. If numerical parts are equal, then compare as strings.
 * @author anna
 */
public class DatabaseVersionComparator implements Comparator<String>
{
    // Maximum number of fractional digits taken into account. Beyond this the values are
    // indistinguishable for any practical version string, and 15 is the last width where a
    // scaled long value cannot overflow (10^15 * 10 < 2^63).
    private static final int MAX_FRACTION_DIGITS = 15;
    private static final long TRUNCATION_MODULUS = 1000000000000000L;

    @Override
    public int compare(String o1, String o2)
    {
        // The numerical prefix format is <digits>.<digits> — parse it directly instead of
        // going through Pattern/Matcher/Double (this comparator runs inside TreeSet operations
        // while scanning all database versions, so it is on a hot path). The parse below is
        // allocation-free and equivalent to the old ^(\\d+(?:\\.\\d+)) regex match: a match
        // requires a decimal point with digits after it, and an empty prefix means "no match"
        // exactly as Matcher.find() == false (e.g. "52" has no match, "52.36n" -> "52.36").
        int i1 = parseNumber( o1 );
        int i2 = parseNumber( o2 );
        if( i1 > 0 && i2 > 0 )
        {
            // Parse integer and fractional digit values separately (e.g. "52.36" -> int 52, frac 36)
            long intPart1 = 0, intPart2 = 0;
            long fracPart1 = 0, fracPart2 = 0;
            int fracDigits1 = 0, fracDigits2 = 0;
            boolean d1 = false, d2 = false;
            for( int i = 0; i < i1; i++ )
            {
                char c = o1.charAt( i );
                if( c == '.' )
                {
                    d1 = true;
                    continue;
                }
                if( d1 )
                {
                    fracPart1 = fracPart1 * 10 + ( c - '0' );
                    fracDigits1++;
                }
                else
                {
                    intPart1 = intPart1 * 10 + ( c - '0' );
                }
            }
            for( int i = 0; i < i2; i++ )
            {
                char c = o2.charAt( i );
                if( c == '.' )
                {
                    d2 = true;
                    continue;
                }
                if( d2 )
                {
                    fracPart2 = fracPart2 * 10 + ( c - '0' );
                    fracDigits2++;
                }
                else
                {
                    intPart2 = intPart2 * 10 + ( c - '0' );
                }
            }
            // Compare integer parts first; scale fractional parts to a common width before
            // comparing them (e.g. 52.3 vs 52.30 -> 30 vs 30).
            int cmp = Long.compare( intPart1, intPart2 );
            if( cmp == 0 )
            {
                if( fracDigits1 > MAX_FRACTION_DIGITS )
                {
                    fracPart1 = fracPart1 % TRUNCATION_MODULUS;
                    fracDigits1 = MAX_FRACTION_DIGITS;
                }
                if( fracDigits2 > MAX_FRACTION_DIGITS )
                {
                    fracPart2 = fracPart2 % TRUNCATION_MODULUS;
                    fracDigits2 = MAX_FRACTION_DIGITS;
                }
                while( fracDigits1 < fracDigits2 )
                {
                    fracPart1 *= 10;
                    fracDigits1++;
                }
                while( fracDigits2 < fracDigits1 )
                {
                    fracPart2 *= 10;
                    fracDigits2++;
                }
                cmp = Long.compare( fracPart1, fracPart2 );
            }
            // Same rule as before: if the numerical parts differ, that decides; if they are
            // equal but the strings have trailing symbols (e.g. "52.36n"), fall back to the
            // plain string comparison.
            if( cmp != 0 || ( i1 == o1.length() && i2 == o2.length() ) )
                return cmp;
        }
        return o1.compareTo( o2 );
    }

    /**
     * Length of the leading <digits>.<digits> prefix of s (0 if s does not contain a
     * decimal point with digits after it — the same condition under which the old
     * regex Matcher.find() returned false).
     */
    private static int parseNumber(String s)
    {
        int len = s.length();
        int i = 0;
        while( i < len )
        {
            char c = s.charAt( i );
            if( c < '0' || c > '9' )
                break;
            i++;
        }
        if( i == 0 )
            return 0;
        if( i < len && s.charAt( i ) == '.' )
        {
            int j = i + 1;
            while( j < len )
            {
                char c = s.charAt( j );
                if( c < '0' || c > '9' )
                    break;
                j++;
            }
            if( j > i + 1 )
                return j;
        }
        return 0;
    }
}
