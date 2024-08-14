package ru.biosoft.galaxy;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;

/**
 * Registry to convert galaxy types to BioUML import/export and file's extensions
 */
public class FormatRegistry
{
    private static final Logger log = Logger.getLogger( FormatRegistry.class.getName() );
    public static final String TYPE_DELIMITER = ",";

    protected static final String TAG_FORMAT = "format";
    protected static final String TAG_EXPORTER = "exporter";
    protected static final String TAG_IMPORTER = "importer";
    protected static final String TAG_PROPERTY = "property";

    protected static final String ATTR_GALAXY_FORMAT = "galaxyFormat";
    protected static final String ATTR_EXPORTER_NAME = "name";
    protected static final String ATTR_IMPORTER_NAME = "name";
    protected static final String ATTR_EXTENSION = "fileExtension";
    protected static final String ATTR_BIOUML_TYPE = "bioumlType";
    protected static final String ATTR_PROPERTY_KEY = "name";
    protected static final String ATTR_PROPERTY_VALUE = "value";

    protected static final String DEFAULT_EXPORTER_NAME = "Generic file";
    protected static final String DEFAULT_IMPORTER_NAME = "Generic file";

    private static Map<String, String> exporters;
    private static Map<String, String> importers;
    private static Map<String, Map<String,String>> exporterProperties;
    private static Map<String, Map<String,String>> importerProperties;
    private static Map<String, String> fileExtensions;
    private static Map<String, Class<? extends DataElement>> classes;

