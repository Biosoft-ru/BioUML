package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import ru.biosoft.bsa.track.big.BigBedTrack;

public abstract class BedEntryToDNasePeak<P extends DNasePeak> extends BedEntryToPeak<P, DNaseExperiment>
{
    public static final String PROP_REPLICATE = PROP_PREFIX + "Replicate";
    protected int replicate;
    
    protected BedEntryToDNasePeak(BigBedTrack<?> origin, Properties props)
    {
        super( origin, props );
        replicate = Integer.parseInt( props.getProperty( PROP_REPLICATE ) );
    }
    
    @Override
    protected void initPeak(P peak)
    {
        peak.setReplicate( replicate );
    }
   
    @Override
    protected DNaseExperiment getExpFromMetadata(Metadata meta, String expId)
    {
        return meta.dnaseExperiments.get( experimentId );
    }
    
    @Override
    protected DNaseExperiment createStubExp()
    {
        String name = experimentId == null ? "DEXP??????" : experimentId;
        return new DNaseExperiment( null, name );
    }
}
