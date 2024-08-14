package ru.biosoft.plugins.javascript;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Starts JScrip shell from CollectionFactory class loader.
 */
public class JScriptRunner implements IApplication
{
    /** Top level function that starts the shell. */
    @Override
    public Object start(IApplicationContext arg)
    {
        try
        {
            Class<?> c = Class.forName( JScriptShell.class.getName() );
            IApplication shell = (IApplication)c.newInstance();
            return shell.start( arg );
        }
        catch( Throwable t )
        {
            System.out.println("Can not start JavaScript shell, error: " + t);
            t.printStackTrace();
            return null;
        }
    }

    @Override
    public void stop()
    {
    }
}
