package ru.biosoft.access;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

public class VideoFileImporter implements DataElementImporter
{

    private static final Pattern EXTENSION_REGEXP = Pattern.compile("\\.(mp4|ogg|webm)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESOLUTION_REGEXP = Pattern.compile("(\\d+) x (\\d+)", Pattern.CASE_INSENSITIVE);
    protected VideoFileImportProperties importProperties = new VideoFileImportProperties();

    @Override
    public int accept(DataCollection parent, File file)
    {
        if ( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, FileDataElement.class) )
            return ACCEPT_UNSUPPORTED;
        if ( file == null )
            return ACCEPT_LOW_PRIORITY;
        if ( EXTENSION_REGEXP.matcher(file.getName()).find() )
            return ACCEPT_HIGH_PRIORITY;
        return ACCEPT_UNSUPPORTED;
    }

    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl)
            throws IOException
    {
        File result = copyFileToRepository(parent, name, file, jobControl);
        VideoDataElement de = new VideoDataElement(name, parent, result);
        de.setDescription(importProperties.getDescription().trim());
        Matcher m = RESOLUTION_REGEXP.matcher(importProperties.getResolution());
        if ( m.find() )
        {
            de.setWidthStr(m.group(1));
            de.setHeightStr(m.group(2));
        }
        de.storeProperties();
        return de;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return VideoDataElement.class;
    }

    @Override
    public boolean init(Properties properties)
    {
        String description = properties.getProperty("Video title");
        if ( description != null )
            importProperties.setDescription(description);
        String resolution = properties.getProperty("Resolution");
        if ( resolution != null )
            importProperties.setResolution(resolution);

        return true;
    }

    @Override
    public VideoFileImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        if ( importProperties == null )
            importProperties = new VideoFileImportProperties();
        return importProperties;
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName,
            FunctionJobControl jobControl, Logger log) throws Exception
    {
        if ( jobControl != null )
        {
            jobControl.functionStarted();
        }
        String name = elementName;
        if ( name == null || name.equals("") )
            name = file.getName();

        Matcher m = Pattern.compile(".+(\\.\\w+)").matcher(file.getName());
        if ( m.matches() && !name.endsWith(m.group(1)) )
        {
            name += m.group(1);
        }

        name = name.replaceAll("\\/", "");
        if ( parent.contains(name) )
        {
            parent.remove(name);
        }
        try
        {
            DataElement de = createElement(parent, name, file, jobControl);
            parent.put(de);
        }
        catch (Exception e)
        {
            if ( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
                return null;
            }
            throw e;
        }
        if ( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return parent.get(name);
    }

    protected File copyFileToRepository(DataCollection parent, String name, File file, JobControl jobControl)
            throws IOException
    {
        File result = null;
        try
        {
            result = DataCollectionUtils.getChildFile(parent, name);
        }
        catch (ClassCastException e)
        {
        }
        if ( result != null )
            try
            {
                ApplicationUtils.linkOrCopyFile(result, file, jobControl);
            }
            catch (IOException e)
            {
                result.delete();
                throw e;
            }
        else
            result = file;
        return result;
    }

    public static class VideoFileImportProperties extends OptionEx
    {
        private String description = "";
        private String resolution = "640 x 480 (Standart Definition)";

        @PropertyName("Video title") 
        @PropertyDescription("Video description (can be empty)")
        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            String oldValue = this.description;
            this.description = description;
            firePropertyChange("description", oldValue, description);
        }

        @PropertyName("Resolution") 
        @PropertyDescription("Video resolution")
        public String getResolution()
        {
            return resolution;
        }

        public void setResolution(String resolution)
        {
            String oldValue = this.resolution;
            this.resolution = resolution;
            firePropertyChange("resolution", oldValue, this.resolution);
        }

        public StreamEx<String> getPossibleResolutions()
        {
            return StreamEx.of("640 x 480 (Standart Definition)", "1280 x 720 (High definition)",
                    "1920 x 1080 (Full HD)");
        }
    }

    public static class VideoFileImportPropertiesBeanInfo extends BeanInfoEx2<VideoFileImportProperties>
    {
        public VideoFileImportPropertiesBeanInfo()
        {
            super(VideoFileImportProperties.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("description");
            addWithTags("resolution", VideoFileImportProperties::getPossibleResolutions);
        }
    }

}
