package biouml.plugins.gtrd.master.analyses.metacluster;

import java.util.List;

import biouml.plugins.gtrd.master.sites.GenomeLocation;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;

//Peak caller specific cluster
public class Cluster extends GenomeLocation
{
    private List<ChIPSeqPeak> peaks;
    private int summit;

    public List<ChIPSeqPeak> getPeaks()
    {
        return peaks;
    }
    public void setPeaks(List<ChIPSeqPeak> peaks)
    {
        this.peaks = peaks;
    }

    @Override
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
    
    public String getPeakCaller()
    {
        //all of peaks from the same peakCaller
        return peaks.get( 0 ).getPeakCaller();
    }
}
