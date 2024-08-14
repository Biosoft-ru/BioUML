package ru.biosoft.bsa.analysis.maos.coord_mapping;

import ru.biosoft.bsa.Interval;

public class ReverseStrandMapping implements CoordinateMapping
{
    private final CoordinateMapping source;
    private final Interval targetInterval;
    public ReverseStrandMapping(CoordinateMapping source, Interval targetInterval)
    {
        this.source = source;
        this.targetInterval = targetInterval;
    }

    @Override
    public int get(int x)
    {
        int result = source.get( source.getBounds().getFrom() + source.getBounds().getTo() - x );
        if( result == -1 )
            return -1;
        return targetInterval.getFrom() + targetInterval.getTo() - result;
    }

    @Override
    public Interval getBounds()
    {
        return source.getBounds();
    }
}