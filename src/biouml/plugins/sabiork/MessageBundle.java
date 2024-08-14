package biouml.plugins.sabiork;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
    
    private final static Object[][] contents =
    {
        { "CN_SABIO_DIAGRAM_DC",             "SABIO-RK pathways"},
        { "CD_SABIO_DIAGRAM_DC",             "SABIO-RK pathway data collection."},

        { "CN_SABIO_DC",             "SABIO-RK data"},
        { "CD_SABIO_DC",             "SABIO-RK data collection."}
    };
}
