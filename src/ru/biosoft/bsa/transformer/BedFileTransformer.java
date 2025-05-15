package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.PropertiesHolder;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.BedTrack;

public class BedFileTransformer extends AbstractFileTransformer<BedTrack> implements PropertiesHolder
{

    private Properties properties;

    @Override
    public Class<? extends BedTrack> getOutputType()
    {
        return BedTrack.class;
    }

    @Override
    public BedTrack load(File file, String name, DataCollection<BedTrack> origin) throws Exception
    {
        Properties trackProps = properties != null ? (Properties) properties.clone() : new Properties();
        trackProps.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        trackProps.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, file.getAbsolutePath() );
        return new BedTrack( origin, trackProps );
    }

    @Override
    public void save(File output, BedTrack element) throws Exception
    {
        if(element.getFile().equals( output ))
            return;
        ApplicationUtils.linkOrCopyFile( output, element.getFile(), null );
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