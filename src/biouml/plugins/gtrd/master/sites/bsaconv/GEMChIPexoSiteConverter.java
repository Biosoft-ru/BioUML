package biouml.plugins.gtrd.master.sites.bsaconv;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoGEMPeak;

public class GEMChIPexoSiteConverter extends ChIPexoSiteConverter<ChIPexoGEMPeak>
{
    public GEMChIPexoSiteConverter(ChIPexoExperiment exp)
    {
        super( exp );
    }
    
    @Override
    protected ChIPexoGEMPeak createPeak()
    {
        return new ChIPexoGEMPeak();
    }
    
    @Override
    protected void updatePeakFromSiteProperties(ChIPexoGEMPeak peak, DynamicPropertySet dps)
    {
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