package biouml.plugins.ensembl.analysis;

public class SiteData
{
    public enum Location
    {
        GENE, INTRON, EXON, FIVE_PRIME, THREE_PRIME
    };
    
    private SiteData.Location location;
    private int strand;
    private int offset;

    public SiteData(SiteData.Location location, int strand, int offset)
    {
        super();
        this.location = location;
        this.strand = strand;
        this.offset = offset;
    }

    public SiteData.Location getLocation()
    {
        return location;
    }

    public int getStrand()
    {
        return strand;
    }

    public int getOffset()
    {
        return offset;
    }
}