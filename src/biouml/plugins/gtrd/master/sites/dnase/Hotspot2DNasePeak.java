package biouml.plugins.gtrd.master.sites.dnase;

public class Hotspot2DNasePeak extends DNasePeak
{
    public static final String PEAK_CALLER = "hotspot2";
    
    protected float score1;
    protected float score2;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getScore1()
    {
        return score1;
    }

    public void setScore1(float score1)
    {
        this.score1 = score1;
    }

    public float getScore2()
    {
        return score2;
    }

    public void setScore2(float score2)
    {
        this.score2 = score2;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 2*4;
    }
}