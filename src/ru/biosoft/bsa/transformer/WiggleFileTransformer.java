package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.file.FileTypePriority;
import ru.biosoft.access.core.PropertiesHolder;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.WiggleFileTrack;

public class WiggleFileTransformer extends AbstractFileTransformer<WiggleFileTrack> implements PriorityTransformer, PropertiesHolder
{
    private Properties properties;

    @Override
    public Class<? extends WiggleFileTrack> getOutputType()
    {
        return WiggleFileTrack.class;
    }

    @Override
    public WiggleFileTrack load(File input, String name, DataCollection<WiggleFileTrack> origin) throws Exception
    {
        Properties trackProps = properties != null ? (Properties) properties.clone() : new Properties();
        trackProps.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        trackProps.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, input.getAbsolutePath());
        return new WiggleFileTrack(origin, trackProps);
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
        newProps.setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, "");
        newProps.setProperty(ChrNameMapping.PROP_CHR_MAPPING, "");
        return newProps;
    }

    @Override
    public Map<String, FileTypePriority> getExtensionPriority()
    {
        Map<String, FileTypePriority> extToProprity = new HashMap<>();
        extToProprity.put( "wig", FileTypePriority.HIGH_PRIORITY );
        return extToProprity;
    }
}
