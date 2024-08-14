package ru.biosoft.util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comparator for database versions. 
 * Version is supposed to be <number>[.<number>[some symbols]], for example: 52.36n or 2020.3
 * Compare numerical parts as doubles. If numerical parts are equal, then compare as strings.
 * @author anna
 */
public class DatabaseVersionComparator implements Comparator<String>
{
    private Pattern p = Pattern.compile( "^(\\d+(?:\\.\\d+))" );
    @Override
    public int compare(String o1, String o2)
    {
        try
        {
            Matcher m1 = p.matcher( o1 );
            Matcher m2 = p.matcher( o2 );
            if( m1.find() && m2.find() )
            {
                String d1s = m1.group( 1 );
                String d2s = m2.group( 1 );
                Double d1 = Double.parseDouble( d1s );
                Double d2 = Double.parseDouble( d2s );
                int cmp = d1.compareTo( d2 );
                if( cmp != 0 || ( d1s.length() == o1.length() && d2s.length() == o2.length() ) )
                    return cmp;
            }
        }
        catch( NullPointerException | NumberFormatException ex )
        {
        }
        return o1.compareTo( o2 );
    }
}