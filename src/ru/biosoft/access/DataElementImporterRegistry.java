package ru.biosoft.access;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.exception.ExceptionRegistry;


public class DataElementImporterRegistry extends DataElementRegistry<DataElementImporterRegistry.ImporterInfo>
{
    public static final String AUTODETECT = "autodetect";

    /** Utility class that stores information about <code>DataElementImporter</code>. */
    public static class ImporterInfo extends DataElementRegistry.RegistryElementInfo
    {
        public static final String DISPLAY_NAME = "displayName";

        public ImporterInfo(DataElementImporter importer, Properties prop)
        {
            super(prop);
            this.importer = importer;
        }

        protected DataElementImporter importer;
        public DataElementImporter getImporter()
        {
            return importer;
        }

        public String getDisplayName()
        {
            return properties.getProperty( DISPLAY_NAME, getFormat() );
        }

        public DataElementImporter cloneImporter()
        {
            DataElementImporter importer = null;
            try
            {
                importer = this.importer.getClass().newInstance();
                importer.init(properties);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            return importer;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static Logger log = Logger.getLogger(DataElementImporterRegistry.class.getName());
    private static DataElementImporterRegistry instance = new DataElementImporterRegistry();

    public static final String IMPORT = "import";
    public static final String IMPORTER_CLASS = "importer";
    public static final String DATABASE_TYPE = "moduleType";

    private DataElementImporterRegistry()
    {
        super("ru.biosoft.access.import");
    }

    @Override
    protected ImporterInfo loadElement(IConfigurationElement element, String format) throws Exception
    {
        DataElementImporter importerInstance = getClassAttribute(element, IMPORTER_CLASS, DataElementImporter.class).newInstance();
        if( !importerInstance.init(getExtensionProperties(element)) )
            throw new BiosoftCustomException( null, "Importer does not support required format " + format );
        return new ImporterInfo(importerInstance, getExtensionProperties(element));
    }

    public static StreamEx<ImporterInfo> importers()
    {
        Preferences preferences = Application.getPreferences();
        String perspectiveStr = preferences != null ? preferences.getStringValue( "Perspective", "Default" ) : "Default";
        Perspective perspective = PerspectiveRegistry.getPerspective( perspectiveStr );
        StreamEx<ImporterInfo> initialStream = instance.stream();
        if( perspective != null )
            return initialStream.filter( ii -> perspective.isImporterAvailable( ii.getImporter().getClass().getCanonicalName() ) );
        else
            return initialStream;
    }

    public static Object getImporterProperties(String format)
    {
        ImporterInfo info = getImporterInfo(format);
        return info == null ? null : info.getImporter().getProperties(null, null, null);
    }

    public static ImporterInfo getImporterInfo(String format)
    {
        return instance.getExtension(format);
    }

    /**
     * Returns suitable importer for the specified file and format.
     *
     * @param file - file to be imported
     * @param format - format name. If format equals <code>autodetect</code>,
     * then registry checks all possible importers and returns first that can accept this format.
     * Otherwise it checks only importers that are registered for this format.
     *
     * @returns most suitable importer for this file or null if suitable importer is not found.
     *
     * @pending possibly Importer should return not boolean, but integer
     * that indicates suitability of the importer for the specified format.
     *
     * @pending who should print in log detected format and importer name - registry or importer?
     */
    public static DataElementImporter getImporter(File file, String format, DataCollection parent)
    {
        if( parent == null || !parent.isMutable() )
            return null;

        ImporterInfo info = getImporterInfo(format);
        if( info == null )
            return null;
        try
        {
            DataElementImporter importer = info.cloneImporter();
            if( importer.accept(parent, file) > DataElementImporter.ACCEPT_UNSUPPORTED )
                return info.cloneImporter();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while creating new instance of importer" + info.getClass().getName() + ": " + ExceptionRegistry.log(ex));
        }
        return null;
    }

    public static boolean isImportAvailableForCollection(DataCollection parent)
    {
        if( parent == null || !parent.isMutable() )
            return false;
        for( ImporterInfo info : importers() )
        {
            try
            {
                DataElementImporter importer = info.getImporter();

                if( importer.accept(parent, null) > DataElementImporter.ACCEPT_UNSUPPORTED )
                    return true;
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error while creating new instance of importer" + ( info != null ? info.getClass().getName() : "" ) + ": ", ex);
            }
        }
        return false;
    }

    public static ImporterInfo[] getAutoDetectImporter(File file, DataCollection parent, boolean all)
    {
        return getAutoDetectImporter( file, parent, all, DataElementImporter.ACCEPT_UNSUPPORTED );
    }

    public static ImporterInfo[] getAutoDetectImporter(File file, DataCollection parent, boolean all, int priorityCutoff)
    {
        if( parent == null || !parent.isMutable() )
            return null;

        class ImporterWithPriority implements Comparable<ImporterWithPriority>
        {
            ImporterInfo importer;
            int priority;

            public ImporterWithPriority(ImporterInfo importer, int priority)
            {
                this.importer = importer;
                this.priority = priority;
            }

            @Override
            public int compareTo(ImporterWithPriority o)
            {
                return priority > o.priority ? -1 : priority < o.priority ? 1 : importer.compareTo(o.importer);
            }
        }

        List<ImporterWithPriority> feasibleImporters = new ArrayList<>();
        int priority = priorityCutoff;
        for( ImporterInfo info : importers() )
        {
            try
            {
                DataElementImporter importer = info.getImporter();
                int newPriority = importer.accept(parent, file);

                if( file == null && newPriority > priorityCutoff )
                    newPriority = DataElementImporter.ACCEPT_HIGH_PRIORITY;

                if( newPriority > priorityCutoff && ( newPriority >= priority || all ) )
                {
                    if( newPriority > priority && !all )
                    {
                        priority = newPriority;
                        feasibleImporters.clear();
                    }
                    feasibleImporters.add(new ImporterWithPriority(info, newPriority));
                }
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error while creating new instance of importer" + ( info != null ? info.getClass().getName() : "" ) + ": ", ex);
            }
        }

        if( feasibleImporters.size() > 0 )
        {
            return StreamEx.of(feasibleImporters).sorted().map( info -> info.importer ).toArray( ImporterInfo[]::new );
        }
        return null;
    }
}
