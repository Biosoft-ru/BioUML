package biouml.plugins.jupyter.auth;

import biouml.plugins.jupyter.configuration.JupyterConfiguration;
import ru.biosoft.access.ClassLoading;

public class AccessorFactory
{
    public static JupyterAccessor getAccessor(JupyterConfiguration conf) throws Exception
    {
        if( !conf.isConfigured() )
        {
            return new DummyJupyterAccessor();
        }

        if( conf.useLocalNotebook() )
        {
            return new DummyJupyterAccessor();
        }

        String className = conf.getAccessorClass();
        if( className == null )
        { 
            return new BioumlJupyterAccessor( conf );
        } 

        Class<? extends JupyterAccessor> clazz = ClassLoading.loadSubClass( className, JupyterAccessor.class );
        return clazz.getConstructor( JupyterConfiguration.class ).newInstance( conf );
    }
}
