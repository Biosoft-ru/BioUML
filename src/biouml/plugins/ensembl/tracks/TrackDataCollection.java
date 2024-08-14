package biouml.plugins.ensembl.tracks;

import java.util.logging.Level;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.track.GCContentTrack;
import biouml.model.Module;
import biouml.plugins.ensembl.EnsemblConstants;

public class TrackDataCollection extends ReadOnlyVectorCollection<Track>
{
    public static final String TRACK_DC_NAME = "Tracks";
    private DataElementDescriptor descriptor;

    public TrackDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        elements = new LinkedHashMap<>();
    }

    @Override
    protected void doInit()
    {
        try
        {
            Track karyotypeTrack = new KaryotypeTrack(this);
            doPut(karyotypeTrack, true);
            
            Track gcContentTrack = new GCContentTrack(this);
            doPut(gcContentTrack, true);

            Track geneTrack = new GeneTrack(this);
            doPut(geneTrack, true);

            Track extendedGeneTrack = new ExtendedGeneTrack(this);
            doPut(extendedGeneTrack, true);
            
            Track repeatTrack = new RepeatTrack(this);
            doPut(repeatTrack, true);

            String variationDb = Module.getModule(this).getInfo().getProperties().getProperty(EnsemblConstants.VARIATION_DB_PROPERTY, getInfo().getProperty(EnsemblConstants.VARIATION_DB_PROPERTY));
            if(variationDb != null)
            {
                Track variationTrack = new VariationTrack(this, false, variationDb);
                doPut(variationTrack, true);
            }
            
            Track transcriptsTrack = new TranscriptsTrack( this );
            doPut( transcriptsTrack, true );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not init tracks", e);
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        init();
        if(descriptor == null)
        {
            try
            {
                descriptor = new DataElementDescriptor(Track.class, true, Collections.singletonMap(Track.SEQUENCES_COLLECTION_PROPERTY,
                        TrackUtils.getPrimarySequencesPath(Module.getModulePath(this)).toString()));
            }
            catch( Exception e )
            {
                descriptor = new DataElementDescriptor(Track.class, true, null);
            }
        }
        return descriptor;
    }
}
