package biouml.plugins.users;

import java.beans.BeanInfo;

import biouml.plugins.server.access.ClientModuleBeanInfo;

/**
 * {@link BeanInfo} for {@link UsersModule}
 */
public class UsersModuleBeanInfo extends ClientModuleBeanInfo
{
    public UsersModuleBeanInfo()
    {
        super(UsersModule.class, "DATABASE", MessageBundle.class.getName ( ) );
    }
}
