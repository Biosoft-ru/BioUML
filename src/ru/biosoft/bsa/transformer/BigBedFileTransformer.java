package ru.biosoft.bsa.transformer;

import java.io.File;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.PriorityTransformer;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.bsa.track.big.BigTrack;
import ru.biosoft.bsa.track.big.BigWigTrack;

public class BigBedFileTransformer extends AbstractFileTransformer<BigBedTrack> implements PriorityTransformer
{

    @Override
    public Class<? extends BigBedTrack> getOutputType()
    {
        return (Class<? extends BigBedTrack>)BigBedTrack.class;
    }

    @Override
    public BigBedTrack<Site> load(File input, String name, DataCollection<BigBedTrack> origin) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( BigTrack.PROP_BIGBED_PATH, input.getAbsolutePath() );
        return new BigBedTrack( origin, properties );
    }

    @Override
    public void save(File output, BigBedTrack element) throws Exception
    {
        ApplicationUtils.linkOrCopyFile( output, new File(element.getFilePath()), null );
    }

    @Override
    public int getInputPriority(Class<? extends DataElement> inputClass, DataElement output)
    {
        return -1;
    }

    @Override
    public int getOutputPriority(String name)
    {
        if(name.toLowerCase().endsWith( ".bb" ))
            return 2;
        return 0;
    }

}
