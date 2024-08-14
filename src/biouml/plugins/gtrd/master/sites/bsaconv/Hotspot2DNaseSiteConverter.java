package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.Hotspot2DNasePeak;

public class Hotspot2DNaseSiteConverter extends DNaseSiteConverter<Hotspot2DNasePeak>
{
    public Hotspot2DNaseSiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }
    @Override
    protected Hotspot2DNasePeak createPeak()
    {
        return new Hotspot2DNasePeak();
    }
    @Override
    protected void updatePeakFromSiteProperties(Hotspot2DNasePeak peak, DynamicPropertySet dps)
    {
        peak.setScore2( Float.parseFloat( (String)dps.getValue( "itemRGB" ) ) );
        peak.setScore1( ((Number)dps.getValue( "score" )).floatValue() );
    }
}