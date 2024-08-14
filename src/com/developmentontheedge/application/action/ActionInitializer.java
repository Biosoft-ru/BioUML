package com.developmentontheedge.application.action;

import java.net.URL;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains common rutines to init actions by the data from the
 * corresponding MessageBundle.
 */
public class ActionInitializer
{
    private static final Logger log = Logger.getLogger( ActionInitializer.class.getName() );
    private final ResourceBundle[] resources;

    public ActionInitializer(Class<?>... classes)
    {
        resources = Stream.of( classes ).map( ActionInitializer::createResourceBundle ).filter( Objects::nonNull )
                .toArray( ResourceBundle[]::new );
    }


    /**
     * Inits ActionInitializer with the specified ResourceBundle and class, used
     * for resource loading.
     */
    public ActionInitializer(ResourceBundle r)
    {
        resources = new ResourceBundle[] {r};
    }

    /**
     * Init action by the data from the MessageBundle. Action name is used as
     * key to find corresponding items in the MessageBundle.
     */
    public void initAction(Action action)
    {
        initAction( action, (String)action.getValue( Action.NAME ) );
    }

    /**
     * Init action by the data from the MessageBundle.
     */
    public void initAction(Action action, String key)
    {
        initActionValue( action, Action.NAME, key + Action.NAME );
        initActionValue( action, Action.SHORT_DESCRIPTION, key + Action.SHORT_DESCRIPTION );
        initActionValue( action, Action.LONG_DESCRIPTION, key + Action.LONG_DESCRIPTION );
        initActionValue( action, Action.ACTION_COMMAND_KEY, key + Action.ACTION_COMMAND_KEY );

        Object mnemonic = getResourceObject( key + Action.MNEMONIC_KEY );
        if( mnemonic != null )
        {
            try
            {
                action.putValue( Action.MNEMONIC_KEY, Integer.parseInt( mnemonic.toString() ) );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Could not parse MNEMONIC_KEY" );
            }
        }

        URL url = getResourceURL( key + Action.SMALL_ICON );
        if( url != null )
            action.putValue( Action.SMALL_ICON, new javax.swing.ImageIcon( url ) );

        Object obj = getResourceObject( key + Action.ACCELERATOR_KEY );
        if( obj instanceof KeyStroke )
            action.putValue( Action.ACCELERATOR_KEY, obj );
    }

    /**
     * Init action value with the specified actionKey by the string data from
     * the message bundle with the corresponding key.
     */
    private void initActionValue(Action action, String actionKey, String resourceKey)
    {
        String value = getResourceString( resourceKey );
        if( value != null )
            action.putValue( actionKey, value );
    }

    /**
     * Creates resourceBundle for the specified name and default locale.
     */
    private static ResourceBundle createResourceBundle(String resourceBundlename, Class<?> l)
    {
        try
        {
            return ResourceBundle.getBundle( resourceBundlename, Locale.getDefault(), l.getClassLoader() );
        }
        catch( MissingResourceException mre )
        {
            log.log(Level.SEVERE,  "ActionInitializer properties not found", mre );
        }
        return null;
    }

    private static ResourceBundle createResourceBundle(Class<?> bundleClass)
    {
        try
        {
            return ResourceBundle.getBundle( bundleClass.getName(), Locale.getDefault(), bundleClass.getClassLoader() );
        }
        catch( MissingResourceException mre )
        {
            log.log(Level.SEVERE,  "ActionInitializer properties not found", mre );
        }
        return null;
    }

    /**
     * Returns object with the specified key from the resource bundle. If the
     * MissingResourceException occurs, catch it and returns null.
     */
    private Object getResourceObject(String nm)
    {
        Object obj = null;

        for( ResourceBundle bundle : resources )
        {
            try
            {
                obj = bundle.getObject( nm );
            }
            catch( MissingResourceException mre )
            {
            }
        }

        if( obj == null )
            log.info( "Action value absents, key=" + nm );

        return obj;
    }

    /**
     * Returns string with the specified key from the resource bundle. If the
     * MissingResourceException occurs, catch it and returns null.
     */
    private String getResourceString(String nm)
    {
        for( ResourceBundle bundle : resources )
        {
            try
            {
                return bundle.getString( nm );
            }
            catch( MissingResourceException mre )
            {
            }
        }
        log.info( "Action value absents, key=" + nm );
        return null;
    }

    private URL getResourceURL(String nm)
    {
        for( ResourceBundle bundle : resources )
        {
            try
            {
                String str = bundle.getString( nm );
                URL url = bundle.getClass().getResource( str );
                if( url == null )
                    url = bundle.getClass().getResource( "resources/" + str );
                if( url == null )
                    log.info( "Action resource absents, key=" + nm + ", resource=" + str + ", bundle=" + bundle.getClass().getName() );
                return url;
            }
            catch( MissingResourceException mre )
            {
            }
        }
        log.info( "Action value absents, key=" + nm );
        return null;
    }
}
