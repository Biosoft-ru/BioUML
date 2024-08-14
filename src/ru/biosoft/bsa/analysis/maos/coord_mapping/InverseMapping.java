package ru.biosoft.bsa.analysis.maos.coord_mapping;

import ru.biosoft.bsa.Interval;

public class InverseMapping extends CoordinateMappingArray
{
    public InverseMapping(Interval bounds, CoordinateMapping source)
    {
        super( bounds );
        for( int pos = source.getBounds().getFrom(); pos <= source.getBounds().getTo(); pos++ )
        {
            int mapped = source.get( pos );
            if( mapped != -1 )
                set( mapped, pos );
        }
    }
}