package biouml.workbench;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;

/**
 * @author lan
 *
 */
public abstract class ConsoleApplicationSupport implements IApplication
{
    /**
     * Special logger for console output
     */
    protected static final ConsoleLogger log = new ConsoleLogger();

    protected void loadPreferences(String prefFileName)
    {
        if( prefFileName == null )
            return;
        String fileName = Platform.getInstallLocation().getURL().getPath() + prefFileName;
        Preferences preferences = new Preferences();

        try
        {
            ClassLoader cl = this.getClass().getClassLoader();
            preferences.load(fileName, cl);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Load preferences error", t);
        }

        Application.setPreferences(preferences);
    }

    protected static class ConsoleLogger extends Logger
    {
        protected ConsoleLogger()
        {
            super( "Console logger", null );
        }

        @Override
        public void fine(String msg)
        {
            info( msg );
        }

        @Override
        public void info(String msg)
        {
            System.out.println( msg );
        }

        @Override
        public boolean isLoggable(Level level)
        {
            if( level.intValue() > Level.FINE.intValue() )
                return false;
            return super.isLoggable( level );
        }

        @Override
        public void log(Level level, String msg, Throwable t)
        {
            info( msg );
            if( level.intValue() > Level.FINE.intValue() )
                t.printStackTrace();
        }

        @Override
        public void log(Level level, String msg)
        {
            info( msg );
        }

        @Override
        public void severe(String msg)
        {
            info( msg );
        }

        @Override
        public void warning(String msg)
        {
            info( msg );
        }
    }

    @Override
    public void stop()
    {
    }
}
