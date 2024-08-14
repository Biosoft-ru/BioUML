package biouml.plugins.gtrd.master.sites.bsaconv;

import java.util.function.Function;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.master.sites.chipseq.GEMPeak;

public class UnionGEMChIPSeqSiteConverter extends UnionChIPSeqSiteConverter<GEMPeak>
{
    public UnionGEMChIPSeqSiteConverter(Function<String, ChIPseqExperiment> expSupplier)
    {
        super( expSupplier );
    }

    @Override
    protected GEMPeak createPeak()
    {
        return new GEMPeak();
    }

    @Override
    protected void updatePeakFromSiteProperties(GEMPeak peak, DynamicPropertySet dps)
    {
        super.updatePeakFromSiteProperties( peak, dps );
        peak.setControl( ( (Number)dps.getValue( "Control" ) ).floatValue() );
        peak.setExpected( ( (Number)dps.getValue( "Expectd" ) ).floatValue() );
        peak.setFold( ( (Number)dps.getValue( "Fold" ) ).floatValue() );
        peak.setIp( ( (Number)dps.getValue( "IP" ) ).floatValue() );
        peak.setIpVsEmp( ( (Number)dps.getValue( "IPvsEMP" ) ).floatValue() );
        peak.setNoise( ( (Number)dps.getValue( "Noise" ) ).floatValue() );
        peak.setPMLog10( ( (Number)dps.getValue( "P_-lg10" ) ).floatValue() );
        peak.setPPoiss( ( (Number)dps.getValue( "P_poiss" ) ).floatValue() );
        peak.setQMLog10( ( (Number)dps.getValue( "Q_-lg10" ) ).floatValue() );
    }
}
