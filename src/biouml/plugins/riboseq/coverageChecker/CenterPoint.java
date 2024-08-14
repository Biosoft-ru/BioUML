package biouml.plugins.riboseq.coverageChecker;

public class CenterPoint
{
    private int point;
    private int counter;
    private int strand;

    public CenterPoint(int point, int strand)
    {
        this.point = point;
        this.strand = strand;
        counter = 1;
    }

    public int getPoint()
    {
        return point;
    }

    public int getStrand()
    {
        return strand;
    }

    public int getCounter()
    {
        return counter;
    }

    public void incrementCounter()
    {
        counter++;
    }

    @Override
    public boolean equals(Object o)
    {
        if( this == o )
        {
            return true;
        }
        if( ! ( o instanceof CenterPoint ) )
        {
            return false;
        }

        CenterPoint that = (CenterPoint)o;

        if( counter != that.counter )
        {
            return false;
        }
        if( point != that.point )
        {
            return false;
        }
        if( strand != that.strand )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }
}
