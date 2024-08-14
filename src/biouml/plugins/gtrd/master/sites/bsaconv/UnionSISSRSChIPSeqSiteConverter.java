package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.SISSRSPeak;

public class UnionSISSRSChIPSeqSiteConverter extends UnionChIPSeqSiteConverter<SISSRSPeak>
{
    public UnionSISSRSChIPSeqSiteConverter(Function<String, ChIPseqExperiment> expSupplier)
    {
        super( expSupplier );
    }

    @Override
    protected SISSRSPeak createPeak()
    {
        return new SISSRSPeak();
    }

    @Override
    protected void updatePeakFromSiteProperties(SISSRSPeak peak, DynamicPropertySet dps)
    {
        super.updatePeakFromSiteProperties( peak, dps );
        peak.setNumTags( (Integer)dps.getValue( "NumTags" ) );
        Object obj = dps.getValue( "Fold" );
        if(obj != null)
            peak.setFold( ( (Number)obj ).floatValue() );
        obj = dps.getValue( "p-value" );
        if(obj != null)
            peak.setPValue( ( (Number)obj ).floatValue() );
    }
}
