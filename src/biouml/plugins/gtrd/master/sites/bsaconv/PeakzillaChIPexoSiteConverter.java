package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeakzillaPeak;

public class PeakzillaChIPexoSiteConverter extends ChIPexoSiteConverter<ChIPexoPeakzillaPeak>
{
    public PeakzillaChIPexoSiteConverter(ChIPexoExperiment exp)
    {
        super( exp );
    }
    
    @Override
    protected ChIPexoPeakzillaPeak createPeak()
    {
        return new ChIPexoPeakzillaPeak();
    }
    
    @Override
    protected void updatePeakFromSiteProperties(ChIPexoPeakzillaPeak peak, DynamicPropertySet dps)
    {
        peak.setChip( ( (Number)dps.getValue( "Chip" ) ).floatValue() );
        peak.setControl( ( (Number)dps.getValue( "Control" ) ).floatValue() );
        peak.setDistributionScore( ( (Number)dps.getValue( "DistributionScore" ) ).floatValue() );
        peak.setFdr( ( (Number)dps.getValue( "FDR" ) ).floatValue() );
        peak.setFoldEnrichment( ( (Number)dps.getValue( "FoldEnrichment" ) ).floatValue() );
        peak.setPeakZillaScore( ( (Number)dps.getValue( "Score" ) ).floatValue() );
        peak.setSummit( ( (Number)dps.getValue( "Summit" ) ).intValue() - peak.getFrom() );
    }
    
}