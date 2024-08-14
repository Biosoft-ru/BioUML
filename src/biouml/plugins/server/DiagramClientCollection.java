package biouml.plugins.server;

import java.util.Properties;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import ru.biosoft.server.ClientConnection;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.server.access.AccessProtocol;
import biouml.plugins.server.access.ClientDataCollection;

/**
 * Special wrapper over ClienDataCollection, which use DiagramClient
 * for optimizing of loading of diagrams
 */
public class DiagramClientCollection extends ClientDataCollection<Diagram>
{
    private final DiagramClient diagramClientConnection;

    private String serverModuleName;

    public DiagramClientCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);

        diagramClientConnection = new DiagramClient(connection.getConnection());
    }

    @Override
    protected void preInit(Properties properties)
    {
        Module module = Module.getModule(getOrigin());
        serverModuleName = module.getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        if( serverModuleName == null )
            throw new DataElementReadException(module, ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
        DataElementPath serverPath = DataElementPath.create(serverModuleName);
        // Remove extra '/' at the end of path if any
        if(serverPath.getName().isEmpty())
            serverPath = serverPath.getParentPath();
        serverModuleName = serverPath.getRelativePath(getCompletePath().getPathDifference(module.getCompletePath())).toString();

        // Init server connection properties
        String serverDCname = properties.getProperty(SERVER_DATA_COLLECTION_NAME);
        if( serverDCname == null )
        {
            serverDCname = serverModuleName;
            properties.put(SERVER_DATA_COLLECTION_NAME, serverDCname);
        }

        String url = properties.getProperty(ClientConnection.URL_PROPERTY);
        if( url == null )
        {
            url = module.getInfo().getProperty(ClientConnection.URL_PROPERTY);
            properties.put(ClientConnection.URL_PROPERTY, url);
        }

        String connectionClassName = properties.getProperty(ClientConnection.CONNECTION_TYPE);
        if( connectionClassName == null )
        {
            connectionClassName = module.getInfo().getProperty(ClientConnection.CONNECTION_TYPE);
            properties.put(ClientConnection.CONNECTION_TYPE, connectionClassName);
        }

        String diagramClass = properties.getProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY);
        if( diagramClass == null )
        {
            properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Diagram.class.getName());
        }

        String id = properties.getProperty(DataCollectionConfigConstants.ID_FORMAT);
        if( id == null )
        {
            properties.put(DataCollectionConfigConstants.ID_FORMAT, "DGR0000");
        }

        String text = properties.getProperty(AccessProtocol.TEXT_TRANSFORMER_NAME);
        if( text == null )
        {
            properties.put(AccessProtocol.TEXT_TRANSFORMER_NAME, BeanInfoEntryTransformer.class.getName());
        }
    }

    @Override
    protected Diagram doGet(String name) throws Exception
    {
        if( canMethodAccess("get") )
        {
            if( contains(name) )
            {
                return diagramClientConnection.getDiagram(this, serverModuleName, name);
            }
        }
        else
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), null, DataCollectionUtils.permissionToString( permission ) );
        }

        return null;
    }

    @Override
    protected void doPut(Diagram obj, boolean isNew) throws Exception
    {
        if( canMethodAccess("put") )
        {
            diagramClientConnection.putDiagram(this, serverModuleName, obj);
            //refresh name list with next getNameList() query
            nameList = null;
        }
        else
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), null, DataCollectionUtils.permissionToString( permission ) );
        }
    }

    @Override
    public void close() throws Exception
    {
        diagramClientConnection.close();
        super.close();
    }

}
