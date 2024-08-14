package biouml.plugins.server.access;

import biouml.model.ModuleBeanInfo;
import biouml.plugins.server.MessageBundle;

public class ClientModuleBeanInfo extends ModuleBeanInfo
{

    public ClientModuleBeanInfo ( )
    {
        this ( ClientModule.class, "DATABASE", MessageBundle.class.getName ( ) );
    }

    protected ClientModuleBeanInfo ( Class beanClass, String key, String messageBundle )
    {
        super ( beanClass, key, messageBundle );
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        super.initProperties ( );
        
        super.initResources ( MessageBundle.class.getName ( ) );
    }
    
}
