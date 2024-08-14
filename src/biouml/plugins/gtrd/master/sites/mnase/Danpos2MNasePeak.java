package biouml.plugins.gtrd.master.sites.mnase;

public class Danpos2MNasePeak extends MNasePeak
{
    public static final String PEAK_CALLER = "danpos2";

    protected float fuzzinessScore;
    protected int summit;
    protected float summitValue;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getFuzzinessScore()
    {
        return fuzzinessScore;
    }

    public void setFuzzinessScore(float fuzzinessScore)
    {
        this.fuzzinessScore = fuzzinessScore;
    }

    public boolean hasSummit()
    {
        return true;
    }
    @Override
    public int getSummit()
    {
        return summit;
    }

    public void setSummit(int summit)
    {
        this.summit = summit;
    }

    public float getSummitValue()
    {
        return summitValue;
    }

    public void setSummitValue(float summitValue)
    {
        this.summitValue = summitValue;
    }
    
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 3*4;
    }
}
