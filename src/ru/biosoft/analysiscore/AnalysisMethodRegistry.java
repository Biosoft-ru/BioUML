package ru.biosoft.analysiscore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;

import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.DataElementImporterRegistry.ImporterInfo;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.exception.ExtensionInitException;
import ru.biosoft.access.generic.GenericTitleIndex;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysesGroupRegistry.GroupItem;
import ru.biosoft.analysiscore.javascript.JavaScriptAnalysisHost;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.VisiblePlugin;
import ru.biosoft.util.TextUtil2;

/**
 * Registry for analysis methods
 */
public class AnalysisMethodRegistry extends VisiblePlugin<DataCollection<AnalysisMethodInfo>>
{
    public static final String ADMIN_GROUP = "Admin";
    public static final String GROUP_ATTR = "group";
    public static final String NAME_ATTR = "name";
    public static final String CLASS_ATTR = "class";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String SHORT_DESCRIPTION_ATTR = "shortDescription";
    public static final String JS_ATTR = "js";
    
    private static Logger log = Logger.getLogger(AnalysisMethodRegistry.class.getName());
    private static AnalysisMethodRegistry instance;

    private Map<String, DataCollection<AnalysisMethodInfo>> groups;
    private Map<String, AnalysisMethodInfo> methods;
    private Map<String, String> old2new;

    /**
     * Constructor to be used by {@link CollectionFactoryUtils} to create a Plugin.
     */
    public AnalysisMethodRegistry(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        instance = this;
    }

    @Override
    public void startup()
    {
        try
        {
            loadExtensions( "ru.biosoft.analysiscore.method" );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not initialize AnalysisMethodRegistry, error:" + t, t);
        }
    }
    
    protected void loadExtensions(String extensionPointId)
    {
        groups = new TreeMap<>();
        methods = new TreeMap<>();
        old2new = new HashMap<>();

        IExtensionRegistry registry = Application.getExtensionRegistry();
        if(registry == null)
        {
            return;
        }
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        for( IConfigurationElement extension : extensions )
        {
            String pluginId = extension.getNamespaceIdentifier();
            String extensionName = extension.getName();
            String className = null;
            try
            {
                Set<String> methodGroups = new HashSet<>();
                String mainGroup = extension.getAttribute(GROUP_ATTR);
                if(mainGroup != null)
                    methodGroups.add( mainGroup );
                
                for(IConfigurationElement groupElem : extension.getChildren( "group" ))
                {
                    String groupName = groupElem.getAttribute( "name" );
                    if(groupName == null)
                        throw new Exception("group.name absent");
                    methodGroups.add( groupName );
                }
                
                if( methodGroups.isEmpty() )
                    throw new Exception("group absents");

                String name = extension.getAttribute(NAME_ATTR);
                if( name == null )
                    throw new Exception("name absents");
                extensionName = name;

                className = extension.getAttribute(CLASS_ATTR);
                if( className == null )
                    throw new Exception("class absents");

                String description = extension.getAttribute(DESCRIPTION_ATTR);
                if( description == null )
                    throw new Exception("description absents");
                
                String shortDescription = extension.getAttribute( SHORT_DESCRIPTION_ATTR );
                
                String js = extension.getAttribute(JS_ATTR);
                
                for(IConfigurationElement oldNameElem : extension.getChildren("old"))
                {
                    String oldName = oldNameElem.getAttribute( "name" );
                    if(oldName == null)
                        throw new Exception("old.name absent");
                    if(old2new.containsKey( oldName ))
                        throw new Exception("Duplicate old name: " + oldName + " -> " + name + "," + old2new.get( oldName ));
                    old2new.put( oldName, name );
                }

                Class<? extends AnalysisMethod> analysisClass = ClassLoading.loadSubClass( className, pluginId, AnalysisMethod.class );

                for( String group : methodGroups )
                {
                    DataCollection<AnalysisMethodInfo> parent = getOrCreateGroup( group );
                    
                    String mainJSFunction = js;
                    if(js ==null)
                        mainJSFunction = "analysis['" + name + "']";
                    
                    AnalysisMethodInfo info = new AnalysisMethodInfo( name, description, parent, analysisClass, mainJSFunction );
                    info.setShortDescription( shortDescription );

                    methods.put( name, info );
                    parent.put( info );
                    
                    if(js != null && js.contains("."))
                    {
                        String[] fields = js.split("\\.");
                        JavaScriptAnalysisHost.addAnalysis(fields[0], fields[1], info);
                    }
                    
                    JavaScriptAnalysisHost.addAnalysis( "analysis", name, info );    
                }
                

                
            }
            catch( Throwable t )
            {
                new ExtensionInitException(t, extensionPointId, extensionName, className).log();
            }

        }
        
        loadImporters();
        JavaScriptAnalysisHost.lock();
    }
    
