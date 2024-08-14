package biouml.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Stub;
import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.TextUtil;

/**
 * The module data is hierarchically organized in following manner:
 * <ul>
 *   <li><b>Diagrams</b>
 *     <br><code>DataCollection</code> of <code>Diagram</code>s.
 *     <br>In future diagrams can be hierarchically grouped.</li>
 *   <li><b>Data</b>
 *     <br>"Kernel" and other data elements. For example, literature reference, that is <code>DataElemrnt</code>)
 *     but can not be used as diagram element.
 *     <br> Data elements ordered by categories. So <b>Data</b> is a <code>DataCollection</code>
 *     where data elements are categories.</li>
 *   <li><b>Metadata</b>
 *     <br>This <code>DataCollection</code> stores <code>DiagramType</code>s,
 *       inherited databases and so on.</li>
 *   </ul>
 *
 */
@PropertyName("module")
public class Module extends DerivedDataCollection implements HtmlDescribedElement
{
    //////////////////////////////////////////////////////////////////
    // Static section
    //

    /** Name of diagrams ru.biosoft.access.core.DataCollection. */
    public static final String DIAGRAM = "Diagrams";

    /** Name of data ru.biosoft.access.core.DataCollection. */
    public static final String DATA = "Data";

    /** Name of metadata ru.biosoft.access.core.DataCollection. */
    public static final String METADATA = "Dictionaries";

    /** Name of DataCollection for simulation results, plots, etc. */
    public static final String SIMULATION = "Simulation";

    /** Name of images ru.biosoft.access.core.DataCollection. */
    public static final String IMAGES = "Images";

    /** Name of graphic notations ru.biosoft.access.core.DataCollection. */
    public static final String GRAPHIC_NOTATIONS = "graphic notations";

    public static final String TYPE_PROPERTY = "module-type";

    public static final String RESULT = "result";
    public static final String PLOT = "plot";
    
    static public @Nonnull Module getModule(@Nonnull ru.biosoft.access.core.DataElement element)
    {
        Assert.notNull("element", element);
        Module module = optModule(element);
        if(module == null)
            throw new InternalException("No module found for "+element.getCompletePath());
        return module;
    }

    /**
     * Get module for every data elements in module, or null
     */
    static public @CheckForNull Module optModule(DataElement element)
    {
        if( element == null )
            return null;
        if( element instanceof Module )
            return (Module)element;
        if( element.getOrigin() == null )
            return null;
        String[] components = element.getCompletePath().getPathComponents();
        if(components.length == 0)
            return null;
        DataCollection<?> dc = DataElementPath.create(components[0]).optDataCollection();
        if( dc == null )
            return null;
        for(int i=1; i<components.length; i++)
        {
            if( dc instanceof Module )
                return (Module)dc;
            try
            {
                DataElement de = dc.get(components[i]);
                if(!(de instanceof DataCollection))
                    return null;
                dc = (DataCollection<?>)de;
            }
            catch( Exception e )
            {
                return null;
            }
        }
        return dc instanceof Module ? (Module)dc : null;
    }
    
    static public @Nonnull DataElementPath getModulePath(@Nonnull ru.biosoft.access.core.DataElement element)
    {
        return getModule(element).getCompletePath();
    }

    static public @CheckForNull DataElementPath optModulePath(DataElement element)
    {
        Module module = optModule(element);
        if(module == null)
            return null;
        return module.getCompletePath();
    }

    /**
     * List of necessary plugins divided by ';'
     */
    protected String pluginNames;

    //////////////////////////////////////////////////////////////////
    // Constructors
    //

    /**
     * Constructs derived data collection with primary collection.
     *
     * @param origin
     * @param properties Properties for creating data collection .
     */
    public Module(DataCollection<?> origin, Properties properties) throws Exception
    {
        super(origin, properties);
        //final String className = java.util.Objects.requireNonNull( properties.getProperty(TYPE_PROPERTY) );
        final String className = properties.getProperty(TYPE_PROPERTY);
        pluginNames = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        initModule();
        applyType(className);

        v_cache = new HashMap<>();
    }

