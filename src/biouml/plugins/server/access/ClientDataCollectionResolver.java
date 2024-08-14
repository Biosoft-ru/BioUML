package biouml.plugins.server.access;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import biouml.plugins.server.DiagramClientCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.generic.GenericDataCollection;

/**
 * Provides mapping between server data collection and its client counterpart.
 * 
 * @todo - extension point   
 */
public class ClientDataCollectionResolver 
{
	protected static Map<String, String> mapping = new HashMap<>();
	static{
	    mapping.put(GenericDataCollection.class.getName(),		ClientGenericDataCollection.class.getName()	);
	    mapping.put("biouml.model.Module",						ClientModule.class.getName()				);
	}
	
    protected static Map<String, String> revertedMapping = new HashMap<>();
    static
    {
        revertedMapping.put( ClientGenericDataCollection.class.getName(), GenericDataCollection.class.getName() );
        revertedMapping.put( ClientModule.class.getName(), "biouml.model.Module" );
    }

    protected static Map<String, String> mappingByDe = new HashMap<>();
    static {
        mappingByDe.put( "biouml.model.Diagram", DiagramClientCollection.class.getName() );
	}

	/**
	 * Returns counterpart on client side for server ru.biosoft.access.core.DataCollection.
	 * @see biouml.plugins.server.access.PropertiesMapper. 
	 * 
	 * @param properties - properties of server data collection.
	 * Class name of server data collection is specified in properties with key {@link ClientDataCollection.CLASS_ON_SERVER_PROPERTY}.
	 * 
	 * @return class name for client data collection.
	 */
	public static String getCounterpart(Properties properties)
	{
		String serverClassName = (String)properties.get(ClientDataCollection.CLASS_ON_SERVER_PROPERTY);
		
		if( serverClassName != null )
		{
			String clientClassName = mapping.get(serverClassName);
			if( clientClassName != null)
				return clientClassName;

            if( serverClassName.equals( TransformedDataCollection.class.getName() ) )
            {
                String deClassName = properties.getProperty( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY );
                if( deClassName != null )
                {
                    clientClassName = mappingByDe.get( deClassName );
                    if( clientClassName != null )
                        return clientClassName;
                }
            }
		}
		
		return ClientDataCollection.class.getName();
	}

    /**
     * Returns counterpart on server side for client ru.biosoft.access.core.DataCollection.
     * 
     * @param clientClassName - Class name of client data collection.
     * 
     * @return class name for server data collection.
     */

    public static String getServerClassName(String clientClassName)
    {
        return revertedMapping.get( clientClassName );
    }
}
