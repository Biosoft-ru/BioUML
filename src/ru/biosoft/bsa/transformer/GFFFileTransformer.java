package ru.biosoft.bsa.transformer;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.GFFTrack;

public class GFFFileTransformer extends AbstractFileTransformer<GFFTrack>
{
    @Override
    public Class<? extends GFFTrack> getOutputType()
    {
        return GFFTrack.class;
    }

    @Override
    public GFFTrack load(File file, String name, DataCollection<GFFTrack> origin) throws Exception
    {
        return new GFFTrack( origin, name, file );
    }

    @Override
    public void save(File output, GFFTrack element) throws Exception
    {
        if( element.getFile().equals( output ) )
            return;
        ApplicationUtils.linkOrCopyFile( output, element.getFile(), null );
    }
}
