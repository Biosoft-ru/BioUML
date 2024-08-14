package biouml.plugins.sabiork;

import java.util.logging.Level;
import java.util.List;

import java.util.logging.Logger;
import org.eml.sdbv.sabioclient.Sabiork_PortType;
import org.eml.sdbv.sabioclient.Sabiork_ServiceLocator;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * Abstract class for SBGN service operations
 */
public abstract class ServiceProvider
{
    protected Logger log = Logger.getLogger(ServiceProvider.class.getName());

    private static boolean isServerInit = false;
    private static Sabiork_PortType spt = null;

    /**
     * Get list of element names
     */
    public abstract List<String> getNameList() throws Exception;
    /**
     * Get element by name
     */
    public abstract ru.biosoft.access.core.DataElement getDataElement(DataCollection<?> parent, String name) throws Exception;

    /**
     * Get sabio-rk port (singleton)
     */
    protected Sabiork_PortType getSabiokrPort()
    {
        try
        {
            initServices();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initialize SABIO-RK connection", e);
        }
        return spt;
    }

    /**
     * Initialize sabio-rk service
     */
    protected static void initServices() throws Exception
    {
        if( isServerInit )
            return;

        isServerInit = true;

        Sabiork_ServiceLocator service = new Sabiork_ServiceLocator();
        spt = service.get_8();
    }
}