    private static boolean isInit = false;
    private synchronized static void init()
    {
        if( !isInit )
        {
            isInit = true;

            exporters = new HashMap<>();
            importers = new HashMap<>();
            exporterProperties = new HashMap<>();
            importerProperties = new HashMap<>();
            fileExtensions = new HashMap<>();
            classes = new HashMap<>();

            IExtensionRegistry registry = Application.getExtensionRegistry();
            if(registry == null) return; // for tests
            IConfigurationElement[] extensions = registry.getConfigurationElementsFor("ru.biosoft.galaxy.types");

            for( IConfigurationElement extension : extensions )
            {
                String extensionName = extension.getName();
                if( extensionName.equals(TAG_FORMAT) )
                {
                    String galaxyFormat = extension.getAttribute(ATTR_GALAXY_FORMAT);
                    if( galaxyFormat == null ) continue;
                    String fileExtension = extension.getAttribute(ATTR_EXTENSION);
                    String bioumlClassName = extension.getAttribute(ATTR_BIOUML_TYPE);
                    Class<? extends DataElement> bioumlClass = null;
                    try
                    {
                        bioumlClass = ClassLoading.loadSubClass( bioumlClassName, DataElement.class );
                    }
                    catch(Throwable t)
                    {
                    }
                    if( fileExtension != null )
                    {
                        fileExtensions.put(galaxyFormat, fileExtension);
                    }
                    if( bioumlClass != null )
                    {
                        classes.put(galaxyFormat, bioumlClass);
                    }

                    IConfigurationElement[] children = extension.getChildren(TAG_EXPORTER);
                    if(children != null && children.length > 0)
                    {
                        String exporter = children[0].getAttribute(ATTR_EXPORTER_NAME);
                        if( exporter != null )
                        {
                            exporters.put(galaxyFormat, exporter);
                            IConfigurationElement[] properties = children[0].getChildren(TAG_PROPERTY);
                            if(properties != null && properties.length > 0)
                            {
                                Map<String, String> props = new LinkedHashMap<>();
                                for(IConfigurationElement property: properties)
                                {
                                    props.put(property.getAttribute(ATTR_PROPERTY_KEY), property.getAttribute(ATTR_PROPERTY_VALUE));
                                }
                                exporterProperties.put(galaxyFormat, props);
                            }
                        }
                    }
                    children = extension.getChildren(TAG_IMPORTER);
                    if(children != null && children.length > 0)
                    {
                        String importer = children[0].getAttribute(ATTR_IMPORTER_NAME);
                        if( importer != null )
                        {
                            importers.put(galaxyFormat, importer);
                            IConfigurationElement[] properties = children[0].getChildren(TAG_PROPERTY);
                            if(properties != null && properties.length > 0)
                            {
                                Map<String, String> props = new LinkedHashMap<>();
                                for(IConfigurationElement property: properties)
                                {
                                    props.put(property.getAttribute(ATTR_PROPERTY_KEY), property.getAttribute(ATTR_PROPERTY_VALUE));
                                }
                                importerProperties.put(galaxyFormat, props);
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getExporterFormat(String galaxyFormat)
    {
        init();
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            String result = exporters.get(type.trim());
            if( result != null )
                return result;
        }
        return DEFAULT_EXPORTER_NAME;
    }
    
    public static Map<String, String> getExporterProperties(String galaxyFormat)
    {
        init();
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            Map<String,String> result = exporterProperties.get(type.trim());
            if( result != null )
                return result;
        }
        return Collections.emptyMap();
    }

    public static String getImporterFormat(String galaxyFormat)
    {
        init();
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            String result = importers.get(type.trim());
            if( result != null )
                return result;
        }
        return DEFAULT_IMPORTER_NAME;
    }
    
    
    public static class ExporterResult
    {
        public final String galaxyFormat;
        public final Map<String, String> exporterProperties;
        public final Object propertiesBean;
        public final DataElementExporter exporter;
        public final String exporterName;
        
        public ExporterResult(String galaxyFormat, Map<String, String> exporterProperties, Object propertiesBean, DataElementExporter exporter, String exporterName)
        {
            this.galaxyFormat = galaxyFormat;
            this.exporterProperties = exporterProperties;
            this.propertiesBean = propertiesBean;
            this.exporter = exporter;
            this.exporterName = exporterName;
        }
    }
    /**
     * 
     * @param galaxyFormatList  comma separated list of galaxy formats
     * @param path
     * @return pair, where first is selected galaxy format and second is exporter for this format
     */
    public static ExporterResult getExporter(String galaxyFormatList, DataElementPath path)
    {
        init();
        String[] types = StreamEx.of( galaxyFormatList.split(TYPE_DELIMITER) ).map( String::trim ).toArray(String[]::new);
        for( String type : types )
        {
            type = type.trim();
            String exporterName = exporters.get(type);
            if( exporterName == null )
                continue;
            Map<String, String> properties = exporterProperties.get(type);
            
            DataElement de = path == null ? null : path.optDataElement();
            
            DataElementExporter exporter;
            if(de == null)
            {
                exporter = DataElementExporterRegistry.getExporterInfoByName( exporterName ).cloneExporter();
            }
            else
            {
                ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo(exporterName, de);
                if( exporterInfo == null || exporterInfo.length == 0 )
                    continue;
                exporter = exporterInfo[0].cloneExporter();
            }
            
            Object propertiesObject = null;
            try {
                propertiesObject = exporter.getProperties(de, null);
            } catch(Exception e)
            {
                //some exporters throws exception when requesting properties for de=null
                log.log( Level.WARNING, "Can not get exporter properties for exporter=" + exporterName + " and de=" + path, e );
            }
            if( properties != null && propertiesObject != null)
            {
                    ComponentModel model = ComponentFactory.getModel(propertiesObject);
                    for( Entry<String, String> entry : properties.entrySet() )
                    {
                        Property property = model.findProperty(entry.getKey());
                        if( property != null )
                        {
                            try
                            {
                                property.setValue(TextUtil.fromString(property.getValueClass(), entry.getValue()));
                            }
                            catch( NoSuchMethodException e )
                            {
                            }
                        }
                    }
                    
            }
            
            if( exporter != null )
                return new ExporterResult( type, properties, propertiesObject, exporter, exporterName );
        }
        log.warning( "Can not find suitable exporter for galaxy format: " + galaxyFormatList );
        DataElementExporter exporter = DataElementExporterRegistry.getExporterInfoByName( DEFAULT_EXPORTER_NAME ).cloneExporter();
        return new ExporterResult( types[0], null, null, exporter , DEFAULT_EXPORTER_NAME );
    }
    
    public static ExporterResult getExporter(String galaxyFormat, DataElementPath path, Properties exporterOptions)
    {
        ExporterResult result = getExporter( galaxyFormat, path );
        if( result != null )
        {
            DataElementExporter exporter = result.exporter;
            Object options = exporter.getProperties( path.optDataElement(), null );
            if( options != null )
                BeanUtil.readBeanFromProperties( options, exporterOptions, "format." );
        }
        return result;
    }

    public static class ImporterResult
    {
        public final DataElementImporter importer;
        public final String importerName;
        public final String galaxyFormat;
        public final Object propertiesBean;
        public ImporterResult(DataElementImporter importer, String importerName, String galaxyFormat, Object propertiesBean)
        {
            this.importer = importer;
            this.importerName = importerName;
            this.galaxyFormat = galaxyFormat;
            this.propertiesBean = propertiesBean;
        }
    }
    
    public static ImporterResult getImporter(String galaxyFormat, DataElementPath path, File file)
    {
        init();
        
        String importerName = DEFAULT_IMPORTER_NAME;
        Map<String, String> properties = new HashMap<>();
        properties.put(FileImporter.PRESERVE_EXTENSION_PROPERTY, "false");
        
        String selectedGalaxyFormat = galaxyFormat;
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            String result = importers.get(type.trim());
            if( result != null )
            {
                importerName = result;
                properties = importerProperties.get(type.trim());
                selectedGalaxyFormat = type;
            }
        }
        DataCollection<?> parent = path == null ? null : path.optParentCollection();
        String deName = path == null ? null : path.getName();
        DataElementImporter importer;
        if(file == null)
            importer = DataElementImporterRegistry.getImporterInfo( importerName ).cloneImporter();
        else
            importer = DataElementImporterRegistry.getImporter(file, importerName, parent);
        if(importer == null)
            return null;
        Object propertiesObject = null;
        try {
            propertiesObject = importer.getProperties(parent, file, deName);
        } catch(Exception e)
        {
            log.log( Level.WARNING, "Can not fetch properties for importer " + importerName, e );
        }
        if( properties != null && propertiesObject != null )
        {
            ComponentModel model = ComponentFactory.getModel( propertiesObject );
            for( Entry<Property, String> entry : EntryStream.of( properties ).mapKeys( model::findProperty ).nonNullKeys() )
            {
                try
                {
                    entry.getKey().setValue( TextUtil.fromString( entry.getKey().getValueClass(), entry.getValue() ) );
                }
                catch( NoSuchMethodException e )
                {
                }
            }
        }
        return new ImporterResult( importer, importerName, selectedGalaxyFormat, propertiesObject );
    }
    
    public static DataElementImporter getImporter(String galaxyFormat, DataElementPath path, File file, Properties importerOptions)
    {
        DataElementImporter result = getImporter( galaxyFormat, path, file ).importer;
        if( result != null )
        {
            Object options = result.getProperties( path.optParentCollection(), file, path.getName() );
            if( options != null )
                BeanUtil.readBeanFromProperties( options, importerOptions, "format." );
        }
        return result;
    }

    public static String getFileExtension(String galaxyFormat)
    {
        init();
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            String result = fileExtensions.get(type.trim());
            if( result != null )
                return result;
        }
        return galaxyFormat;
    }

    public static Class<? extends DataElement> getClass(String galaxyFormat)
    {
        init();
        for( String type : galaxyFormat.split(TYPE_DELIMITER) )
        {
            Class<? extends DataElement> bioumlClass = classes.get(type.trim());
            if( bioumlClass != null )
                return bioumlClass;
        }
        return FileDataElement.class;
    }
}
