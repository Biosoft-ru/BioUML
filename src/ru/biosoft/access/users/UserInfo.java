package ru.biosoft.access.users;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Bean with user info. Is used as element in special data collections.
 */
public class UserInfo extends DataElementSupport
{
    public UserInfo(DataCollection origin, String name)
    {
        super(name, origin);
    }
}
