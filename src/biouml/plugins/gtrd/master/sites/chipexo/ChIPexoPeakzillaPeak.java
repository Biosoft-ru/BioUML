package biouml.plugins.gtrd.master.sites.chipexo;

public class ChIPexoPeakzillaPeak extends ChIPexoPeak
{
    public static final String PEAK_CALLER = "peakzilla";
    
    protected float chip,control,distributionScore,fdr,foldEnrichment,peakZillaScore;
    protected int summit;
    
    @Override
    public double getScore()
    {
        return getFdr();
    }
    
    public float getPeakZillaScore()
    {
        return peakZillaScore;
    }

    public void setPeakZillaScore(float peakZillaScore)
    {
        this.peakZillaScore = peakZillaScore;
    }
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getChip()
    {
        return chip;
    }

    public void setChip(float chip)
    {
        this.chip = chip;
    }

    public float getControl()
    {
        return control;
    }

    public void setControl(float control)
    {
        this.control = control;
    }

    public float getDistributionScore()
    {
        return distributionScore;
    }

    public void setDistributionScore(float distributionScore)
    {
        this.distributionScore = distributionScore;
    }

    public float getFdr()
    {
        return fdr;
    }

    public void setFdr(float fdr)
    {
        this.fdr = fdr;
    }

    public float getFoldEnrichment()
    {
        return foldEnrichment;
    }

    public void setFoldEnrichment(float foldEnrichment)
    {
        this.foldEnrichment = foldEnrichment;
    }

    public int getSummit()
    {
        return summit;
    }
    
    @Override
    public boolean hasSummit()
    {
        return true;
    }

    public void setSummit(int summit)
    {
        this.summit = summit;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4*7;
    }
}