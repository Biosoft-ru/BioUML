package biouml.plugins.server.access;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.util.Log;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.generic.TransformerRegistry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TempFiles;

/**
 * Define common functions and constants for data excahnage between client and server
 * for AccessService
 */
public class AccessProtocol
{
    protected Logger log = Logger.getLogger( AccessProtocol.class.getName() );
    
    public static final String ACCESS_SERVICE = "access.service";

    public static final String TEXT_TRANSFORMER_NAME = "transformer.text";
    public static final String TEXT_TRANSFORMER_INSTANCE = "transformer.text.instance";

    public static final String SHOW_STATISTICS = "showStatistics";
    public static final String DB_VERSION = "version";
    public static final String UPDATE = "update";
    public static final String DEPENDENCIES = "dependencies";

    public static final String CLIENT_PREFIX = "client-";
    public static final String SERVER_PREFIX = "server-";
    
    //////////////////////////////////////////////
    // Command keys
    //
    /**
     * Argument key, that present data element name.
     */
    public static final String KEY_DE = "de";

    /**
     * Argument key, that present set of data element names.
     */
    public static final String KEY_IDS = "ids";

    /**
     * Argument key, that present data element entry.
     */
    public static final String KEY_ENTRY = "entry";

    /**
     * Argument key, that present data element names.
     */
    public static final String KEY_DEs = "des";

    /**
     * Argument key, that present "from" value.
     */
    public static final String KEY_FROM = "from";

    /**
     * Argument key, that present "to" value.
     */
    public static final String KEY_TO = "to";

    /**
     * Argument key, whether or not return extended information
     */
    public static final String KEY_EXTENDED = "extended";

    /**
     * Argument key, which contains serialized properties object
     */
    public static final String KEY_PROPERTIES = "properties";

    /**
     * Argument key, that present "chunk size" value (namelist can be divided to chunks which are to be sent separately).
     */
    public static final String KEY_CHUNK_SIZE = "chunk";

    /**
     * Argument key, that present "name" value
     */
    public static final String KEY_NAME = "name";
    
    /**
     * Argument key, that present "comment" value
     */
    public static final String KEY_COMMENT = "comment";
    
    /**
     * Argument key which contains transformer class name if applicable
     */
    public static final String KEY_TRANSFORMER_NAME = "transformer";
    
    /**
     * Argument key which contains file content (base64-encoded)
     */
    public static final String KEY_BASE64_CONTENT = "base64content";

    /**
     * Argument key to pass wanted class name of children elements
     */
    public static final String CHILD_CLASS_NAME = "childClassName";

    /**
     * Argument key to pass wanted class name of elements
     */
    public static final String ELEMENT_CLASS_NAME = "elementClassName";

    /**
     * Argument key to pass wanted class name of reference types
     */
    public static final String REFERENCE_TYPE_NAME = "referenceTypeName";

    /**
     * Argument key to pass class name
     */
    public static final String CLASS_NAME = "class";

    /**
     * Argument key to indicate that client wants also all common classes
     */
    public static final String ADD_COMMON_CLASSES = "commonClasses";

    //JSON property names
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_TITLE = "title";
    public static final String PROPERTY_ENABLED = "enabled";
    public static final String PROPERTY_HAS_CHILDREN = "hasChildren";
    public static final String PROPERTY_CLASSES = "classes";
    public static final String PROPERTY_CLASS = "class";
    public static final String PROPERTY_PROTECTION = "protection";
    public static final String PROPERTY_PERMISSION = "permissions";
    public static final String PROPERTY_IMPORT_TYPE = "importType";
    public static final String PROPERTY_NAMES = "names";
    public static final String PROPERTY_ICONS = "icons";
    public static final String PROPERTY_TARGET = "target";
    public static final String PROPERTY_ICON = "icon";
    public static final String PROPERTY_SIZE = "size";
    public static final String PROPERTY_FROM = "from";
    public static final String PROPERTY_TO = "to";
    public static final String PROPERTY_FILE = "file";  // whether element can be transferred as file
    public static final String PROPERTY_REFERENCE_TYPE = "referenceType";

    //Import types
    public static final String IMPORT_TYPE_LINK = "link";
    public static final String IMPORT_TYPE_COPY = "copy";

