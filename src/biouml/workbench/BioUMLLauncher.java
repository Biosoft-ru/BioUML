package biouml.workbench;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JWindow;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.splash.BioUMLConsoleSplasher;

public class BioUMLLauncher implements IApplication
{
    @Override
    public Object start(IApplicationContext arg0) throws Exception
    {
        try
        {
            //PropertyConfigurator.configure( "biouml.lcf" );
            File configFile = new File( "biouml.lcf" );
            try( InputStream inputStream = new FileInputStream( configFile ) )
            {
                try
                {
                    LogManager.getLogManager().readConfiguration( inputStream );
                }
                catch( IOException e )
                {
                    Logger.getGlobal().log( Level.SEVERE, "init logging system", e );
                }
            }

            Object appArgs = arg0.getArguments().get( "application.args" );
            if( ! ( appArgs instanceof String[] ) )
            {
                if( appArgs == null )
                    appArgs = "null";
                System.out.println( "Can not start: incorrect input application arguments (" + appArgs + ")" );
                endOnPress();
                return IApplication.EXIT_OK;
            }

            String[] args = (String[])appArgs;

            //splash screen while starting
            JWindow splash = null;
            try
            {
                URL url = new URL( BioUMLLauncher.class.getResource( "resources/" ), "AboutLogo.png" );
                splash = ApplicationUtils.createSplashScreen( url );
            }
            catch( Throwable t )
            {
                System.out.println( "Can not load splash screen: " + t );
            }

            //taskbar icon support
            Image icon = null;
            try
            {
                URL url = BioUMLLauncher.class.getResource( "resources/biouml.gif" );
                icon = Toolkit.getDefaultToolkit().getImage( url );
            }
            catch( Throwable t )
            {
                System.out.println( "Can not load taskbar icon: " + t );
            }

            Class<?> workbench = BioUMLApplication.class;
            Constructor<?> constr = workbench.getConstructor( Image.class, String[].class );
            constr.newInstance( icon, args );

            BioUMLConsoleSplasher.printSplash();

            if( splash != null )
                splash.dispose();

            while( true )
            {
                Thread.sleep( 1000 );
            }
        }
        catch( Throwable t )
        {
            System.err.println( "Can not start application:" + t );
            t.printStackTrace();

            endOnPress();
        }

        return IApplication.EXIT_OK;
    }

    private void endOnPress()
    {
        System.out.println( "\nPress any key to continue" );
        try
        {
            System.in.read();
        }
        catch( Throwable ignore )
        {
        }
    }

    @Override
    public void stop()
    {
    }

}
