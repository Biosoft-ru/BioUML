package biouml.plugins.gtrd.master.index;

import java.util.ArrayList;

import ru.biosoft.bsa.Interval;
import ru.biosoft.rtree.IntervalArray;

public class ListOfIntervals extends ArrayList<Interval> implements IntervalArray
{
    @Override
    public int getFrom(int i)
    {
        return get( i ).getFrom();
    }

    @Override
    public int getTo(int i)
    {
        return get(i).getTo();
    }
}
