package ru.biosoft.bsa.view.resources;

import java.util.ListResourceBundle;
public class Resources
{
    private static ListResourceBundle messageBundle;
    public static String getResourceString( String key )
    {
        if ( messageBundle == null )
            //@todo make many MessageBundles
            messageBundle = new MessageBundle();
        return ((MessageBundle)messageBundle).getResourceString( key );
    }


}
