package biouml.plugins.research;

import java.io.File;
import java.util.Properties;

import biouml.model.Module;
import biouml.model.ProtectedModule;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.generic.SessionCollection;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.util.ExProperties;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builder for research modules
 */
public class ResearchBuilder
{
    protected static final Logger log = Logger.getLogger( ResearchBuilder.class.getName() );

    private static String DESCRIPTION_FILE = "description.html";

    protected Properties properties;

    public ResearchBuilder(Properties properties)
    {
        this.properties = properties;
    }

    /**
     * Create research in {@link Repository}
     */
    public DataCollection<?> createResearch(Repository parent, String name, boolean isProtected) throws Exception
    {
        log.log( Level.INFO, "createResearch: START" );
        long before = System.currentTimeMillis();

        // Create Module data collection (root)
        Properties primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName());

        Properties transformed = new Properties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, Module.class.getName());
        transformed.setProperty(Module.TYPE_PROPERTY, ResearchModuleType.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.research");
        transformed.setProperty(DataCollectionConfigConstants.REMOVE_CHILDREN, "true");
        transformed.setProperty(DataCollectionConfigConstants.DESCRIPTION_PROPERTY, DESCRIPTION_FILE);
        transformed.setProperty(QuerySystem.QUERY_SYSTEM_CLASS, "biouml.plugins.lucene.LuceneQuerySystemImpl");
        transformed.setProperty("lucene-directory", "luceneIndex");

        Module research = (Module)CollectionFactoryUtils.createDerivedCollection(parent, name, primary, transformed, name);
        new File(research.getPath(), DESCRIPTION_FILE).createNewFile();
        LocalRepository researchLR = (LocalRepository)research.getPrimaryCollection();

        primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, "default.dat");
        primary.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        primary.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"");
        primary.setProperty(EntryCollection.ENTRY_KEY_FULL, "true");
        
        Properties derived = new Properties();
        derived.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, Module.DATA);
        derived.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, GenericDataCollection.class.getName());
        derived.setProperty(DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        derived.setProperty(LocalRepository.PARENT_COLLECTION, researchLR.getCompletePath().toString());
        if( properties != null )
        {
            derived.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY));
            derived.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, properties
                    .getProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY));
            String historyCollection = properties.getProperty(HistoryFacade.HISTORY_COLLECTION);
            if(historyCollection != null)
                derived.setProperty(HistoryFacade.HISTORY_COLLECTION, historyCollection);
        }

        CollectionFactoryUtils.createDerivedCollection(researchLR, Module.DATA, primary, derived, Module.DATA);

        primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "tmp");
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, "default.dat");
        primary.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        primary.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"");
        primary.setProperty(EntryCollection.ENTRY_KEY_FULL, "true");
        
        derived = new Properties();
        derived.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "tmp");
        derived.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, SessionCollection.class.getName());
        if(!isProtected) derived.setProperty(SessionCollection.STATIC_SESSION_PROPERTY, "true");
        derived.setProperty(DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        derived.setProperty(LocalRepository.PARENT_COLLECTION, researchLR.getCompletePath().toString());
        if( properties != null )
        {
            derived.setProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_DRIVER_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_URL_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_USER_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY));
            derived.setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, properties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY));
            derived.setProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY, properties
                    .getProperty(GenericDataCollection.PREFERED_TABLE_IMPLEMENTATION_PROPERTY));
        }

        CollectionFactoryUtils.createDerivedCollection(researchLR, "tmp", primary, derived, "tmp");
        
        // create journal collection
        CollectionFactoryUtils.createTransformedCollection(researchLR, "Journal", BeanInfoEntryTransformer.class, TaskInfo.class, null, null,
                ".dat", "ID", "ID", "//", null);
        DataCollection<?> journalDC = researchLR.get( "Journal" );
        journalDC.getInfo().writeProperty(DataCollectionConfigConstants.IS_LEAF, "true");
        journalDC.getInfo().writeProperty(DataCollectionConfigConstants.NODE_IMAGE, ClassLoading.getResourceLocation( getClass(), "resources/journal.gif" ));

        if( isProtected )
        {
            File primaryConfig = new File(researchLR.getRootDirectory(), DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            primaryConfig.renameTo(new File(researchLR.getRootDirectory(), "default.primary.config"));
            Properties protectedProperties = new Properties();
            protectedProperties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
            protectedProperties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, ProtectedModule.class.getName());
            protectedProperties.setProperty(DataCollectionConfigConstants.NEXT_CONFIG, "default.primary.config");
            protectedProperties.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.research");
            protectedProperties.setProperty(ProtectedDataCollection.PROTECTION_STATUS, "2");
            Long quota = SecurityManager.getCurrentUserPermission().getLimit("project_quota");
            if( quota != null )
            {
                log.log( Level.INFO, "QUOTA for the \"" + name + "\" is " + quota + "." );
                protectedProperties.setProperty(DataCollectionConfigConstants.DISK_QUOTA_PROPERTY, String.valueOf(quota));
            }
            else
            {
                log.log( Level.INFO, "NO QUOTA for the \"" + name + "\"." );
            } 
            File defaultConfig = new File(researchLR.getRootDirectory(), DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            ExProperties.store(protectedProperties, defaultConfig);
            parent.release(name);
            research = research.getCompletePath().getDataElement( ProtectedModule.class );
        }

        long after = System.currentTimeMillis();
        log.log( Level.INFO, "createResearch: END " + ( after - before ) + "ms" );
        return research;
    }
}
