package ru.biosoft.bsa.analysis.maos.coord_mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

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
        // Hot path of TFBS mutation analysis (called once per site position per
        // model). Keep the first distinct mapped value in a primitive local and
        // materialize an int[] buffer only when a second distinct value appears, so
        // the common single-position case does no temporary allocation or boxing.
        //
        // Invariant once the buffer exists: values[0] is always 'first'. Because the
        // first distinct value is always handled in the size==0 branch (before any
        // buffer exists), a value equal to 'first' can only be a duplicate after that
        // point, and it lives at values[0] -- so the linear duplicate scan must start
        // at j=0, NOT j=1 (a j=1 scan would let the first value re-enter as a new
        // distinct value). The distinct count is bounded by the number of
        // constant-offset segments in a MappingByVCF (each segment contributes at most
        // one distinct mapped value), which is small -- not by the interval length --
        // so the linear scan stays cheap. Callers consume the (deduplicated) result
        // only for emptiness checks and iterate/best scans, which need no Set semantics.
        int length = interval.getLength();
        int from = interval.getFrom();
        int[] values = null;
        int first = 0;
        int size = 0;
        for( int i = 0; i < length; i++ )
        {
            int y = get( from + i );
            if( y == -1 )
                continue;
            int mapped = y - i;
            if( size == 0 )
            {
                first = mapped;
                size = 1;
                continue;
            }
            if( values == null )
            {
                // Second distinct value: materialize the primitive buffer. Capacity
                // tracks the distinct count, never the interval length.
                if( mapped == first )
                    continue;
                values = new int[2];
                values[0] = first;
                values[1] = mapped;
                size = 2;
                continue;
            }
            // Linear membership test. Must scan from j=0: values[0] is 'first' and a
            // later value equal to 'first' is a duplicate, not a new distinct value.
            boolean found = false;
            for( int j = 0; j < size; j++ )
                if( values[j] == mapped )
                {
                    found = true;
                    break;
                }
            if( found )
                continue;
            if( size == values.length )
            {
                int[] grown = new int[values.length * 2];
                System.arraycopy( values, 0, grown, 0, size );
                values = grown;
            }
            values[size++] = mapped;
        }
        if( size == 0 )
            return Collections.emptySet();
        if( size == 1 )
            return Collections.singleton( first );
        Collection<Integer> result = new ArrayList<>( size );
        for( int j = 0; j < size; j++ )
            result.add( values[j] );
        return result;
    }
}