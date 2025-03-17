package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.WiggleFileTrack;

public class WiggleFileTransformer extends AbstractFileTransformer<WiggleFileTrack> implements PriorityTransformer
{

    @Override
    public Class<? extends WiggleFileTrack> getOutputType()
    {
        return WiggleFileTrack.class;
    }

    @Override
    public WiggleFileTrack load(File input, String name, DataCollection<WiggleFileTrack> origin) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, input.getAbsolutePath() );
        
        String configDir = origin.getInfo().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configDir );
        
        String seqBase = origin.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
        if(seqBase != null)
            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, seqBase );
        
        return new WiggleFileTrack(origin, properties);
    }

    @Override
    public void save(File output, WiggleFileTrack element) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return -1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if( name.toLowerCase().endsWith(".wig") )
            return 2;
        return 0;
    }
}