    //////////////////////////////////////////////
    // Access constants
    //

    public static final int DB_CHECK_MUTABLE = 10;

    public static final int DB_DESCRIPTION = 11;

    public static final int DB_LIST_ID = 12;

    public static final int DB_CONTAINS_ENTRY = 13;

    public static final int DB_ENTRY_BY_ID = 14;

    public static final int DB_ENTRIES_SET_BY_ID = 15;

    public static final int DB_SIZE = 16;

    public static final int DB_WRITE_ENTRY = 17;

    public static final int DB_WRITE_ENTRIES_SET = 18;
    
    public static final int DB_REMOVE_ENTRY = 19;

    public static final int DB_GET_DE_SET = 20;

    public static final int DB_GET_INFO = 21;

    public static final int DB_LOGIN = 22;

    public static final int DB_CHECK_PROTECTED = 23;

    public static final int DB_GENERIC_NEW_FOLDER = 25;

    public static final int DB_REMOVE_ELEMENT = 26;

    public static final int DB_GENERIC_RENAME_FOLDER = 27;

    public static final int DB_FLAGGED_LIST = 29;

    public static final int DB_CHECK_PERMISSION = 30;

    public static final int DB_CHECK_ACCEESSIBLE = 31;

    public static final int DB_GET_CLASS_HIERARCHY = 32;
    
    public static final int DB_EXPORT_DC = 33;
    
    public static final int DB_GET_AS_FILE = 34;
    
    public static final int DB_PUT_AS_FILE = 35;

    public static final int DB_PUT_AS_PROPERTIES = 36;
    
    public static final int DB_UPDATE_PROPERTIES = 37;

    public static final int DB_GET_DESCRIPTOR = 38;

    public static final int DB_GET_PERMISSIONS = 39;
    
    public static final int DB_GET_TARGET = 40;

    ///////////////////////////////////////////////////////////////////
    // Utility functions for transfer DataElment as text
    //

    protected <T extends DataElement> Transformer<Entry, T> getTransformer(DataCollection dc) throws Exception
    {
        // try to get already created transformer
        Transformer<Entry, T> transformer = (Transformer<Entry, T>)dc.getInfo().getProperties()
                .get(TEXT_TRANSFORMER_INSTANCE);

        // try to create new one
        if( transformer == null )
        {
            String transformerClassName = dc.getInfo().getProperty(TEXT_TRANSFORMER_NAME);
            try
            {
                if( transformerClassName == null )
                {
                    transformer = new BeanInfoEntryTransformer<>();
                } else
                {
                    transformer = ( ClassLoading.loadSubClass( transformerClassName, Transformer.class ) ).newInstance();
                }
                transformer.init(null, dc);
            }
            catch( Throwable t )
            {
                throw new Exception("Can not instantiate text transformer for data collection " + dc.getCompletePath() + ", error: " + t, t);
            }

            if( !Entry.class.isAssignableFrom(transformer.getInputType()) )
                throw new Exception("Incorrect input type for text transformer, data collection=" + dc.getCompletePath()
                        + "\r\n  transformer=" + transformerClassName + ", input type=" + transformer.getInputType());

            dc.getInfo().getProperties().put(TEXT_TRANSFORMER_INSTANCE, transformer);
        }

        return transformer;
    }
    /**
     * Converts ru.biosoft.access.core.DataElement to String that will be sent to client.
     */
    protected <T extends DataElement> String convertToString(T de) throws Exception
    {
        if( de == null )
            return null;

        Transformer<Entry, T> transformer = getTransformer(de.getOrigin());
        Entry entry = transformer.transformOutput(de);

        return entry.getData();
    }

    /**
     * Converts ru.biosoft.access.core.DataElement to String that will be sent to cleint.
     */
    protected <T extends DataElement> String convertToString(T de, Transformer<Entry, T> transformer) throws Exception
    {
        if( de == null )
            return null;

        Entry entry = transformer.transformOutput(de);

        return entry.getData();
    }

    /**
     * Converts String to ru.biosoft.access.core.DataElement.
     */
    public DataElement convertToDataElement(DataCollection parent, String name, String str) throws Exception
    {
        if( str == null )
            return null;

        Transformer<Entry, ? extends DataElement> transformer = getTransformer(parent);
        Entry entry = new Entry(parent, name, str, Entry.TEXT_FORMAT);

        return transformer.transformInput(entry);
    }

