package ru.biosoft.gui.setupwizard;

import javax.swing.JPanel;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.util.NetworkConfigurator;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.swing.PropertyInspector;

/**
 * Wizard page for proxy settings
 * 
 * @author tolstyh
 */
public class ProxySettingsWizardPage implements WizardPage
{
    protected static final Logger log = Logger.getLogger(ProxySettingsWizardPage.class.getName());

    public static final String NAME = "Proxy settings";

    protected Preferences proxyPreferences;

    @Override
    public JPanel getPanel()
    {
        PropertyInspector pane = new PropertyInspector();
        try
        {
            proxyPreferences = new Preferences();
            proxyPreferences.setSaveOrder(true);

            proxyPreferences.add(new DynamicProperty(NetworkConfigurator.PROXY_PREFERENCES_PROXY_USE, "Use proxy", "Use proxy",
                    Boolean.class, NetworkConfigurator.isProxyUsed()));

            proxyPreferences.add(new DynamicProperty(NetworkConfigurator.PROXY_PREFERENCES_PROXY_HOST,
                    NetworkConfigurator.PROXY_PREFERENCES_PROXY_HOST, "Proxy host", String.class, NetworkConfigurator.getHost()));

            proxyPreferences.add(new DynamicProperty(NetworkConfigurator.PROXY_PREFERENCES_PROXY_PORT,
                    NetworkConfigurator.PROXY_PREFERENCES_PROXY_PORT, "Proxy port", Integer.class, NetworkConfigurator.getPort()));


            proxyPreferences.add(new DynamicProperty(NetworkConfigurator.PROXY_PREFERENCES_PROXY_USERNAME,
                    NetworkConfigurator.PROXY_PREFERENCES_PROXY_USERNAME, "Proxy username", String.class, NetworkConfigurator
                            .getUsername()));

            proxyPreferences.add(new DynamicProperty(NetworkConfigurator.PROXY_PREFERENCES_PROXY_PASSWORD,
                    NetworkConfigurator.PROXY_PREFERENCES_PROXY_PASSWORD, "Proxy password", String.class, NetworkConfigurator
                            .getPassword()));

            pane.explore(proxyPreferences);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can find proxy preferences", e);
        }

        return pane;
    }

    @Override
    public void saveSettings()
    {
        //nothing to do, properties was saved by fireClosePage() method
    }
    
    @Override
    public void fireOpenPage()
    {
        //nothing to do
    }
    
    @Override
    public void fireClosePage()
    {
        try
        {
            NetworkConfigurator.changeProxySettings(proxyPreferences);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not save proxy preferences", e);
        }
    }
}
