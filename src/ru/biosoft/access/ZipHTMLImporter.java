package ru.biosoft.access;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.jobcontrol.JobControl;

public class ZipHTMLImporter extends FileImporter
{
    public ZipHTMLImporter()
    {
        suffix=".zhtml";
    }
    
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        File result = copyFileToRepository( parent, name, file, jobControl );
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, result.getAbsolutePath());
        return new ZipHtmlDataCollection(parent, properties);
    }    
}
