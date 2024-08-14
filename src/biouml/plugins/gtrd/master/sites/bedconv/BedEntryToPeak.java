package biouml.plugins.gtrd.master.sites.bedconv;

import java.util.Properties;

import org.jetbrains.bio.big.BedEntry;

import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.sites.Peak;
import biouml.plugins.gtrd.master.utils.StringPool;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil;

public abstract class BedEntryToPeak<P extends Peak<E>, E extends Experiment> implements  BedEntryConverter<P>
{
    public static final String PROP_METADATA_PATH = PROP_PREFIX + "MetadataPath";
    public static final String PROP_EXPERIMENTS_PATH = PROP_PREFIX + "ExperimentsPath";
    public static final String PROP_EXPERIMENT_ID = PROP_PREFIX + "ExperimentId";
    
    private BigBedTrack<?> origin;
    
    //Peaks can be linked either to biouml.plugins.gtrd.master.meta.Metadata or to collection of Experiments
    protected DataElementPath metadataPath;
    protected DataElementPath experimentsPath;
    protected String experimentId;
    protected E exp;
    
    protected BedEntryToPeak(BigBedTrack<?> origin, Properties props)
    {
        this.origin = origin;
        String value = props.getProperty( PROP_METADATA_PATH );
        if(value != null)
            metadataPath = DataElementPath.create( value );
        value = props.getProperty( PROP_EXPERIMENTS_PATH );
        if(value != null)
            experimentsPath = DataElementPath.create( value );
        experimentId = props.getProperty( PROP_EXPERIMENT_ID );
    }
    
    protected abstract P createPeak();
    protected void initPeak(P peak) {};
    protected abstract void updatePeakFromColumns(P peak, String[] columns);
    
    
    @Override
    public P fromBedEntry(BedEntry e)
    {
        P peak = createPeak();
        initPeak(peak);
        
        String chr = e.getChrom();
        peak.setChr( StringPool.get(chr) );
        peak.setFrom( e.getStart() + 1 );
        peak.setTo( e.getEnd() );
        peak.setOrigin( origin );
        
        if(exp == null)
            initExp();
        peak.setExp( exp  );
        
        String[] parts = TextUtil.split(e.getRest(), '\t');
        updatePeakFromColumns( peak, parts );
        
        //TODO: export id to .bb file
        if(peak.getId() == 0)//not set
            peak.setId( peak.getFrom() );
       
        return peak;
    }
    
    protected abstract String createRestStringFromPeak(P peak);

    @Override
    public BedEntry toBedEntry(P peak)
    {
        String rest = createRestStringFromPeak(peak);
        return new BedEntry( peak.getChr(), peak.getFrom() - 1, peak.getTo(), rest );
    }

    private void initExp()
    {
        if(experimentId == null)
            exp = createStubExp();
        else if(metadataPath != null)
        {
            Metadata meta = metadataPath.getDataElement( Metadata.class );
            exp = getExpFromMetadata(meta, experimentId);
        }else if(experimentsPath != null)
        {
            exp = (E)experimentsPath.getChildPath( experimentId ).getDataElement( );
        }else
        {
            exp = createStubExp();
        }
    }

    protected abstract E getExpFromMetadata(Metadata meta, String expId);
    
    protected E createStubExp() { throw new UnsupportedOperationException(); }

}
