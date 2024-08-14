package ru.biosoft.access.users;

import java.util.logging.Level;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.access.security.SecurityAdminUtils;

/**
 * {@link ru.biosoft.access.core.DataCollection} for users repository
 */
public class UsersRepository extends ReadOnlyVectorCollection<UserGroup>
{
    protected static final Logger log = Logger.getLogger(UsersRepository.class.getName());

    protected JabberProvider jabberProvider;

    public UsersRepository(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String jabberProviderName = properties.getProperty(JabberProvider.JABBER_PROVIDER);
        if( jabberProviderName == null )
        {
            this.jabberProvider = new SQLJabberProvider(properties);
        }
        else
        {
            try
            {
                this.jabberProvider = ClassLoading.loadSubClass( jabberProviderName, JabberProvider.class )
                        .getConstructor( Properties.class ).newInstance( properties );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot init jabber provider", e);
            }
        }
    }

    /**
     * Get jabber info provider for {@link ru.biosoft.access.core.DataElement}
     * @param de
     * @return
     */
    public static JabberProvider getJabberProvider(DataElement de)
    {
        DataCollection<?> dc = de.getOrigin();
        while( ( dc != null ) && ! ( dc instanceof UsersRepository ) )
        {
            dc = dc.getOrigin();
        }
        if( dc != null )
            return ( (UsersRepository)dc ).getJabberProvider();
        return null;
    }

    public JabberProvider getJabberProvider()
    {
        return jabberProvider;
    }

    @Override
    public @Nonnull Class<UserGroup> getDataElementType()
    {
        return UserGroup.class;
    }

    /**
     * Recreate all group structure
     */
    public void reinit()
    {
        reset();
    }

    @Override
    protected void doInit()
    {
        try
        {
            List<String> groupNames = SecurityAdminUtils.getGroupNameList();
            for( String groupName : groupNames )
            {
                Properties prop = new Properties();
                prop.put(DataCollectionConfigConstants.NAME_PROPERTY, groupName);
                UserGroup userGroup = new UserGroup(this, prop);

                doRemove(userGroup.getName());
                doPut(userGroup, true);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot init group list", e);
        }
    }
}
