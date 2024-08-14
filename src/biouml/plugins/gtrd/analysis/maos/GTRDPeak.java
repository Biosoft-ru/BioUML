package biouml.plugins.gtrd.analysis.maos;

import ru.biosoft.bsa.Interval;

public class GTRDPeak extends Interval
{
    private double score;
    private SiteReference originalPeak;
    private String tfUniprotId;

    public GTRDPeak(int fromInclusive, int toInclusive, double score, SiteReference originalPeak, String tfUniprotId)
    {
        super( fromInclusive, toInclusive );
        this.score = score;
        this.originalPeak = originalPeak;
        this.tfUniprotId = tfUniprotId;
    }

    public double getScore() { return score; }
    
    public SiteReference getOriginalPeak()
    {
        return originalPeak;
    }
    
    public String getTfUniprotId()
    {
        return tfUniprotId;
    }
}
