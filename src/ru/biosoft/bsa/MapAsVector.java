package ru.biosoft.bsa;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @todo comment
 */
public class MapAsVector extends VectorDataCollection<Track> implements AnnotatedSequence
{
    public MapAsVector(String name, DataCollection parent, Sequence sequence, Properties properties)
    {
        super(name, parent, properties);

        this.sequence = sequence;
        if(sequence instanceof SequenceSupport)
        {
            ((SequenceSupport)sequence).setOrigin(this);
        }
    }

    public MapAsVector(DataCollection parent, Properties properties)
    {
        super(parent, properties);
        sequence = (Sequence)properties.get(SITE_SEQUENCE_PROPERTY);
        properties.remove(SITE_SEQUENCE_PROPERTY);
        if(sequence instanceof SequenceSupport)
        {
            ((SequenceSupport)sequence).setOrigin(this);
        }
    }

    /**
     * Returns the type of DataElements stored in the data collection.
     * @return Type of DataElements stored in the data collection.
     */
    @Override
    public @Nonnull Class<Track> getDataElementType()
    {
        return Track.class;
    }

    /** The site sequence. */
    protected Sequence sequence;
    @Override
    public Sequence getSequence()
    {
        return sequence;
    }
    
    private DynamicPropertySet properties = null;
    @Override
    final public DynamicPropertySet getProperties()
    {
        if(properties == null)
            properties = new DynamicPropertySetAsMap();

        return properties;
    }
}
