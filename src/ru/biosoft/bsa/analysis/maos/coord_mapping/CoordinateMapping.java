package ru.biosoft.bsa.analysis.maos.coord_mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ru.biosoft.bsa.Interval;

public interface CoordinateMapping
{
    /**
     * Map coordinates from one coordinate system to another
     * @param x coordinates in source coordinate system
     * @return coordinates in destination coordinate system
     */
    int get(int x);

    /**
     * Domain of definition on source coordinate system.
     * @return
     */
    Interval getBounds();

    default Collection<Integer> mapInterval(Interval interval)
    {
        Set<Integer> result = Collections.emptySet();
        for( int i = 0; i < interval.getLength(); i++ )
        {
            int y = get( interval.getFrom() + i );
            if( y == -1 )
                continue;
            int mapped = y - i;
            if(result.isEmpty())
            {
                result = Collections.singleton( mapped );
            }
            else
            {
                if(result.size() == 1)
                {
                    if(!result.contains( mapped ))
                    {
                        result = new HashSet<>(result);
                        result.add(mapped);
                    }
                }
                else
                    result.add(mapped);
            }
        }
        return result;
    }
}