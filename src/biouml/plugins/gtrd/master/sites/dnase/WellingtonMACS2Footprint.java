package biouml.plugins.gtrd.master.sites.dnase;

public class WellingtonMACS2Footprint extends WellingtonFootprint
{
    public static final String PEAK_CALLER = "wellington_macs2";

    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }
}
