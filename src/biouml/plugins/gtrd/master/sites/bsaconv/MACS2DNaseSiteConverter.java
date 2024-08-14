package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.dnase.MACS2DNasePeak;

public class MACS2DNaseSiteConverter extends DNaseSiteConverter<MACS2DNasePeak>
{
    public MACS2DNaseSiteConverter(DNaseExperiment exp)
    {
        super( exp );
    }
    
    @Override
    protected MACS2DNasePeak createPeak()
    {
        return new MACS2DNasePeak();
    }
    
    @Override
    protected void updatePeakFromSiteProperties(MACS2DNasePeak peak, DynamicPropertySet dps)
    {
        peak.setFoldEnrichment( ( (Number)dps.getValue( "fold_enrichment" ) ).floatValue() );
        peak.setMLog10PValue( ( (Number)dps.getValue( "-log10(pvalue)" ) ).floatValue() );
        peak.setMLog10QValue( ( (Number)dps.getValue( "-log10(qvalue)" ) ).floatValue() );
        peak.setSummit( (Integer)dps.getValue( "abs_summit" ) - peak.getFrom() + 1 );
        peak.setPileup( ((Number)dps.getValue( "pileup" )).floatValue() );            
    }
    
}