    private AnalysesGroup getOrCreateGroup(String groupName)
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, AnalysisMethodInfo.class.getName());
        properties.put("lucene-indexes", "name;descriptionHTML" );
        
        AnalysesGroup result = (AnalysesGroup)groups.computeIfAbsent( groupName, k -> new AnalysesGroup( k, this, properties ) );
        
        result.getInfo().getProperties().setProperty( QuerySystem.INDEX_LIST, "title" );
        result.getInfo().getProperties().setProperty( "index.title", GenericTitleIndex.class.getName() );
        DefaultQuerySystem qs = new DefaultQuerySystem( result );
        result.getInfo().setQuerySystem( qs );
        
        GroupItem groupItem = AnalysesGroupRegistry.instance.getExtension( groupName );
        if(groupItem != null)
        {
            result.setDescription( groupItem.description );
            result.setRelated( groupItem.relatedGroups );
        }
        
        return result;
    }
    
    /**
     * Add new method to analysis registry
     */
    public static void addMethod(String name, AnalysisMethodInfo ami)
    {
        if(instance != null && instance.methods != null)
        {
            instance.methods.put(name, ami);
        }
    }
    
    public static void addMethodToGroup(String methodName, String groupName, AnalysisMethodInfo ami)
    {
        if(instance != null && instance.methods != null)
        {
            instance.methods.put(methodName, ami);
            DataCollection<AnalysisMethodInfo> parent = instance.getOrCreateGroup( groupName );
            AnalysisMethodInfo amiClone = ami.clone( parent, methodName );
            parent.put( amiClone );
        }
    }

    private void loadImporters()
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, AnalysisMethodInfo.class.getName());
        DataCollection<AnalysisMethodInfo> parent = groups.containsKey("Import") ? groups.get("Import") : new AnalysesGroup("Import", this, properties);
        groups.put("Import", parent);
        for(ImporterInfo importerInfo: DataElementImporterRegistry.importers())
        {
            try
            {
                AnalysisMethodInfo info = new AnalysisMethodInfo(importerInfo.getFormat(), importerInfo.getDescription(), parent, ImportAnalysis.class);
                info.setShortDescription( "" );
                methods.put(importerInfo.getFormat(), info);
                parent.put(info);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Error adding importer '"+(importerInfo==null?null:importerInfo.getFormat())+"' to analysis list", e);
            }
        }
    }

    @Override
    public @Nonnull Class<DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return StreamEx.ofKeys(groups)
                .remove( AnalysisMethodRegistry::isGroupProtected )
                .remove( name->groups.get( name ).isEmpty() )//Don't show empty analyses categories
                .toList();
    }

    @Override
    protected DataCollection<AnalysisMethodInfo> doGet(String name)
    {
        if(isGroupProtected( name )) return null;
        return groups.get(name);
    }
    
   
    protected static boolean isProtectionEnabled = true;

    private static boolean isGroupProtected(String name)
    {
        if(name.equals(ADMIN_GROUP))
            return !SecurityManager.isAdmin();
        if(!isProtectionEnabled)
            return false;
        Permission permission = SecurityManager.getPermissions(DataElementPath.create( "analyses/Methods/" + name ));
        return !permission.isReadAllowed();
    }
    
    protected DataCollection<AnalysisMethodInfo> getAdminGroup()
    {
        return groups.get(ADMIN_GROUP);
    }

    protected AnalysisMethodInfo getMethod(String methodName)
    {
        String[] parts = TextUtil2.split( methodName, '/' );
        
        String justName = parts.length > 1 ? parts[1] : methodName;
        String groupName = parts.length > 1? parts[0] : null;
        if(!methods.containsKey( justName ) && old2new.containsKey( justName ))
        {
            justName = old2new.get( justName );
        }

        if( groupName != null )
        {
            DataCollection<AnalysisMethodInfo> group = instance.doGet(groupName);
            if( group != null )
            {
                try
                {
                    AnalysisMethodInfo ami = group.get(justName);
                    if(ami != null)
                        return ami;
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not get analysis", e);
                }
            }
        }

        AnalysisMethodInfo analysisMethodInfo = methods.get( justName );
        if( analysisMethodInfo != null && analysisMethodInfo.getOrigin() != null
                && analysisMethodInfo.getOrigin().getName().equals(ADMIN_GROUP) && !SecurityManager.isAdmin() )
            return null;
        return analysisMethodInfo;
    }

    ///////////////////////////////////////////////////////////////////
    // utility methods
    //

    protected static void init()
    {
        if( instance == null )
        {
            Plugins.getPlugins();
        }
    }

    public static AnalysisMethodInfo getMethodInfo(String methodName)
    {
        init();
        if( instance != null )
        {
            return instance.getMethod(methodName);
        }
        return null;
    }

    //return all analysis groups
    public static @Nonnull StreamEx<String> getAnalysisGroups()
    {
        init();
        if( instance != null )
        {
            return StreamEx.of(instance.getNameList());
        }
        return StreamEx.empty();
    }

    ///////////////////////////////////////////////////////////////////
    // utility methods
    //
    
    //get all analysis names from group id
    public static @Nonnull StreamEx<String> getAnalysisNames(String id)
    {
        init();
        if( instance != null )
        {
            return StreamEx.of( instance.doGet( id ).names() );
        }
        return StreamEx.empty();
    }

    public static @Nonnull StreamEx<String> getAnalysisNamesWithGroup()
    {
        return getAnalysisGroups().remove( AnalysisMethodRegistry::isGroupProtected )
                .cross( AnalysisMethodRegistry::getAnalysisNames ).join("/");
    }

    public static AnalysisMethod getAnalysisMethod(String methodName)
    {
        init();
        if( instance != null )
        {
            AnalysisMethodInfo ami = instance.getMethod( methodName );
            return ami == null ? null : ami.createAnalysisMethod();
        }
        return null;
    }
    
    @SuppressWarnings ( "unchecked" )
    public static <T extends AnalysisMethod> T getAnalysisMethod(Class<T> clazz)
    {
        init();
        if( instance != null )
        {
            for(AnalysisMethodInfo ami : instance.methods.values())
                try
                {
                    if(ami.getAnalysisClass().equals(clazz))
                    {
                        return (T)ami.createAnalysisMethod();
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not get analysis", e);
                }
        }
        return null;
    }
    
    public static void addTestAnalysis(String analysisName, Class<? extends AnalysisMethod> analysisClass)
    {
        if(!SecurityManager.isTestMode())
            throw new SecurityException();
        if(instance == null)
            new AnalysisMethodRegistry(null, new Properties());
        instance.doAddTestAnalysis(analysisName, analysisClass);
    }

    private void doAddTestAnalysis(String analysisName, Class<? extends AnalysisMethod> analysisClass)
    {
        AnalysisMethodInfo info = new AnalysisMethodInfo(analysisName, "Test", null, analysisClass, null);
        methods.put(analysisName, info);
    }
}
