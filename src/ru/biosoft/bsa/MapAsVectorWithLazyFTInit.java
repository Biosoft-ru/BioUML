
package ru.biosoft.bsa;

import java.util.logging.Level;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.Entry;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.bsa.transformer.EmblTrackTransformer;

/** @todo comment */
public class MapAsVectorWithLazyFTInit extends MapAsVector
{
    /**
     * String constant tp pass FeatureTable content.
     * The content should be <code>Reader</code>.
     */
    public final static String FEATURE_TABLE_CONTENT = "feature-table-content";
    private final DataElementDescriptor descriptor;

    public MapAsVectorWithLazyFTInit(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        descriptor = new DataElementDescriptor(Track.class, true, Collections.singletonMap(Track.SEQUENCES_COLLECTION_PROPERTY, getCompletePath().getParentPath().toString()));
    }

    private boolean isFTInitialized = false;
    private boolean inProcess = false;

    protected void initTracks()
    {
        if( isFTInitialized || inProcess )
            return;

        try
        {
            inProcess = true;
            Entry entry = (Entry)getInfo().getProperties().get(FEATURE_TABLE_CONTENT);
            if( entry != null )
            {
                EmblTrackTransformer trackTransformer = new EmblTrackTransformer();
                trackTransformer.init(null, this);

                char[] cbuff = new char[2];
                entry.getReader().read(cbuff);
                if( !"ID".equalsIgnoreCase(new String(cbuff)) )
                {
                    trackTransformer.setFormat(EmblTrackTransformer.GENBANK_FORMAT);
                }

                Track track = trackTransformer.transformInput(entry);
                put(track);
            }
            inProcess = false;
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not initialise FeatureTable for map " + getName(), t);
        }

        isFTInitialized = true;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Overwride DataCollection methods so initFeatureTable() method was called before
    //

    @Override
    public int getSize()
    {
        initTracks();
        return super.getSize();
    }

    @Override
    public boolean contains(String name)
    {
        initTracks();
        return super.contains(name);
    }

    @Override
    public boolean contains(DataElement element)
    {
        initTracks();
        return super.contains(element);
    }

    @Override
    public Track get(String name)
    {
        initTracks();
        return super.get(name);
    }

    @Override
    public @Nonnull Iterator<Track> iterator()
    {
        initTracks();
        return super.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        initTracks();
        return super.getNameList();
    }

    @Override
    public Track put(Track element) throws DataElementPutException
    {
        if( inProcess )
            super.doPut(element, true);
        else
            super.put(element);

        return null;
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return descriptor;
    }
}
