
package ru.biosoft.access.support;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.html.ZipHtmlDataCollection;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @author lan
 * Transformer for zipped-html files
 */
public class FileZipHtmlTransformer extends AbstractTransformer<FileDataElement, ZipHtmlDataCollection>
{
    @Override
    public Class<FileDataElement> getInputType()
    {
        return FileDataElement.class;
    }

    @Override
    public Class<ZipHtmlDataCollection> getOutputType()
    {
        return ZipHtmlDataCollection.class;
    }

    @Override
    public ZipHtmlDataCollection transformInput(FileDataElement fileElement) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, fileElement.getName());
        properties.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, fileElement.getFile().getAbsolutePath());
        return new ZipHtmlDataCollection(fileElement.getOrigin(), properties);
    }

    @Override
    public FileDataElement transformOutput(ZipHtmlDataCollection zdc) throws Exception
    {
        FileDataElement fde = new FileDataElement(zdc.getName(), getPrimaryCollection().cast( FileBasedCollection.class ));
        File srcFile = new File(zdc.getInfo().getProperty(DataCollectionConfigConstants.FILE_PROPERTY));
        if(!srcFile.equals(fde.getFile()))
            ApplicationUtils.copyFile(fde.getFile(), srcFile, null);
        return fde;
    }

}
