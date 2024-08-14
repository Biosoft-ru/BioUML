package ru.biosoft.access.generic;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.ListUtil;

/**
 * DataCollection which returns different elements for different sessions
 * @author lan
 */
public class SessionCollection extends GenericDataCollection
{
    /**
     * Should be used for standalone versions
     */
    public static final String STATIC_SESSION_PROPERTY = "staticSession";
    private static final long WIPE_DELAY = 5000; // ms
    private Repository repository;
    private long lastWipeTime = 0;
    private final UUID staticSession = UUID.randomUUID();
    
    protected void wipeOldSessions()
    {
        if(lastWipeTime == -1 || System.currentTimeMillis()-lastWipeTime < WIPE_DELAY) return;
        lastWipeTime = -1;
        for(final Object name: repository.getNameList())
        {
            String session = name.toString();
            if(!session.equals(staticSession.toString()) && SecurityManager.isSessionInvalid(session))
            {
                log.info("Wiping session collection "+getCompletePath()+"/"+session);
                (new SessionThread()
                {
                    @Override
                    public void doRun()
                    {
                        try
                        {
                            repository.remove(name.toString());
                        }
                        catch( Exception e )
                        {
                        }
                        ApplicationUtils.removeDir(new File(SessionCollection.super.getRootDirectory(), name.toString()));
                    }
                }).start();
                break;
            }
        }
        lastWipeTime = System.currentTimeMillis();
    }
    
    protected GenericDataCollection getSessionCollection()
    {
        wipeOldSessions();
        String session = null;
        if(getInfo().getProperty(STATIC_SESSION_PROPERTY) != null)
        {
            session = staticSession.toString();
        }
        else
        {
            session = SecurityManager.getSession();
            if(session.equals(SecurityManager.SYSTEM_SESSION) || SecurityManager.isSessionInvalid(session)) return null;
        }
        try
        {
            if(!repository.contains(session))
                createSessionEntry(session);
            return (GenericDataCollection)DataCollectionUtils.fetchPrimaryElement(repository.get(session), Permission.WRITE);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Unable to get session-specific collection for "+session, e);
            return null;
        }
    }
    
    /**
     * Creates an entry for the specified session
     * @param session to create entry for (assumed that it's not exist yet)
     * @throws Exception
     */
    protected void createSessionEntry(final String session) throws Exception
    {
        SecurityManager.runPrivileged(() -> {
            new File(getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY), session).mkdir();
            DataCollection collection = GenericDataCollection.createGenericCollection(SessionCollection.this, repository, session, session);
            DataCollection primary = DataCollectionUtils.fetchPrimaryCollectionPrivileged(collection);
            primary.getInfo().getProperties().setProperty(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            primary.getInfo().getProperties().setProperty(LocalRepository.PUT_TO_REPOSITORY, "true");
            primary.getInfo().getProperties().setProperty( DataCollectionConfigConstants.REMOVE_CHILDREN, "true" );
            primary.getInfo().getProperties().setProperty(GenericDataCollection.SKIP_PARENT, "true");
            primary.getInfo().getProperties().setProperty(GenericDataCollection.SKIP_UPDATE_SIZES, "true");
            repository.put(primary);
            repository.put(primary);
            return null;
        });
    }

    /**
     * Initializes repository
     * @throws Exception
     */
    private void initRepository() throws Exception
    {
        ExProperties propRepository = new ExProperties();
        propRepository.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        propRepository.setProperty(LocalRepository.PARENT_COLLECTION, getOrigin().getCompletePath().toString());
        propRepository.setProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        propRepository.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, getName());
        
        repository = (Repository)CollectionFactory.createCollection(null, propRepository);
    }

    public SessionCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);
        initRepository();
    }

    @Override
    public @Nonnull Iterator<DataElement> iterator()
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?ListUtil.<DataElement>emptyIterator():sessionCollection.iterator();
    }

    @Override
    public boolean contains(String name)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?false:sessionCollection.contains(name);
    }

    @Override
    public DataElement get(String name) throws Exception
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.get(name);
    }

    @Override
    public DataElement put(DataElement element) throws DataElementPutException
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.put(element);
    }

    @Override
    public void remove(String name) throws Exception, UnsupportedOperationException
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        if(sessionCollection != null) sessionCollection.remove(name);
    }

    @Override
    public void release(String name)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        if(sessionCollection != null) sessionCollection.release(name);
    }

    @Override
    public void close() throws Exception
    {
        repository.close();
        super.close();
    }

    @Override
    public String getDescription()
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getDescription();
    }

    @Override
    public void setDescription(String description)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        if(sessionCollection != null)
            sessionCollection.setDescription(description);
    }

    @Override
    public String getRootDirectory()
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getRootDirectory();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?Collections.<String>emptyList():sessionCollection.getNameList();
    }

    @Override
    protected DataElementInfo getChildInfo(String name) throws Exception
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getChildInfo(name);
    }

    @Override
    public int getSize()
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?0:sessionCollection.getSize();
    }

    @Override
    public boolean contains(DataElement de)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?false:sessionCollection.contains(de);
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getDescriptor(name);
    }
    
    @Override
    public DataCollection<?> getTypeSpecificCollection(DataElementTypeDriver driver)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getTypeSpecificCollection( driver );
    }
    
    @Override
    public DataCollection<?> getTypeSpecificCollection(Class<? extends DataElement> child)
    {
        GenericDataCollection sessionCollection = getSessionCollection();
        return sessionCollection == null?null:sessionCollection.getTypeSpecificCollection( child );
    }

    @Override
    protected void initSize()
    {
        diskSize = -1;
    }

    @Override
    protected void updateSize(long size, boolean propagate)
    {
    }

    @Override
    protected long estimateSize(String name, boolean update)
    {
        return -1;
    }
    
    
}
