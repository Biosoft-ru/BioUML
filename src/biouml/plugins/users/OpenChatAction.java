package biouml.plugins.users;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.access.users.UserInfo;

public class OpenChatAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        UsersModule userModule = UsersModule.getUsersModule(de);
        if( userModule != null )
        {
            userModule.openChatDialog(de.getName());
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return de instanceof UserInfo;
    }
}
