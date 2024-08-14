package ru.biosoft.bsa.analysis.maos.coord_mapping;

import ru.biosoft.bsa.Interval;

public class CoordinateMappingArray implements CoordinateMapping
{
    protected int[] mapping;
    protected Interval bounds;

    public CoordinateMappingArray(Interval bounds)
    {
        this.bounds = bounds;
        mapping = new int[bounds.getLength()];
        for(int i = 0; i < bounds.getLength(); i++)
            mapping[i] = -1;
    }

    @Override
    public int get(int x)
    {
        return mapping[x - bounds.getFrom()];
    }

    protected void set(int sourceCoord, int refCoord)
    {
        mapping[sourceCoord - bounds.getFrom()] = refCoord;
    }

    @Override
    public Interval getBounds()
    {
        return bounds;
    }
}