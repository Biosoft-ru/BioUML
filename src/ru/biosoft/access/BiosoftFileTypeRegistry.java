package ru.biosoft.access;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.FileTypePriority;
import ru.biosoft.access.exception.ExtensionInitException;
import ru.biosoft.access.exception.InitializationException;
import ru.biosoft.access.file.FileType;
import ru.biosoft.access.file.FileTypeRegistry;
import ru.biosoft.access.file.FileTypeRegistryImpl;
import ru.biosoft.exception.MissingParameterException;

public class BiosoftFileTypeRegistry extends FileTypeRegistryImpl
{
    protected static final Logger log = Logger.getLogger( BiosoftFileTypeRegistry.class.getName() );
    public static final String NAME_ATTR = "name";
    public static final String EXTENSIONS_ATTR = "extensions";
    public static final String TRANSFORMER_CLASS = "transformerClass";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String PRIORITY_ATTR = "priority";

    protected static Map<String, FileType> byName = new HashMap<>();
    protected static Map<String, FileType> byExtension = new HashMap<>();

    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    private static String extensionPointId = "ru.biosoft.access.fileType";


    static
    {
        byName.put( FileTypeRegistry.FILE_TYPE_TEXT.getName(), FileTypeRegistry.FILE_TYPE_TEXT );
        byName.put( FileTypeRegistry.FILE_TYPE_BINARY.getName(), FileTypeRegistry.FILE_TYPE_BINARY );
    }

    public void register(FileType fileType)
    {
        if(byName.containsKey( fileType.getName() ))
        {
            if( fileType.getTransformerClassName().equals( byName.get( fileType.getName() ).getTransformerClassName() ) )
                log.warning( "FileTypeRegistry: file type '" + fileType.getName() + "'registered more than once with the same transformer class" );
            else
                log.warning( "FileTypeRegistry: duplicated fileType name '" + fileType.getName() + System.lineSeparator() + "FileType (used):    "
                        + byName.get( fileType.getName() ) + System.lineSeparator() + "FileType (skipped): " + fileType );
            return;
        }
        byName.put( fileType.getName(), fileType );

        for ( String extension : fileType.getExtensions() )
        {
            if( !byExtension.containsKey( extension ) )
                byExtension.put( extension, fileType );
            else
            {
                FileType ft = byExtension.get( extension );

                if( ft.getPriority().isHigher( fileType.getPriority() ) )
                    continue;
                else if( fileType.getPriority().isHigher( ft.getPriority() ) )
                    byExtension.put( extension, fileType );

                else // ft.getPriority() == fileType.getPriority()
                    log.warning( "FileTypeRegistry: extension '" + extension + "'" + "corresponds to 2 file types with the same priority " + ft.getPriority()
                            + System.lineSeparator() + "FileType (used):    " + ft + System.lineSeparator() + "FileType (skipped): " + fileType );
            }
        }
    }

    public FileType getFileType(String name)
    {
        init();
        return byName.get( name );
    }

    public FileType getFileTypeByExtension(String extension)
    {
        init();
        return byExtension.get( extension );
    }

    public FileType getFileTypeByTransformer(String transformerClassName)
    {
        init();
        if( transformerClassName == null )
            return FileTypeRegistry.FILE_TYPE_BINARY;
        return byName.values().stream().filter( ft -> transformerClassName.equals( ft.getTransformerClassName() ) ).findFirst().orElse( null );
    }

    protected final void init()
    {
        if( !initialized )
        {
            synchronized (this)
            {
                if( !initialized )
                {
                    if( initializing )
                        throw new InitializationException( "Concurrent initialization of extension point " + extensionPointId );
                    initializing = true;
                    try
                    {
                        loadExtensions();
                    }
                    finally
                    {
                        initializing = false;
                        initialized = true;
                    }
                }
            }
        }
    }

    private void loadExtensions()
    {
        IExtensionRegistry extensionRegistry = Application.getExtensionRegistry();
        if( extensionRegistry == null )
        {
            new ExtensionInitException( extensionPointId ).log();
        }
        else
        {
            IExtensionPoint point = extensionRegistry.getExtensionPoint( extensionPointId );
            if( point == null )
            {
                new ExtensionInitException( extensionPointId ).log();
            }
            else
            {
                for ( IExtension extension : point.getExtensions() )
                {
                    String pluginId = extension.getNamespaceIdentifier();
                    for ( IConfigurationElement element : extension.getConfigurationElements() )
                    {
                        String name = element.getName();
                        try
                        {
                            name = getStringAttribute( element, NAME_ATTR );
                            String extensionsStr = element.getAttribute( EXTENSIONS_ATTR );
                            String[] extensions = extensionsStr.split( ";" );
                            String transformerClass = element.getAttribute( TRANSFORMER_CLASS );
                            int priority = getIntAttribute( element, DESCRIPTION_ATTR );
                            String description = element.getAttribute( DESCRIPTION_ATTR );
                            FileType ft = new FileType( name, extensions, transformerClass, getPriorityByValue( priority ), description );
                            register( ft );

                        }
                        catch (Throwable t)
                        {
                            new ExtensionInitException( t, extensionPointId, pluginId, name, null ).log();
                        }
                    }
                }
            }
        }
    }

    protected int getIntAttribute(IConfigurationElement element, String name)
    {
        int defaultValue = 0;
        String value = element.getAttribute( name );
        if( value == null )
            return defaultValue;
        try
        {
            return Integer.parseInt( value );
        }
        catch (NumberFormatException ex)
        {
            return defaultValue;
        }
    }

    protected @Nonnull String getStringAttribute(IConfigurationElement element, String name)
    {
        String value = element.getAttribute( name );
        if( value == null )
        {
            throw new MissingParameterException( name );
        }
        return value;
    }

    private FileTypePriority getPriorityByValue(int value)
    {
        FileTypePriority curPriority = FileTypePriority.ZERO_PROPRITY;
        for ( FileTypePriority ftp : FileTypePriority.values() )
        {
            if( ftp.getPriorityValue() < value && ftp.getPriorityValue() > curPriority.getPriorityValue() )
                curPriority = ftp;
        }
        return curPriority;
    }

    @Override
    public Stream<FileType> fileTypes()
    {
        init();
        return byName.values().stream();
    }
}
