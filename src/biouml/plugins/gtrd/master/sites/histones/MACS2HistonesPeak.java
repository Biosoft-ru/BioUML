package biouml.plugins.gtrd.master.sites.histones;

public class MACS2HistonesPeak extends HistonesPeak
{
    public static final String PEAK_CALLER = "macs2";

    protected float foldEnrichment;
    protected float mLog10PValue;
    protected float mLog10QValue;
    protected float pileup;
    protected int summit;
    protected boolean hasSummit;

    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getFoldEnrichment()
    {
        return foldEnrichment;
    }

    public void setFoldEnrichment(float foldEnrichment)
    {
        this.foldEnrichment = foldEnrichment;
    }

    public float getMLog10PValue()
    {
        return mLog10PValue;
    }

    public void setMLog10PValue(float mLog10PValue)
    {
        this.mLog10PValue = mLog10PValue;
    }

    public float getMLog10QValue()
    {
        return mLog10QValue;
    }

    public void setMLog10QValue(float mLog10QValue)
    {
        this.mLog10QValue = mLog10QValue;
    }

    public float getPileup()
    {
        return pileup;
    }

    public void setPileup(float pileup)
    {
        this.pileup = pileup;
    }
    
    @Override
    public boolean hasSummit()
    {
        return hasSummit;
    }
    @Override
    public int getSummit()
    {
        return summit;
    }

    public void setSummit(int summit)
    {
        this.summit = summit;
        hasSummit = true;
    }

    @Override
    public double getScore()
    {
        return getMLog10QValue();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 6*4;
    }

}
