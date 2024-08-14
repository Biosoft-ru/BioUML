package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.MACS2ChIPSeqPeak;

public class UnionMACS2ChIPSeqSiteConverter extends UnionChIPSeqSiteConverter<MACS2ChIPSeqPeak>
{
    public UnionMACS2ChIPSeqSiteConverter(Function<String, ChIPseqExperiment> expSupplier)
    {
        super( expSupplier );
    }

    @Override
    protected MACS2ChIPSeqPeak createPeak()
    {
        return new MACS2ChIPSeqPeak();
    }

    @Override
    protected void updatePeakFromSiteProperties(MACS2ChIPSeqPeak peak, DynamicPropertySet dps)
    {
        super.updatePeakFromSiteProperties( peak, dps );
        peak.setFoldEnrichment( ( (Number)dps.getValue( "fold_enrichment" ) ).floatValue() );
        peak.setMLog10PValue( ( (Number)dps.getValue( "-log10(pvalue)" ) ).floatValue() );
        peak.setMLog10QValue( ( (Number)dps.getValue( "-log10(qvalue)" ) ).floatValue() );
        peak.setSummit( (Integer)dps.getValue( "abs_summit" ) + 1 - peak.getFrom() );
        peak.setPileup( ((Number)dps.getValue( "pileup" )).floatValue() );
    }
}
