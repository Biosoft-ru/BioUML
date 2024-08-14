package biouml.plugins.server.access;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.util.ExProperties;
import biouml.model.Module;

/**
 * Wrapper class for supporting of loading connection properties from
 * current module
 */
public class DataClientCollection<T extends DataElement> extends ClientDataCollection<T>
{

    /**
     * Data client collection init all necessary properties
     * and all other rpropertyes obtain from module
     */
    public DataClientCollection ( DataCollection parent, Properties properties ) throws Exception
    {
        super ( parent, properties );
    }
    
    @Override
    protected void preInit ( Properties properties )
    {
        Module module = Module.getModule ( this );
        
        ExProperties.addPlugins( properties, module.getInfo().getProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY ) );
        
        // Init server connection properties
        String serverDCname = properties.getProperty ( SERVER_DATA_COLLECTION_NAME );
        if ( serverDCname == null )
        {
            serverDCname = DataElementPath.create(module.getInfo().getProperty(SERVER_DATA_COLLECTION_NAME))
                    .getRelativePath(getCompletePath().getPathDifference(module.getCompletePath())).toString();
            properties.put ( SERVER_DATA_COLLECTION_NAME, serverDCname );
        }
        
        String url = properties.getProperty ( ClientConnection.URL_PROPERTY );
        if (url == null)
        {
            url = module.getInfo ( ).getProperty ( ClientConnection.URL_PROPERTY );
            properties.put ( ClientConnection.URL_PROPERTY, url );
        }
        
        String connectionClassName = properties.getProperty ( ClientConnection.CONNECTION_TYPE );
        if ( connectionClassName == null )
        {
            connectionClassName = module.getInfo ( ).getProperty ( ClientConnection.CONNECTION_TYPE );
            properties.put ( ClientConnection.CONNECTION_TYPE, connectionClassName );
        }
        
        String text = properties.getProperty ( AccessProtocol.TEXT_TRANSFORMER_NAME );
        if ( text == null )
        {
            properties.put ( AccessProtocol.TEXT_TRANSFORMER_NAME, BeanInfoEntryTransformer.class.getName ( ) );
        }
        
        // Init query system property
        properties.put (  QuerySystem.QUERY_SYSTEM_CLASS, TitleClientQuerySystem.class.getName ( ));
    }

}