    protected void initModule()
    {
        Properties primaryProperties = getPrimaryCollection().getInfo().getProperties();
        EntryStream.of(primaryProperties).forKeyValue( getInfo().getProperties()::putIfAbsent );
    }

    /**
     * Do nothing
     */
    @Override
    public void release(String name)
    {
    }

    /**
     * Apply current module type
     */
    protected void applyType(String className) throws Exception
    {
        type = ClassLoading.loadSubClass( className, pluginNames, ModuleType.class ).newInstance();
    }

    protected ModuleType type;

    /**
     * Get module type
     */
    public ModuleType getType()
    {
        return type;
    }

    /**
     * Get module version
     */
    public String getVersion()
    {
        return type.getVersion();
    }

    /**
     * Get data collection in this module for elements
     * with <code>Class</code> type or throw
     * <code>NullPointerException</code>
     */
    public <T extends DataElement> DataCollection<T> getCategory(Class<T> c)
    {
        if( Stub.class.isAssignableFrom(c) )
            return null;

        String name = type.getCategory(c);
        if( name == null )
            return null;

        DataElement de = CollectionFactory.getDataElement(name, this);
        if( de instanceof DataCollection )
            return (DataCollection)de;
        return null;
    }

    /**
     * @pending Temp for module removing
     * @return
     */
    public File getPath()
    {
        return new File(path);
    }

    //////////////////////////////////////////////////////////////////
    // Kernel data access methods
    //

    /**
     * Get kernel from module using relative name
     */
    public DataElement getKernel(String relativeName)
    {
        if( TextUtil.isFullPath(relativeName) )
        {
            return CollectionFactory.getDataElement(relativeName);
        }
        return CollectionFactory.getDataElement(relativeName, this);
    }

    /**
     * Get kernel of properly type with name
     */
    public <T extends DataElement> T getKernel(Class<T> c, String name) throws Exception
    {
        DataCollection<T> dc = getCategory(c);
        if( dc == null )
            return null;
        return dc.get(name);
    }

    /**
     * Put kernel in properly data collection
     */
    public <T extends DataElement> void putKernel(T kernel) throws Exception
    {
        @SuppressWarnings ( "unchecked" )
        Class<T> clazz = (Class<T>)kernel.getClass();
        DataCollection<T> dc = getCategory(clazz);
        if( dc != null )
            dc.put(kernel);
    }

    //////////////////////////////////////////////////////////////////
    // Diagram access methods
    //

    DataCollection<Diagram> diagrams;
    /**
     * Return diagrams data collection
     */
    public DataCollection<Diagram> getDiagrams()
    {
        if( diagrams == null )
        {
            try
            {
                diagrams = (DataCollection<Diagram>)get(DIAGRAM);
            }
            catch( Exception e )
            {
                throw new DataElementGetException( e, getCompletePath().getChildPath( DIAGRAM ) );
            }
        }

        return diagrams;
    }

    /**
     * Get diagram from diagrams data collection
     */
    public Diagram getDiagram(String name) throws Exception
    {
        return getDiagrams().get(name);
    }

    /**
     * Put diagram
     */
    public void putDiagram(Diagram diagram) throws Exception
    {
        if( diagrams == null )
            diagrams = (DataCollection<Diagram>)get(DIAGRAM);

        diagrams.put(diagram);
    }

    /**
     * List of external collection descriptions
     */
    protected List<CollectionDescription> externalTypes;

    /**
     * Get external types list
     */
    public List<CollectionDescription> getExternalTypes()
    {
        return externalTypes;
    }

