package ru.biosoft.access.support;

import java.text.DecimalFormat;

import ru.biosoft.access.core.DataCollection;

/**
 * Utility class to generate unique name for the specified database using pattern
 * specified by decimal format.
 */
public class IdGenerator
{
    /**
     * Generates unique name for the specified database using pattern specified by decimal format.
     * @param dc database
     * @param formatter name pattern
     * @return unique name
     */
    public static String generateUniqueName(DataCollection<?> dc, DecimalFormat formatter)
    {
        return generateUniqueName( dc, formatter, 0 );
    }

    /**
     * Generates unique name for the specified database using pattern specified by decimal format.
     * Necessary for correct reaction id creation (reaction id should be unique in all diagram elements).
     * Use {@link IdGenerator#generateUniqueName(DataCollection, DecimalFormat)} for not reaction elements.
     * @param dc database
     * @param formatter name pattern
     * @param startInd index to start with (if bigger than <b>dc</b> size)
     * @return unique name
     */
    public static String generateUniqueName(DataCollection<?> dc, DecimalFormat formatter, int startInd)
    {
        String name = null;

        int n = 0;
        if( dc != null )
            n = dc.getSize() + 1;
        if( n < startInd )
            n = startInd;
        while( dc != null )
        {
            name = formatter.format( n );
            if( !dc.contains( name ) )
                break;
            n++;
        }

        return name;
    }

}
