package ru.biosoft.bsa.transformer;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.track.BedTrack;

public class BedFileTransformer extends AbstractFileTransformer<BedTrack>
{

    @Override
    public Class<? extends BedTrack> getOutputType()
    {
        return BedTrack.class;
    }

    @Override
    public BedTrack load(File file, String name, DataCollection<BedTrack> origin) throws Exception
    {
        return new BedTrack( origin, name, file );
    }

    @Override
    public void save(File output, BedTrack element) throws Exception
    {
        if(element.getFile().equals( output ))
            return;
        ApplicationUtils.linkOrCopyFile( output, element.getFile(), null );
    }
    
}