    /**
     * Get external collections description for type
     */
    public CollectionDescription[] getExternalCategories(Class<?> c) throws Exception
    {
        if( getExternalTypes() == null )
            return null;

        if( Stub.class.isAssignableFrom(c) )
            return null;

        List<CollectionDescription> results = new ArrayList<>();

        for( CollectionDescription eType : getExternalTypes() )
        {
            DataCollection<?> dc = eType.getDc();
            if( dc == null )
            {
                DataElementPath completeName = DataElementPath.create("databases", eType.getModuleName());
                if( eType.getSectionName() != null )
                    completeName = completeName.getChildPath(eType.getSectionName());
                if( eType.getTypeName() != null && !eType.getTypeName().equals("") )
                    completeName = completeName.getChildPath(eType.getTypeName());
                dc = completeName.optDataCollection();
                eType.setDc(dc);
            }
            if( dc != null && dc.getDataElementType().getName().equals(c.getName()) )
            {
                results.add(eType);
            }
        }

        if( results.size() == 0 )
            return null;

        return results.toArray(new CollectionDescription[results.size()]);
    }

    /**
     * Get query systems for external databases
     */
    public QuerySystem[] getExternalLuceneFacades()
    {
        if( getExternalTypes() == null )
            return null;

        List<QuerySystem> results = new ArrayList<>();
        try
        {
            for( CollectionDescription eType : getExternalTypes() )
            {
                Module module = (Module)this.getOrigin().get(eType.getModuleName());
                if( module != null && module.getInfo() != null )
                {
                    QuerySystem lqs = module.getInfo().getQuerySystem();
                    if( !results.contains(lqs) )
                        results.add(lqs);
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "error while external databases loading", t);
            return null;
        }

        if( results.size() == 0 )
            return null;

        return results.toArray(new QuerySystem[results.size()]);
    }

    /**
     * Get external module name list
     */
    public @Nonnull String[] getExternalModuleNames()
    {
        if( getExternalTypes() == null )
            return new String[0];

        return getExternalTypes().stream().map( CollectionDescription::getModuleName ).distinct().toArray( String[]::new );
    }

    /**
     * Get external databases for data collection relative name
     */
    public Module[] getExternalModules(String relativeName)
    {
        if( getExternalTypes() == null )
            return null;

        List<Module> results = new ArrayList<>();
        try
        {
            for( CollectionDescription eType : getExternalTypes() )
            {
                DataCollection<?> dc = eType.getDc();
                Module module = (Module)this.getOrigin().get(eType.getModuleName());
                if( dc == null )
                {
                    DataElementPath completeName = DataElementPath.create("databases", eType.getModuleName());
                    if( eType.getSectionName() != null )
                        completeName = completeName.getChildPath(eType.getSectionName());
                    if( eType.getTypeName() != null )
                        completeName = completeName.getChildPath(eType.getTypeName());
                    dc = completeName.optDataCollection();
                    eType.setDc(dc);
                }
                // TODO: check this strange "indexOf" condition.
                if( module != null && dc != null && dc.getCompletePath().toString().indexOf(relativeName) != -1 )
                {
                    results.add(module);
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "error while external databases loading", t);
            return null;
        }

        if( results.size() == 0 )
            return null;

        return results.toArray(new Module[results.size()]);
    }
    
    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return getPrimaryCollection().getDescriptor(name);
    }

    @Override
    public URL getBase()
    {
        try
        {
            return getPath().toURI().toURL();
        }
        catch( MalformedURLException e )
        {
            return null;
        }
    }

    @Override
    public String getBaseId()
    {
        return path;
    }

    @Override
    public String getDescriptionHTML()
    {
        String description = getInfo().getDescription();
        if( description == null )
            return "";
        URL url = null;
        try
        {
            url = ( new File(getPath(), description) ).toURI().toURL();
        }
        catch( MalformedURLException e1 )
        {
        }
        if( url == null )
        {
            return description;
        }
        try
        {
            return ApplicationUtils.readAsString(url.openStream());
        }
        catch( Exception e )
        {
            return description;
        }
    }
    
    
    /**
     * Store description to description file
     * In case of error use getInfo().setDescrption()
     */
    public void setDescription(String description)
    {
        String descriptionFileName = getInfo().getDescription();
        File file = new File( getPath(), descriptionFileName );
        if( file.exists() )
        {
            try (PrintWriter writer = new PrintWriter( file, "UTF-8" ))
            {
                writer.write( description );
            }
            catch( IOException e )
            {
                getInfo().setDescription( description );
            }
        }
        else
        {
            getInfo().setDescription( description );
        }
    }
}
