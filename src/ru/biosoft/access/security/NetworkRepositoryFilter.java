package ru.biosoft.access.security;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.Filter;

public class NetworkRepositoryFilter implements Filter<DataElement>
{
    @Override
    public boolean isAcceptable(DataElement de)
    {
        Permission permission = SecurityManager.getPermissions(de.getCompletePath());
        if( ( permission.getPermissions() & Permission.READ ) != 0 )
        {
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
