package ru.biosoft.workbench.editors;

import java.util.ListResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger( MessageBundle.class.getName() );

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"CN_CLASS", "Parameters"},
            {"CD_CLASS", "Parameters"},
        };
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
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
}
