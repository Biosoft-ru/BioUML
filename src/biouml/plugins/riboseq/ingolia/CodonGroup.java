package biouml.plugins.riboseq.ingolia;

import ru.biosoft.bsa.Interval;

public class CodonGroup extends Interval
{
    public CodonGroup(int from, int to)
    {
        super(from, to);
    }
    
    public CodonGroup(int from)
    {
        super( from, from );
    }
    
    @Override
    public String toString()
    {
        if(getFrom() == getTo())
            return "["+getFrom()+"]";
        return "[" + getFrom() + "," + getTo() + "]";
    }
}
