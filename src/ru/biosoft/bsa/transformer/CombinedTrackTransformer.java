package ru.biosoft.bsa.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.combined.CombinedTrack;
import ru.biosoft.util.BeanUtil;

public class CombinedTrackTransformer extends AbstractFileTransformer<CombinedTrack>
{

    @Override
    public Class<? extends CombinedTrack> getOutputType()
    {
        return CombinedTrack.class;
    }

    @Override
    public CombinedTrack load(File input, String name, DataCollection<CombinedTrack> origin) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( input ))
        {
            CombinedTrack track = new CombinedTrack( origin, name );
            Properties properties = new Properties();
            properties.load( fis );
            fis.close();
            BeanUtil.readBeanFromProperties( track, properties, "" );
            return track;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public void save(File output, CombinedTrack element) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( output ))
        {
            Properties properties = new Properties();
            BeanUtil.writeBeanToProperties( element, properties, "" );
            properties.store( fos, null );
            fos.close();
        }

    }

}
