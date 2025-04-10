package biouml.plugins.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Request;
import ru.biosoft.util.TextUtil2;
import biouml.model.CollectionDescription;
import biouml.plugins.server.access.ClientDataCollection;
import biouml.plugins.server.access.ClientModule;

/**
 * Network support for the ClientModuleType
 */
public class ModuleClient
{
    protected MessageBundle messageBundle = new MessageBundle();

    private final Request connection;

    protected Logger log;

    public ModuleClient(Request conn, Logger log)
    {
        this.connection = conn;
        this.log = log;
    }

    public void close()
    {
        if( connection != null )
            connection.close();
    }

    // //////////////////////////////////////
    // Request functions
    //

    /**
     * Opens the connection with the server, sends request, reads the answer,
     * check it, and close the connection.
     *
     * @param command
     *            request command (cod)
     * @param argument
     *            request argument
     *
     * @see Connection
     */
    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
        {
            return connection.request(ModuleProtocol.DATABASE_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }

    public String getModuleTypeVersion(ClientModule module) throws UnsupportedEncodingException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, module.getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
        byte[] result = request(ModuleProtocol.DB_MODELE_TYPE_VERSION, map, true);
        if( result != null )
            return new String(result, "UTF-16BE");
        else
            return messageBundle.getResourceString("DATABASE_TYPE_UNKNOWN_VERSION");
    }

    public Class[] getModuleTypeDiagramTypes(ClientModule module) throws LoggedClassNotFoundException, UnsupportedEncodingException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, module.getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
        String plugins = module.getInfo().getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);

        byte[] result = request(ModuleProtocol.DB_MODELE_TYPE_DIAGRAM_TYPES, map, true);
        if( result != null )
        {
            List<Class> classes = new ArrayList<>();
            String types = new String(result, "UTF-16BE");
            if( "null".equals(types) )
                return new Class[0];
            StringTokenizer tokenizer = new StringTokenizer(types, "\n\r");
            while( tokenizer.hasMoreTokens() )
            {
                String type = tokenizer.nextToken();
                Class<?> clazz = ClassLoading.loadClass( type, plugins );
                classes.add(clazz);
            }
            return classes.toArray(new Class[classes.size()]);
        }
        return new Class[0];
    }

    public String getModuleTypeCategory(ClientModule module, Class<?> aClass) throws UnsupportedEncodingException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, module.getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
        map.put(ModuleProtocol.KEY_CLASS, aClass.getName());
        byte[] result = request(ModuleProtocol.DB_MODELE_TYPE_CATEGORY, map, true);
        if( result != null )
        {
            String category = new String(result, "UTF-16BE");
            if( !"null".equals(category) )
                return category;
        }
        return null;
    }

    public boolean isCategorySupported(ClientModule module) throws UnsupportedEncodingException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, module.getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
        byte[] result = request(ModuleProtocol.DB_MODELE_TYPE_CHECK_SUPPORT, map, true);
        if( result != null )
        {
            String str = new String(result, "UTF-16BE");
            if( "true".equals(str) )
                return true;
        }
        return false;
    }

    public List<CollectionDescription> getExternalCollections(String serverDCName) throws UnsupportedEncodingException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, serverDCName);
        byte[] result = request(ModuleProtocol.DB_MODELE_EXTERNAL_COLLECTIONS, map, true);
        if( result != null )
        {
            String str = new String(result, "UTF-16BE");
            List<CollectionDescription> resultList = new ArrayList<>();
            if( ( str.length() == 0 ) || "null".equals(str) )
                return null;
            StringTokenizer tokenizer = new StringTokenizer(str, "\n\r");
            while( tokenizer.hasMoreTokens() )
            {
                String oneDescription = tokenizer.nextToken();
                String[] values = TextUtil2.split( oneDescription, '@' );
                if( values.length >= 4 )
                {
                    CollectionDescription cd = new CollectionDescription();
                    cd.setModuleName(values[0].trim());
                    cd.setSectionName(values[1].trim());
                    cd.setTypeName(values[2].trim());
                    if( values[3].trim().equals("false") )
                    {
                        cd.setReadOnly(false);
                    }
                    else
                    {
                        cd.setReadOnly(true);
                    }
                    resultList.add(cd);
                }
            }
            return resultList;
        }
        return null;
    }
}
