package biouml.plugins.gtrd.master.sites.dnase;

public class MACS2DNaseCluster extends DNaseCluster
{
    public static final String PEAK_CALLER = "macs2";
    
    private float meanAbsSummit;
    private float medianAbsSummit;
    private float meanPileup;
    private float medianPileup;
    private float meanFoldEnrichment;
    private float medianFoldEnrichment;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }
    
    public float getMeanAbsSummit()
    {
        return meanAbsSummit;
    }
    public void setMeanAbsSummit(float meanAbsSummit)
    {
        this.meanAbsSummit = meanAbsSummit;
    }

    public float getMedianAbsSummit()
    {
        return medianAbsSummit;
    }
    public void setMedianAbsSummit(float medianAbsSummit)
    {
        this.medianAbsSummit = medianAbsSummit;
    }

    public float getMeanPileup()
    {
        return meanPileup;
    }
    public void setMeanPileup(float meanPileup)
    {
        this.meanPileup = meanPileup;
    }

    public float getMedianPileup()
    {
        return medianPileup;
    }
    public void setMedianPileup(float medianPileup)
    {
        this.medianPileup = medianPileup;
    }

    public float getMeanFoldEnrichment()
    {
        return meanFoldEnrichment;
    }
    public void setMeanFoldEnrichment(float meanFoldEnrichment)
    {
        this.meanFoldEnrichment = meanFoldEnrichment;
    }

    public float getMedianFoldEnrichment()
    {
        return medianFoldEnrichment;
    }
    public void setMedianFoldEnrichment(float medianFoldEnrichment)
    {
        this.medianFoldEnrichment = medianFoldEnrichment;
    }
    
    @Override
    public double getScore()
    {
        return medianPileup;
    }
    @Override
    public int getSummit()
    {
        return Math.round( medianAbsSummit ) - getFrom();
    }
    @Override
    public boolean hasSummit()
    {
        return true;
    }

    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 6*4;
    }
}
