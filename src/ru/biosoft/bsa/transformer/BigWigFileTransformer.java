package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PropertiesHolder;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.big.BigTrack;
import ru.biosoft.bsa.track.big.BigWigTrack;

public class BigWigFileTransformer extends AbstractFileTransformer<BigWigTrack> implements PriorityTransformer, PropertiesHolder
{
    private Properties properties;

    @Override
    public Class<? extends BigWigTrack> getOutputType()
    {
        return BigWigTrack.class;
    }

    @Override
    public BigWigTrack load(File input, String name, DataCollection<BigWigTrack> origin) throws Exception
    {
        Properties trackProperties = new Properties();
        if( properties != null )
            trackProperties.putAll( properties );
        trackProperties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        trackProperties.setProperty( BigTrack.PROP_BIGBED_PATH, input.getAbsolutePath() );
        return new BigWigTrack( origin, trackProperties );
    }

    @Override
    public void save(File output, BigWigTrack element) throws Exception
    {
        ApplicationUtils.linkOrCopyFile( output, new File(element.getFilePath()), null );
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return 1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(name.toLowerCase().endsWith( ".bw" ))
            return 2;
        return 0;
    }

    @Override
    public Properties getProperties()
    {
        return properties;
    }

    @Override
    public void setProperties(Properties props)
    {
        properties = props;
    }

    @Override
    public Properties createProperties()
    {
        Properties newProps = new Properties();
        newProps.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, "" );
        newProps.setProperty( ChrNameMapping.PROP_CHR_MAPPING, "" );
        return newProps;
    }
}
