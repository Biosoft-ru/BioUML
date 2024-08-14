package ru.biosoft.access.users;

import java.util.logging.Level;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.access.security.SecurityAdminUtils;

/**
 * Group for BioUML users.
 */
public class UserGroup extends ReadOnlyVectorCollection<UserInfo>
{
    private static final DataElementDescriptor OFFLINE_DESCRIPTOR = new DataElementDescriptor(UserInfo.class, ClassLoading.getResourceLocation( UserGroup.class, "resources/offline.gif" ), true);
    private static final DataElementDescriptor ONLINE_DESCRIPTOR = new DataElementDescriptor(UserInfo.class, ClassLoading.getResourceLocation( UserGroup.class, "resources/online.gif" ), true);

    public UserGroup(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return UserInfo.class;
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        JabberProvider jabber = UsersRepository.getJabberProvider(this);
        if( ( jabber != null ) && ( jabber.isUserOnline(name) ) )
        {
            return ONLINE_DESCRIPTOR;
        }
        return OFFLINE_DESCRIPTOR;
    }

    @Override
    protected void doInit()
    {
        try
        {
            List<UserInfo> users = SecurityAdminUtils.getGroupUsers(getName(), this);
            for( UserInfo user : users )
            {
                doPut(user, true);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot init user list for group: " + getName(), e);
        }
    }
}
