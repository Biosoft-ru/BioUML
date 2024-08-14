package biouml.plugins.simulation.java;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static Object[][] contents = {
            //--- Java preferences ---/
            {"PREFERENCES_ENGINE", "Java"},
            {"PREFERENCES_ENGINE_PN", "Java simulation engine"},
            {"PREFERENCES_ENGINE_PD", "Java simulation engine preferences."},

            //--- Simulation engine errors and warnings ---------------------/
            {"ERROR_CODE_GENERATION", "Can not generate JAVA code for model {0}, error: {1}."},
            {"ERROR_SIMULATION", "Some error have occured during simulation using JAVA, model={0}, error: {1}."},
           
    };

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
