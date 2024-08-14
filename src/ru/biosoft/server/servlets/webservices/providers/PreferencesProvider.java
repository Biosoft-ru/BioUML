package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.access.support.SessionPreferencesManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;

import com.developmentontheedge.beans.Preferences;

/**
 * @author lan
 *
 */
public class PreferencesProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        Preferences preferences = SessionPreferencesManager.getPreferences();
        if( "add".equals(arguments.optAction()) )
        {
            String propName = arguments.getString("name");
            String propValue = arguments.getString("value");
            preferences.addValue(propName, propValue);
            SessionPreferencesManager.saveCurrentSessionPreferences(preferences);
        }
        WebBeanProvider.sendBeanStructure("preferences", preferences, response);
    }
}
