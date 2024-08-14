package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.WellingtonFootprint;

public abstract class WellingtonSiteConverter<T extends WellingtonFootprint> extends DNaseSiteConverter<T>
{
    public WellingtonSiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }

    @Override
    protected void updatePeakFromSiteProperties(WellingtonFootprint peak, DynamicPropertySet dps)
    {
        peak.setWellingtonScore( ((Number)dps.getValue( "score" )).floatValue() );
    }
}