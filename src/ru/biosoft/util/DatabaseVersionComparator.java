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
    // scaled long value cannot overflow (10^15 * 10 < 2^63). Digits beyond this are dropped
    // while parsing (so the accumulator can never overflow).
    private static final int MAX_FRACTION_DIGITS = 15;
    // Maximum number of integer digits accumulated before dropping the rest (18 is the last
    // width where value * 10 + 9 cannot overflow a long).
    private static final int MAX_INTEGER_DIGITS = 18;

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
            // Parse integer and fractional digit values separately (e.g. "52.36" -> int 52, frac 36).
            // Digits beyond MAX_*_DIGITS are dropped while parsing so the accumulators cannot
            // overflow (real version strings are far shorter; the old Double-based code would
            // lose precision or saturate on such inputs, never reverse direction).
            long intPart1 = 0, intPart2 = 0;
            long fracPart1 = 0, fracPart2 = 0;
            int intDigits1 = 0, intDigits2 = 0;
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
                    if( fracDigits1 < MAX_FRACTION_DIGITS )
                    {
                        fracPart1 = fracPart1 * 10 + ( c - '0' );
                        fracDigits1++;
                    }
                }
                else
                {
                    if( intDigits1 < MAX_INTEGER_DIGITS )
                    {
                        intPart1 = intPart1 * 10 + ( c - '0' );
                        intDigits1++;
                    }
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
                    if( fracDigits2 < MAX_FRACTION_DIGITS )
                    {
                        fracPart2 = fracPart2 * 10 + ( c - '0' );
                        fracDigits2++;
                    }
                }
                else
                {
                    if( intDigits2 < MAX_INTEGER_DIGITS )
                    {
                        intPart2 = intPart2 * 10 + ( c - '0' );
                        intDigits2++;
                    }
                }
            }
            // Compare integer parts first; scale fractional parts to a common width before
            // comparing them (e.g. 52.3 vs 52.30 -> 30 vs 30).
            int cmp = Long.compare( intPart1, intPart2 );
            if( cmp == 0 )
            {
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
