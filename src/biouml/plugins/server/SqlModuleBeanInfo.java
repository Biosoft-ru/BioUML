package biouml.plugins.server;

import biouml.model.ModuleBeanInfo;

public class SqlModuleBeanInfo extends ModuleBeanInfo
{
    
    public SqlModuleBeanInfo ( )
    {
        this ( SqlModule.class, "SQLDATABASE", MessageBundle.class.getName ( ) );
    }

    protected SqlModuleBeanInfo ( Class beanClass, String key, String messageBundle )
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
