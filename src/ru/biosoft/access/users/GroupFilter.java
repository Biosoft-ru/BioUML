package ru.biosoft.access.users;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.security.SecurityManager;

/**
 * Filter {@link UserGroup}, available groups:
 *  - support
 *  - current user group
 */
public class GroupFilter implements Filter<UserGroup>
{
    @Override
    public boolean isAcceptable(UserGroup de)
    {
        if(de.getName().equals( "Support" ))
        {
            return true;
        }
        return SecurityManager.getPermissions( DataElementPath.create( "groups", de.getName() ) ).isReadAllowed();
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
