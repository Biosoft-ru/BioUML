package biouml.plugins.sbml;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import ru.biosoft.util.ExtensionRegistrySupport;

public class SbmlPackageRegistry extends ExtensionRegistrySupport<SbmlPackageRegistry.PackageInfo>
{
    private static final SbmlPackageRegistry instance = new SbmlPackageRegistry();
    private static Logger log = Logger.getLogger( SbmlPackageRegistry.class.getName() );

    public static final String READER_CLASS_NAME = "reader";
    public static final String WRITER_CLASS_NAME = "writer";
    public static final String PACKAGE_NAME = "name";
    
    private SbmlPackageRegistry()
    {
        super("biouml.plugins.sbml.package", PACKAGE_NAME);
    }

    @Override
    protected PackageInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        Class<? extends SbmlPackageReader> readerClass = getClassAttribute(element, READER_CLASS_NAME, SbmlPackageReader.class);
        Class<? extends SbmlPackageWriter> writerClass = getClassAttribute(element, WRITER_CLASS_NAME, SbmlPackageWriter.class);
        return new PackageInfo(name, readerClass, writerClass);
    }

    public synchronized static SbmlPackageReader getReader(String packageName)
    {
        PackageInfo packageInfo = instance.getExtension(packageName);
        if( packageInfo != null )
        {
            try
            {
                return packageInfo.readerClass.newInstance();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not get reader by package name, error: " + e, e );
                return null;
            }
        }
        return null;
    }

    public static List<SbmlPackageReader> getReaders(List<String> packages)
    {
        List<SbmlPackageReader> result = new ArrayList<>();
        for( String packageName : packages )
        {
            SbmlPackageReader reader = getReader( packageName );
            if( reader != null )
                result.add( reader );
        }
        return result;
    }

    public static List<SbmlPackageWriter> getWriters(List<String> packages)
    {
        List<SbmlPackageWriter> result = new ArrayList<>();
        for( String packageName : packages )
        {
            SbmlPackageWriter writer = getWriter( packageName );
            if( writer != null )
                result.add( writer );
        }
        return result;
    }

    public synchronized static SbmlPackageWriter getWriter(String packageName)
    {
        PackageInfo packageInfo = instance.getExtension(packageName);
        if( packageInfo != null )
        {
            try
            {
                return packageInfo.writerClass.newInstance();
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not get writer by package name, error: " + e, e );
                return null;
            }
        }
        return null;
    }

    static class PackageInfo
    {
        public PackageInfo(String name, Class<? extends SbmlPackageReader> readerClass, Class<? extends SbmlPackageWriter> writerClass)
        {
            this.readerClass = readerClass;
            this.writerClass = writerClass;
            this.packageName = name;
        }
        public String packageName;
        public Class<? extends SbmlPackageReader> readerClass;
        public Class<? extends SbmlPackageWriter> writerClass;
    }
}
