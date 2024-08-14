package biouml.plugins.gtrd.master.sites.dnase;

public abstract class WellingtonFootprint extends DNaseFootprint
{
    protected float wellingtonScore;

    public float getWellingtonScore()
    {
        return wellingtonScore;
    }

    public void setWellingtonScore(float wellingtonScore)
    {
        this.wellingtonScore = wellingtonScore;
    }
    
    @Override
    public double getScore()
    {
        return getWellingtonScore();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4;
    }
}