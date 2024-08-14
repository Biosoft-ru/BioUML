package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.BCFFileTrack;
import ru.biosoft.bsa.Track;

public class BCFFileTransformer extends AbstractFileTransformer<BCFFileTrack> implements PriorityTransformer
{
    @Override
    public Class<? extends BCFFileTrack> getOutputType()
    {
        return BCFFileTrack.class;
    }

    @Override
    public BCFFileTrack load(File input, String name, DataCollection<BCFFileTrack> origin) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, input.getAbsolutePath() );
        
        String configDir = origin.getInfo().getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        properties.setProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, configDir );
        
        String seqBase = origin.getInfo().getProperty( Track.SEQUENCES_COLLECTION_PROPERTY );
        if(seqBase != null)
            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, seqBase );
        
        return new BCFFileTrack( origin, properties );
    }

    @Override
    public void save(File output, BCFFileTrack element) throws Exception
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
        if(name.toLowerCase().endsWith( ".bcf" ))
            return 2;
        return 0;
    }
}
