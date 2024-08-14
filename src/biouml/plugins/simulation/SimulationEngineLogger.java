package biouml.plugins.simulation;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimulationEngineLogger
{
    public SimulationEngineLogger()
    {
        this(SimulationEngine.class);
    }

    public SimulationEngineLogger(Class clazz)
    {
        log = Logger.getLogger( clazz.getName() );
    }

    public SimulationEngineLogger(String messageBundle, Class clazz)
    {
        this(clazz);
        initResources(messageBundle, clazz);
    }

    private Logger log;

    /** Resource bundle containing necessary String data. This resource bundle contains join of childResources with parentResources. */
    protected ResourceBundle resources = null;


    /** Resource bundle containing necessary String data. */
    protected ResourceBundle childResources = null;

    /**Common description is located in biouml.plugins.simulation.resources.MessagesBundle. This bundle is used as parent bundle for resources. */
    protected static final ResourceBundle parentResources = ResourceBundle
            .getBundle( biouml.plugins.simulation.resources.MessageBundle.class.getName() );

    public ResourceBundle getResourceBundle()
    {
        return resources;
    }

    public Logger getLogger()
    {
        return log;
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code>.
     */
    public String getResourceString(String key)
    {
        try
        {
            return resources.getString( key );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE,  "Missing resource <" + key + ">." );
        }

        return key;
    }

    /** Initialize resources necessary to retrieve localized strings. */
    protected void initResources(String resourceBundleName, Class<?> c)
    {
        try
        {
            if( resourceBundleName == null )
            {
                resources = parentResources;
                childResources = resources;
                return;
            }

            ClassLoader cl = c.getClassLoader();

            childResources = ( cl != null ) ? ResourceBundle.getBundle( resourceBundleName, Locale.getDefault(), cl ) : ResourceBundle
                    .getBundle( resourceBundleName, Locale.getDefault() );

            if( parentResources == null )
            {
                resources = childResources;
                return;
            }

            // if childResources & parentResources both != null
            // we should create new ResourceBundle, that joins them.
            resources = new ResourceBundle()
            {
                /**
                 * Override of ResourceBundle, same semantics. The only difference, if
                 * resource is not found,
                 * it will be search in parentResources.
                 */
                @Override
                protected Object handleGetObject(String key) throws MissingResourceException
                {
                    MissingResourceException mre;
                    try
                    {
                        return childResources.getObject( key );
                    }
                    catch( MissingResourceException e )
                    {
                        mre = e;
                        try
                        {
                            return parentResources.getObject( key );
                        }
                        catch( MissingResourceException ee )
                        {
                        }
                        throw mre;
                    }
                }

                /** Implementation of ResourceBundle.getKeys. */
                @Override
                public Enumeration<String> getKeys()
                {
                    Enumeration<String> result = new Enumeration<String>()
                    {
                        final Enumeration<String> childKeys = childResources.getKeys();
                        final Enumeration<String> parentKeys = parentResources.getKeys();
                        String temp = null;
                        @Override
                        public boolean hasMoreElements()
                        {
                            if( temp == null )
                                nextElement();
                            return temp != null;
                        }

                        @Override
                        public String nextElement()
                        {
                            String returnVal = temp;
                            if( childKeys.hasMoreElements() )
                            {
                                temp = childKeys.nextElement();
                                return returnVal;
                            }
                            temp = null;
                            while( temp == null && parentKeys.hasMoreElements() )
                            {
                                temp = parentKeys.nextElement();
                                // check if childResources bundle contains the temp as key
                                try
                                {
                                    childResources.getObject( temp );
                                    temp = null;
                                }
                                catch( MissingResourceException e )
                                {
                                }
                            }
                            return returnVal;
                        } // nextElement
                    }; // Enumeration
                    return result;
                } // getKeys
            }; // ResourceBundle
        } // try
        catch( MissingResourceException mre )
        {
            log.log(Level.SEVERE,  "Resource '" + resourceBundleName + "' can not be initilized", mre );
        }
    }

    public static String getCurrentTime()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("[ HH:mm:ss ] ");
        return sdf.format(cal.getTime());
    }

    public void info(String message)
    {
        log.info(getCurrentTime() + message);
    }

    public void warn(String message)
    {
        log.warning(getCurrentTime() + message);
    }

    public void error(String message, Exception ex)
    {
        log.log(Level.SEVERE, getCurrentTime() + message, ex);
    }

    public void error(String message)
    {
        log.log(Level.SEVERE, getCurrentTime() + message);
    }

    public void error(String messageBundleKey, String[] params)
    {
        String message = getResourceString( messageBundleKey );
        message = MessageFormat.format( message, (Object[])params );
        log.log(Level.SEVERE,  message );
    }

    public void error(String messageBundleKey, String[] params, Throwable t)
    {
        String message = getResourceString( messageBundleKey );
        message = MessageFormat.format( message, (Object[])params );
        log.log(Level.SEVERE,  message, t );
    }

    public void warn(String messageBundleKey, String[] params)
    {
        String message = getResourceString( messageBundleKey );
        message = MessageFormat.format( message, (Object[])params );
        log.warning( message );
    }
}
