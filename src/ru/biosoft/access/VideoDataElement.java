package ru.biosoft.access;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.ExProperties;


@ClassIcon ( "resources/video.png" )
@PropertyName("video")
public class VideoDataElement extends DataElementSupport
{

    public static final Logger log = Logger.getLogger(VideoDataElement.class.getName());
    private File file;
    Properties properties;

    public VideoDataElement(String name, DataCollection origin, File file)
    {
        super(name, origin);
        this.file = file;
        properties = new ExProperties();
        detectFormat();
    }

    public File getFile()
    {
        return file;
    }

    @PropertyName("Format")
    public String getFormat()
    {
        return properties.getProperty("format");
    }

    public void setFormat(String format)
    {
        properties.put("format", format);
    }

    private void detectFormat()
    {
        String format = "mp4"; // default format
        Matcher m = Pattern.compile(".+(\\.\\w+)").matcher(file.getName());
        if ( m.find() )
        {
            format = m.group(1).substring(1);
        }
        setFormat(format);
    }

    @PropertyName("Description") @PropertyDescription("Video description (can be empty)")
    public String getDescription()
    {
        return properties.getProperty("description");
    }

    public void setDescription(String description)
    {
        properties.put("description", description);
    }

    public String getWidthStr()
    {
        return properties.getProperty("width");
    }

    public void setWidthStr(String width)
    {
        properties.put("width", width);
    }

    public String getHeightStr()
    {
        return properties.getProperty("height");
    }

    public void setHeightStr(String height)
    {
        properties.put("height", height);
    }

    public File getPropertiesFile(File baseFile)
    {
        if ( baseFile == null )
            return null;
        String format = getFormat();
        String name = baseFile.getName();
        if ( name.endsWith(format) )
            name = name.substring(0, name.length() - format.length());
        name = name + ".properties";
        return new File(baseFile.getParentFile(), name);
    }

    public void storeProperties(File propFile)
    {
        try
        {
            ExProperties.store(properties, propFile);
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Can not store properties for element " + getCompletePath().toString(), e);
        }
    }

    public void storeProperties()
    {
        File propFile = getPropertiesFile(file);
        storeProperties(propFile);
    }

    public void loadProperties()
    {
        File propFile = getPropertiesFile(file);
        try
        {
            if ( propFile.exists() )
            {
                try (FileReader reader = new FileReader(propFile))
                {
                    properties.load(reader);
                }
            }
        }
        catch (Exception e)
        {
            log.log(Level.WARNING, "Can not load properties for element " + getCompletePath().toString(), e);
        }
    }
}
