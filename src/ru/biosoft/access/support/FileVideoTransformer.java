package ru.biosoft.access.support;

import java.io.File;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.VideoDataElement;
import ru.biosoft.access.core.DataCollection;

public class FileVideoTransformer extends AbstractFileTransformer<VideoDataElement>
{
    @Override
    public Class<? extends VideoDataElement> getOutputType()
    {
        return VideoDataElement.class;
    }

    @Override
    public VideoDataElement load(File file, String name, DataCollection<VideoDataElement> origin) throws Exception
    {
        VideoDataElement de = new VideoDataElement(name, origin, file);
        de.loadProperties();
        return de;
    }

    @Override
    public void save(File output, VideoDataElement element) throws Exception
    {
        if ( element.getFile().equals(output) )
            return;
        ApplicationUtils.linkOrCopyFile(output, element.getFile(), null);
        element.storeProperties(element.getPropertiesFile(output));
    }
}
