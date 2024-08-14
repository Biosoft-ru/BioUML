package biouml.plugins.gtrd.master.sites.chipseq;

public class PICSPeak extends ChIPSeqPeak
{
    public static final String PEAK_CALLER = "pics";
    public static final String[] FIELDS = new String[]{"score"};
    
    protected float picsScore;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getPicsScore()
    {
        return picsScore;
    }

    public void setPicsScore(float picsScore)
    {
        this.picsScore = picsScore;
    }
    
    @Override
    public double getScore()
    {
        return getPicsScore();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4;
    }
}