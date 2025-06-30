package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.PriorityTransformer;
import ru.biosoft.access.core.PropertiesHolder;
import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.hic.HICTrack;

public class HICFileTransformer extends AbstractFileTransformer<HICTrack> implements PriorityTransformer, PropertiesHolder
{

    private Properties properties;

    @Override
    public Class<? extends HICTrack> getOutputType()
    {
        return (Class<? extends HICTrack>) HICTrack.class;
    }

    @Override
    public HICTrack load(File input, String name, DataCollection<HICTrack> origin) throws Exception
    {
        Properties trackProperties = new Properties();
        if( properties != null )
            trackProperties.putAll( properties );
        trackProperties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        trackProperties.setProperty( HICTrack.PROP_HIC_PATH, input.getAbsolutePath() );
        return new HICTrack( origin, trackProperties );
    }

    @Override
    public void save(File output, HICTrack element) throws Exception
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
        if( name.toLowerCase().endsWith( ".hic" ) )
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
