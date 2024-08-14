
package biouml.standard;

import com.developmentontheedge.application.Application;

public class SqlModuleType extends StandardModuleType
{
    
    @Override
    public boolean canCreateEmptyModule ( )
    {
        return false;
    }

    public SqlModuleType ( )
    {
        super ( Application.getGlobalValue("ApplicationName")+" standard (SQL)" );
    }

}
