package biouml.plugins.ensembl.type;

import ru.biosoft.bsa.Interval;

public class AssemblyException
{
    private int derivedRegionId;
    private Interval derivedRegion;
    private int sourceRegionId;
    private Interval sourceRegion;

    public AssemblyException(int derivedRegionId, Interval derivedRegion, int sourceRegionId, Interval sourceRegion)
    {
        this.derivedRegionId = derivedRegionId;
        this.derivedRegion = derivedRegion;
        this.sourceRegionId = sourceRegionId;
        this.sourceRegion = sourceRegion;
    }

    public int getDerivedRegionId()
    {
        return derivedRegionId;
    }

    public Interval getDerivedRegion()
    {
        return derivedRegion;
    }

    public int getSourceRegionId()
    {
        return sourceRegionId;
    }

    public Interval getSourceRegion()
    {
        return sourceRegion;
    }

    public int translateFromDerivedToSource(int pos)
    {
        return sourceRegion.getFrom() + pos - derivedRegion.getFrom();
    }
    
    public int translateFromSourceToDerived(int pos)
    {
        return derivedRegion.getFrom() + pos - sourceRegion.getFrom();
    }
}
