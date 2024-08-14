package biouml.plugins.gtrd.master.analyses.percellclusters;

import biouml.plugins.gtrd.master.sites.GenomeLocation;


public class PerCellCluster extends GenomeLocation
{
    private String masterSiteId;
    
    private int summit;
    
    int chipSeqExpCount;
    int chipExoExpCount;
    int dnasePeakCount;
    int motifCount;

    public String getMasterSiteId()
    {
        return masterSiteId;
    }
    public void setMasterSiteId(String masterSiteId)
    {
        this.masterSiteId = masterSiteId;
    }
    
    public int getSummit()
    {
        return summit;
    }
    public void setSummit(int summit)
    {
        this.summit = summit;
    }
}
