package ru.biosoft.access;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IConfigurationElement;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveRegistry;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * Base class for exporters of various type DataElements
 * Cannot be instantiated directly, its ancestors should define methods to do this
 * For usage example see biouml.workbench.diagram.DiagramExporterRegistry
 * @author lan
 */
public class DataElementExporterRegistry extends DataElementRegistry<DataElementExporterRegistry.ExporterInfo>
{
    private volatile static DataElementExporterRegistry instance = new DataElementExporterRegistry();
    /** Utility class that stores information about <code>DataElementExporter</code>. */
    public static class ExporterInfo extends DataElementRegistry.RegistryElementInfo
    {
        public ExporterInfo(DataElementExporter exporter, Properties prop)
        {
            super(prop);
            this.exporter = exporter;
        }

        protected DataElementExporter exporter;
        public DataElementExporter getExporter()
        {
            return exporter;
        }

        public DataElementExporter cloneExporter()
        {
            try
            {
                DataElementExporter clone = exporter.getClass().newInstance();
                clone.init(properties);
                return clone;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not clone exporter", e);
                return null;
            }

        }

        public String getSuffix()
        {
            return properties.getProperty(SUFFIX);
        }

        public String getContentType()
        {
            return properties.getProperty(CONTENT_TYPE);
        }

    }

    ///////////////////////////////////////////////////////////////////

    private static Logger log = Logger.getLogger(DataElementExporterRegistry.class.getName());

    public static final String EXPORTER_CLASS = "exporter";
    public static final String SUFFIX = "suffix";
    public static final String CONTENT_TYPE = "contentType";

    private DataElementExporterRegistry()
    {
        super("ru.biosoft.access.export");
    }

    @Override
    protected ExporterInfo loadElement(IConfigurationElement element, String format) throws Exception
    {
        DataElementExporter exporterInstance = getClassAttribute(element, EXPORTER_CLASS, DataElementExporter.class).newInstance();
        if( !exporterInstance.init(getExtensionProperties(element)) )
            throw new Exception("Exporter does not support required format " + format + ", suffix=" + getExtensionProperties(element).getProperty(SUFFIX));

        return new ExporterInfo(exporterInstance, getExtensionProperties(element));
    }

    /**
     * Returns suitable exporter for the specified format.
     */
    protected ExporterInfo[] doGetExporterInfo(String format, DataElement de)
    {
        return exporters()
            .filter( info -> info.getFormat().equals( format ) )
            .mapToEntry( info -> getExporterPriority( de, info ) )
            .filterValues( priority -> priority > 0 )
            .reverseSorted( Comparator.comparingInt( Entry::getValue ) )
            .keys()
            .toArray( ExporterInfo[]::new );
    }

    /**
     * Returns supported exporters for the specified ru.biosoft.access.core.DataElement.
     */
    protected boolean doHasExporter(DataElement de)
    {
        return exporters().anyMatch( info -> getExporterPriority( de, info ) > 0 );
    }

    /**
     * Get possible exporter formats order by accept priority
     */
    public static List<String> getExporterFormats(DataElement de)
    {
        if(de == null) return Collections.emptyList();
        return exporters()
                .mapToEntry( ExporterInfo::getFormat, info -> getExporterPriority( de, info ) )
                .filter( entry -> entry.getValue() > 0 )
                .reverseSorted( Comparator.comparingInt( Entry::getValue ) )
                .map( Entry::getKey )
                .toList();
    }
    
    public static ExporterInfo getExporterInfoByName(String name)
    {
        return exporters()
                .findAny( info->info.getFormat().equals( name ) )
                .get();
    }

    public static ExporterInfo[] getExporterInfo(String format, DataElement de)
    {
        ExporterInfo[] infos = instance.doGetExporterInfo(format, de);
        return infos.length == 0 ? null : infos;
    }

    public static boolean hasExporter(DataElement de)
    {
        return instance.doHasExporter(de);
    }

    public static StreamEx<ExporterInfo> exporters()
    {
        Preferences preferences = Application.getPreferences();
        String perspectiveStr = preferences != null ? preferences.getStringValue( "Perspective", "Default" ) : "Default";
        Perspective perspective = PerspectiveRegistry.getPerspective( perspectiveStr );
        StreamEx<ExporterInfo> initialStream = instance.stream();
        if( perspective != null )
            return initialStream.filter( ei -> perspective.isExporterAvailable( ei.getExporter().getClass().getCanonicalName() ) );
        else
            return initialStream;
    }

    public static StreamEx<String> formats()
    {
        return exporters().map( ei -> ei.getFormat() );
    }

    private static int getExporterPriority(DataElement de, ExporterInfo info)
    {
        DataElementExporter exporter = info.getExporter();
        int acceptPriority;
        try
        {
            acceptPriority = exporter.accept(de);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Error calling "+exporter.getClass().getName()+".accept(): "+ExceptionRegistry.log( e ) );
            acceptPriority = 0;
        }
        if( acceptPriority > 0 )
        {
            return Math.max(acceptPriority, info.getPriority());
        }
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }
}
