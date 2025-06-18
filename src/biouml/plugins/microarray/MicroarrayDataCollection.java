package biouml.plugins.microarray;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.SimpleFileFilter;
import ru.biosoft.access.core.VectorDataCollection;

import com.developmentontheedge.application.ApplicationUtils;


public class MicroarrayDataCollection extends VectorDataCollection
{
    public final static String FILTER = "fac";

    /** Property for storing filter file extension  */
    public static final String FILE_FILTER = "fileFilter";

    /** Subdirectory corresponded with this collection. */
    private final File root;

    public MicroarrayDataCollection(DataCollection parent, Properties properties)
    {
        super(parent, properties);
        String file = properties.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
        if( file == null )
            file = properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
        root = new File(file);
        init(properties);
    }

    protected void init(Properties properties)
    {
        FileFilter filter = new SimpleFileFilter(properties.getProperty(FILE_FILTER));
        File[] files = root.listFiles(filter);
        if( files != null )
        {
            try
            {
                for( File file : files )
                {
                    if( filter.accept(file) )
                    {
                        String name = file.getName();
                        int index = name.lastIndexOf(".");
                        if( 0 < index )
                            name = name.substring(0, index);
                        super.doPut(new FileDataElement(name, this, file), true);
                        getInfo().addUsedFile(file);
                    }
                }
            }
            catch( Exception exc )
            {
                log.log(Level.SEVERE, "Error during file collection creating root=" + root, exc);
                throw new RuntimeException("FileCollection failed root=" + root, exc);
            }
        }
    }
    @Override
    public @Nonnull Class<? extends FileDataElement> getDataElementType()
    {
        return FileDataElement.class;
    }

    public File getFile()
    {
        return root;
    }

    @Override
    public void doRemove(String name) throws Exception
    {
        File file = new File(root, name + MessageBundle.getMessage("MICROARRAY_FILE_EXT"));
        if( !file.delete() )
            throw new Exception("File can not be destroyed: " + file.getAbsolutePath());
        super.doRemove(name);
    }

    public String getRoot()
    {
        String rootPath = root.getPath();
        if( rootPath.charAt(0) == '\\' )
        {
            rootPath = System.getProperty("user.dir") + rootPath.substring(1);
        }
        return rootPath;
    }

    @Override
    protected DataElement doGet(String name)
    {
        File file = new File(root, name + MessageBundle.getMessage("MICROARRAY_FILE_EXT"));
        if( file.exists() )
        {
            return new Entry(this, name, file, 0, file.length());
        }
        return null;
    }

    @Override
    protected void doPut(DataElement obj, boolean isNew)
    {
        Entry entry = (Entry)obj;
        try
        {
            File dst = new File(getRoot() + System.getProperty("file.separator") + entry.getName()
                    + MessageBundle.getMessage("MICROARRAY_FILE_EXT"));
            ApplicationUtils.writeString(dst, entry.getData());
            super.doPut(entry, isNew);
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Import microarray error", e);
        }
    }
}
