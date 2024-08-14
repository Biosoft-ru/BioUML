package biouml.plugins.server.access;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import biouml.model.Diagram;
import biouml.plugins.server.DiagramClientCollection;
import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class PropertiesResolver
{
	private static final Set<String> ALLOWED_PROPERTIES = StreamEx.of( DataCollectionConfigConstants.ID_FORMAT, 
    															  DataCollectionConfigConstants.ASK_USER_FOR_ID,
    															  DataCollectionConfigConstants.CAN_CREATE_ELEMENT_FROM_BEAN,
    															  DataCollectionConfigConstants.PLUGINS_PROPERTY,
    															  DataCollectionConfigConstants.URL_TEMPLATE,
    															  DataCollectionConfigConstants.CAN_OPEN_AS_TABLE,
    															  AccessProtocol.TEXT_TRANSFORMER_NAME,
    															  ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY,
    															  ClientDataCollection.CLASS_ON_SERVER_PROPERTY,
    															  "SequencesCollection", "defaultPosition", "openWithTracks").toSet();
    
    private static final Set<String> DENIED_PROPERTIES = StreamEx.of( DataCollectionConfigConstants.FILE_PATH_PROPERTY,
																 DataCollectionConfigConstants.CONFIG_FILE_PROPERTY,
																 DataCollectionConfigConstants.CONFIG_PATH_PROPERTY,
																 DataCollectionConfigConstants.FILE_PROPERTY).toSet();
    
    public static Properties getClientProperties(DataCollection dc, Properties serverProperties)
    {
        Properties properties = new Properties();

        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, dc.getName());
        
        properties.setProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME, dc.getCompletePath().toString());
        
        String classOnServer = dc.getClass().getName();
        if( dc instanceof ProtectedElement )
        	classOnServer = ((ProtectedElement)dc).getUnprotectedElement(0).getClass().getName();

        properties.setProperty(ClientDataCollection.CLASS_ON_SERVER_PROPERTY, classOnServer);

        if( serverProperties != null )
        {
        	for(Enumeration<Object> e = serverProperties.keys(); e.hasMoreElements();)
        	{
        		String key = (String)e.nextElement();

        		if( ALLOWED_PROPERTIES.contains(key) 
        			|| key.startsWith(AccessProtocol.CLIENT_PREFIX)
        			|| key.startsWith(AccessProtocol.SERVER_PREFIX)
       				|| (SecurityManager.isNode() && !DENIED_PROPERTIES.contains(key)) )
        			properties.put(key, serverProperties.get(key));
        	}
        }
        
        // old properties, before 2020 Q3, for compatibility purposes
        if( Diagram.class.isAssignableFrom(dc.getDataElementType()) )
        {
            properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, DiagramClientCollection.class.getName());
            properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Diagram.class.getName());
        }
        else
        {
            properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, DataClientCollection.class.getName());

            if( serverProperties != null && serverProperties.getProperty(ClientDataCollection.CLIENT_DATA_ELEMENT_CLASS_PROPERTY) != null )
            {
                properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, serverProperties
                        .getProperty(ClientDataCollection.CLIENT_DATA_ELEMENT_CLASS_PROPERTY));
            }
            else if( serverProperties != null && serverProperties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY) != null )
            {
                try
                {
                    properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, ClassLoading.loadClass( serverProperties
                    .getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY) ).getName());
                }
                catch( LoggedClassNotFoundException e )
                {
                }
            }
            if(properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY) == null)
            {
                properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, dc.getDataElementType().getName());
            }
        }

		return properties;
    }
}
