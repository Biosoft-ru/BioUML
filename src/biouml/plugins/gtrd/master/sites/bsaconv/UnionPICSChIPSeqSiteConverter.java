package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.PICSPeak;

public class UnionPICSChIPSeqSiteConverter extends UnionChIPSeqSiteConverter<PICSPeak>
{
    public UnionPICSChIPSeqSiteConverter(Function<String, ChIPseqExperiment> expSupplier)
    {
        super( expSupplier );
    }

    @Override
    protected PICSPeak createPeak()
    {
        return new PICSPeak();
    }

    @Override
    protected void updatePeakFromSiteProperties(PICSPeak peak, DynamicPropertySet dps)
    {
        super.updatePeakFromSiteProperties( peak, dps );
        peak.setPicsScore( ( (Number)dps.getValue( "score" ) ).floatValue() );
    }
}