    /**
     * Converts String to ru.biosoft.access.core.DataElement.
     */
    protected DataElement convertToDataElement(DataCollection parent, Transformer<Entry, ? extends DataElement> transformer, String name, String str)
            throws Exception
    {
        if( str == null )
            return null;

        Entry entry = new Entry(parent, name, str, Entry.TEXT_FORMAT);

        return transformer.transformInput(entry);
    }
    
    /**
     * Checks whether the element of given type can be exported to file
     * @param type to check
     * @return true if it can be exported via {@link AccessProtocol#saveAsFile} method
     */
    protected boolean canExportToFile(Class<? extends DataElement> type)
    {
        if(FileDataElement.class == type)
            return true;
        List<Class<? extends Transformer>> transformers = TransformerRegistry.getTransformerClass(FileDataElement.class, type);
        if(!transformers.isEmpty())
            return true;
        transformers = TransformerRegistry.getTransformerClass(ru.biosoft.access.Entry.class, type);
        if(!transformers.isEmpty())
            return true;
        return false;
    }
    
    /**
     * Saves element into file and returns a FileItem object pointing to file
     * @param de element to save
     * @return FileItem object. Its original names contains transformer class name if necessary
     * @throws Exception if element cannot be saved to file
     * @throws IOException if I/O error occurs during saving
     */
    protected FileItem saveAsFile(DataElement de) throws Exception, IOException
    {
        FileItem file;
        if(de instanceof FileDataElement)
        {
            file = new FileItem(((FileDataElement)de).getFile());
            file.setOriginalName("");
        } else
        {
            Transformer transformer = TransformerRegistry.getBestTransformer(de, FileDataElement.class);
            if(transformer == null)
            {
                transformer = TransformerRegistry.getBestTransformer(de, ru.biosoft.access.Entry.class);
                if(transformer == null)
                    throw new Exception("No transformer for "+DataElementPath.create(de));
                if(!transformer.isOutputType(de.getClass()))
                    throw new Exception("Invalid transformer for "+DataElementPath.create(de));
                ru.biosoft.access.Entry entry = (ru.biosoft.access.Entry)transformer.transformOutput(de);
                file = new FileItem(TempFiles.file("element", entry.getEntryData()));
                file.setOriginalName(transformer.getClass().getName());
            } else
            {
                if(!(transformer instanceof AbstractFileTransformer))
                    throw new Exception("No transformer for "+DataElementPath.create(de));
                if(!transformer.isOutputType(de.getClass()))
                    throw new Exception("Invalid transformer for "+DataElementPath.create(de));
                file = new FileItem(TempFiles.file("element"));
                file.setOriginalName(transformer.getClass().getName());
                ( (AbstractFileTransformer)transformer ).save(file, de);
            }
        }
        return file;
    }
    
    
    protected static DataElement loadFromStream(DataElementPath path, String transformerName, InputStream stream)
            throws IOException, FileNotFoundException, InstantiationException, IllegalAccessException, Exception
    {
        DataCollection<DataElement> parent = path.getParentCollection();
        if( transformerName.isEmpty() )
        {
            File dir = TempFiles.dir( "element" );
            File file = new File( dir, path.getName() );
            file.getParentFile().mkdirs();
            ApplicationUtils.copyStream( new FileOutputStream( file ), stream );
            return new FileDataElement( path.getName(), parent, file );
        }

        Transformer transformer = ( ClassLoading.loadSubClass( transformerName, Transformer.class ) ).newInstance();
        if( transformer instanceof AbstractFileTransformer )
        {
            File dir = TempFiles.dir( "element" );
            try
            {
                File file = new File( dir, path.getName() );
                ApplicationUtils.copyStream( new FileOutputStream( file ), stream );
                transformer.init( null, parent );

                DataElement de = ( (AbstractFileTransformer)transformer ).load( file, path.getName(), parent );
                return de;
            }
            finally
            {
                ApplicationUtils.removeDir( dir );
            }
        }
        Entry entry = new Entry( parent, path.getName(), ApplicationUtils.readAsString( stream ) );
        transformer.init( null, parent );
        return transformer.transformInput( entry );
    }
}
