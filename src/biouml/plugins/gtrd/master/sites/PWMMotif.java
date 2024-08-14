package biouml.plugins.gtrd.master.sites;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.StrandType;

public class PWMMotif extends GenomeLocation
{
    protected boolean forwardStrand;
    protected DataElementPath siteModelPath;
    protected float score;
   
    @Override
    public String getStableId()
    {
        return "w." + siteModelPath.getName() + "." + id;
    }
    
    public boolean isForwardStrand()
    {
        return forwardStrand;
    }
    public void setForwardStrand(boolean forwardStrand)
    {
        this.forwardStrand = forwardStrand;
    }

    public DataElementPath getSiteModelPath()
    {
        return siteModelPath;
    }

    public void setSiteModelPath(DataElementPath siteModelPath)
    {
        this.siteModelPath = siteModelPath;
    }
    
    public SiteModel getSiteModel()
    {
        return getSiteModelPath().getDataElement( SiteModel.class );
    }

    @Override
    public double getScore()
    {
        return score;
    }

    public void setScore(float score)
    {
        this.score = score;
    }

    @Override
    public int getStrand()
    {
        return forwardStrand? StrandType.STRAND_PLUS : StrandType.STRAND_MINUS;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize()
                +4//forwardStrand
                +8//siteModelPath
                +4//score
                ;
    }
}