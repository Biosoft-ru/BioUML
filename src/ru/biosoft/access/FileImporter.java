package ru.biosoft.access;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import one.util.streamex.IntStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.file.FileDataCollection;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.access.support.FileImageTransformer;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.OptionEx;

public class FileImporter implements DataElementImporter
{
    protected FileImportProperties importProperties = new FileImportProperties();
    protected String suffix;
    public static final String PRESERVE_EXTENSION_PROPERTY = "preserveExtension";

    //kept only for compatibility with genexplain code
    protected boolean detectType()
    {
        return false;
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if(parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, FileDataElement.class)) return ACCEPT_UNSUPPORTED;
        if(file == null) return ACCEPT_LOW_PRIORITY;
        if(DataCollectionUtils.isFileAccepted(parent, file) )
        {
            if(parent instanceof FileDataCollection)
                return suffix == null ? ACCEPT_HIGHEST_PRIORITY : ACCEPT_UNSUPPORTED; //Allow only 'GenericFile' importer for FileDataCollection
            if(suffix != null)
            {
                return file.getName().toLowerCase().endsWith(suffix) ? ACCEPT_HIGHEST_PRIORITY : ACCEPT_LOW_PRIORITY;
            }
            return file.getName().toLowerCase().endsWith(".pdf") ? ACCEPT_HIGHEST_PRIORITY : ACCEPT_LOW_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }
    
    protected DataElement createElement(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        File result = copyFileToRepository( parent, name, file, jobControl );
        if( file.getName().endsWith(".zhtml") )
        {
            Properties properties = new Properties();
            properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
            properties.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, result.getAbsolutePath());
            return new ZipHtmlDataCollection(parent, properties);
        } else
        {
            return new FileDataElement(name, parent, result);
        }
    }

    protected File copyFileToRepository(DataCollection parent, String name, File file, JobControl jobControl) throws IOException
    {
        File result = null;
        try
        {
            result = DataCollectionUtils.getChildFile( parent, name );
        }
        catch( ClassCastException e )
        {
        }
        if(result != null)
            try
            {
                ApplicationUtils.linkOrCopyFile(result, file, jobControl);
            }
            catch( IOException e )
            {
                result.delete();
                throw e;
            }
        else
            result = file;
        return result;
    }
    

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        String name = elementName;
        if( name == null || name.equals("") )
            name = file.getName();
        else if(importProperties.isPreserveExtension())
        {
            Matcher m = Pattern.compile(".+(\\.\\w+)").matcher(file.getName());
            if( m.matches() && !name.endsWith(m.group(1)) )
            {
                name += m.group(1);
            }
        }
        name = name.replaceAll("\\/", "");
        if( parent.contains(name) )
        {
            parent.remove(name);
        }
        try
        {
            DataElement de = createElement( parent, name, file, jobControl );
            parent.put(de);
        }
        catch( Exception e )
        {
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
                return null;
            }
            throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
        return parent.get(name);
    }

    /**
     * @param file
     * @return
     */
    public static boolean isTextFile(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 255);
            double probBinary = IntStreamEx.ofChars( header )
                    .filter( ch -> !Character.isLetter( ch ) && !( ch >= 32 && ch <= 127 ) && ch != '\n' && ch != '\r' && ch != '\t' )
                    .count();

            if( probBinary / header.length() < 0.3 )
            {
                return true;
            }
        }
        catch( Exception e )
        {
        }
        return false;
    }
    
    @Override
    public boolean init(Properties properties)
    {
        this.suffix = properties.getProperty(SUFFIX);
        String preserveExtension = properties.getProperty(PRESERVE_EXTENSION_PROPERTY);
        if(preserveExtension != null)
            importProperties.setPreserveExtension(Boolean.valueOf(preserveExtension));
        return true;
    }
    
    public static class FileImportProperties extends OptionEx
    {
        private boolean preserveExtension = true;

        public boolean isPreserveExtension()
        {
            return preserveExtension;
        }

        public void setPreserveExtension(boolean preserveExtension)
        {
            boolean oldValue = this.preserveExtension;
            this.preserveExtension = preserveExtension;
            firePropertyChange("preserveExtension", oldValue, preserveExtension);
        }
    }
    
    public static class FileImportPropertiesBeanInfo extends BeanInfoEx
    {
        public FileImportPropertiesBeanInfo()
        {
           super(FileImportProperties.class, MessageBundle.class.getName());
           beanDescriptor.setDisplayName(getResourceString("PN_FILE_IMPORT_PROPERTIES"));
           beanDescriptor.setShortDescription(getResourceString("PD_FILE_IMPORT_PROPERTIES"));
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("preserveExtension", beanClass), getResourceString("PN_PRESERVE_EXTENSION"), getResourceString("PD_PRESERVE_EXTENSION"));
        }
    }

    @Override
    public FileImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        return importProperties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return DataElement.class; // as it can create different elemens (FileDataElement, TextDataElement, etc.)
    }
}
