package biouml.plugins.gtrd.master.sites.dnase;

public class Hotspot2DNaseCluster extends DNaseCluster
{
    public static final String PEAK_CALLER = "hotspot2";
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

}
