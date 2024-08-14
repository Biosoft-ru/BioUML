package biouml.plugins.pharm.prognostic;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger( MessageBundle.class.getName() );

    protected static final Locale locale = new Locale( "en" );
    protected static final ResourceBundle resources = ResourceBundle.getBundle( MessageBundle.class.getName(), locale );

    public static String getMessage(String key)
    {
        return ( (MessageBundle)resources ).getResourceString( key );
    }

    public static String format(String key, Object ... arguments)
    {
        return MessageFormat.format( getMessage( key ), arguments );
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString( key );
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass() );
        }
        return key;
    }

    @Override
    protected Object[][] getContents()
    {
        return null;
    }
}
