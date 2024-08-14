package biouml.plugins.server.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.server.DiagramClientCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Request;
import ru.biosoft.util.FileItem;
import ru.biosoft.util.TempFiles;

/**
 * Provides interaction with AccessService on the server.
 *
 * @see AccessService
 */
public class AccessClient extends AccessProtocol
{
    protected Logger log;

    protected Request connection;


    ////////////////////////////////////////
    // Public section
    //

    /**
     * Constructs the ClientConnection
     * with specified database NAME for transfer
     * and text panel to display prompts.
     *
     * @param host the server address
     */
    public AccessClient(@Nonnull Request conn, Logger log)
    {
        connection = conn;
        this.log = log;
    }

    public Request getConnection()
    {
        return connection;
    }

    public void close()
    {
        connection.close();
    }

    ////////////////////////////////////////
    // Request functions
    //

    /**
     * Opens the connection with the server,
     * sends request, reads the answer,
     * check it, and close the connection.
     *
     * @param command  request command (cod)
     * @param argument request argument
     *
     * @see Connection
     */
    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        return connection.request(AccessProtocol.ACCESS_SERVICE, command, arguments, readAnswer);
    }

    public String requestString(int command, Map<String, String> arguments) throws BiosoftNetworkException
    {
        return connection.requestString(AccessProtocol.ACCESS_SERVICE, command, arguments);
    }

    //////////////////////////////////////////////
    // Common supported client commands
    //

    /**
     * Returns true if the database is mutable
     *
     * @param completeName data collection complete name used by client & server
     */
    public boolean checkMutable(DataElementPath path) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        return Boolean.parseBoolean(requestString(AccessProtocol.DB_CHECK_MUTABLE, map));
    }

    /**
     * Returns  the current database description.
     *
     * @param completeName data collection complete name used by client & server
     */
    public String getDescription(DataElementPath path) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        return requestString(AccessProtocol.DB_DESCRIPTION, map);
    }

    /**
     * Returns the list of identifiers for all entries
     * in current database.
     *
     * @param completeName data collection complete name used by client & server
     */
    public @Nonnull List<String> getNameList(DataElementPath path) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        String result = requestString(AccessProtocol.DB_LIST_ID, map);
        List<String> list = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(result, "\r\n");
        while( tokens.hasMoreTokens() )
            list.add(tokens.nextToken());
        return list;
    }

    /**
     * Returns true if the database contains entry with the specified ID(name)
     *
     * @param completeName data collection complete name used by client & server
     */
    public boolean containsEntry(DataElementPath path, String name) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        map.put(AccessProtocol.KEY_DE, name);
        return Boolean.parseBoolean(requestString(AccessProtocol.DB_CONTAINS_ENTRY, map));
    }

    /**
     * Returns the required entry by its identifcator.
     */
    public String getEntry(DataElementPath path, String name) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        map.put(AccessProtocol.KEY_DE, name);
        String result = requestString(AccessProtocol.DB_ENTRY_BY_ID, map);
        return "null".equals(result) ? null : result;
    }

    public DataElement getFileElement(DataElementPath path) throws LoggedException
    {
        try
        {
            Map<String, String> map = new HashMap<>();
            map.put(Connection.KEY_DC, path.getParentPath().toString());
            map.put(AccessProtocol.KEY_DE, path.getName());
            byte[] result = request(AccessProtocol.DB_GET_AS_FILE, map, true);
            ByteArrayInputStream bais = new ByteArrayInputStream(result);
            DataInputStream dis = new DataInputStream(bais);
            String transformerName = dis.readUTF();
            //System.out.println( "transformerName = \n" + transformerName );
            return loadFromStream(path, transformerName, bais);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    /**
     * Returns the set of required entries by their identifiers.
     */
    public String getEntriesSet(DataElementPath path, List<String> ids) throws BiosoftNetworkException
    {
        if( ids.size() == 0 )
            return null;
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        StringBuffer buffer = new StringBuffer();
        for( String id : ids )
        {
            buffer.append(id).append('\n');
        }
        map.put(AccessProtocol.KEY_IDS, buffer.toString());
        String result = requestString(AccessProtocol.DB_ENTRIES_SET_BY_ID, map);
        return "null".equals(result) ? null : result;
    }

    /**
     * Write the entry.
     */
    public void writeEntry(DataElementPath path, String name, String entry) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        map.put(AccessProtocol.KEY_DE, name);
        map.put(AccessProtocol.KEY_ENTRY, entry);
        request(AccessProtocol.DB_WRITE_ENTRY, map, false);
    }

    /**
     * Write the set of entries.
     */
    public void writeEntriesSet(String completeName, String entries) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, completeName);
        map.put(AccessProtocol.KEY_ENTRY, entries);
        request(AccessProtocol.DB_WRITE_ENTRIES_SET, map, false);
    }

    /**
     * Removes the required entry by its identifier.
     */
    public void removeEntry(DataElementPath path, String name) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        map.put(AccessProtocol.KEY_DE, name);
        request(AccessProtocol.DB_REMOVE_ENTRY, map, false);
    }

    /**
     * Get data elements set.
     * @throws Exception
     */
    public List<DataElement> getSet(DataElementPath path, Collection<String> names, DataCollection base) throws Exception
    {
        if( names.size() == 0 )
            return new ArrayList<>();
        List<DataElement> kernels = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        StringBuilder des = new StringBuilder();
        for( String name : names )
        {
            DataElementPath kernelPath = DataElementPath.create(name);
            DataCollection<?> parentCollection = base.getCompletePath().getRelativePath(kernelPath.getPathDifference(path)).optParentCollection();
            if(parentCollection == null)
                continue;
            DataElement kernel = parentCollection.getFromCache(kernelPath.getName());
            if(kernel != null)
                kernels.add(kernel);
            else
                des.append(name).append("\n");
        }
        if(des.length() == 0)
            return kernels;
        map.put(AccessProtocol.KEY_DEs, des.toString());
        byte[] result = request(AccessProtocol.DB_GET_DE_SET, map, true);
        if( result == null )
            return new ArrayList<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(result);
        ObjectInputStream ois = new ObjectInputStream(bais);
        List<AccessService.Entry> answer = (List<AccessService.Entry>)ois.readObject();
        if( answer != null )
        {
            for( AccessService.Entry entry : answer )
            {
                DataCollection dc = base;
                if( entry.dcName.length() > 0 )
                {
                    if( entry.dcName.startsWith("databases/") )
                    {
                        dc = CollectionFactory.getDataCollection(entry.dcName);
                    }
                    else
                    {
                        dc = dc.getCompletePath().getChildPath(DataElementPath.create(entry.dcName).getPathComponents()).optDataCollection();
                    }
                    if( dc == null )
                        continue;
                }
                if( dc instanceof ClientDataCollection && ! ( dc instanceof DiagramClientCollection ) )
                {
                    DataElement de = convertToDataElement(dc, entry.deName, entry.entry);
                    if( de != null )
                    {
                        kernels.add(de);
                        ( (ClientDataCollection)dc ).cachePut(de);
                    }
                }
            }
        }
        return kernels;
    }

    /**
     * Get client properties for child data collection
     * @throws IOException
     */
    public Properties getClientCollectionProperties(DataElementPath path, String name) throws BiosoftNetworkException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        if(name != null)
            map.put(AccessProtocol.KEY_DE, name);
        String result = requestString(AccessProtocol.DB_GET_INFO, map);
        if( "null".equals(result) )
            return null;
        StringReader sr = new StringReader(result);
        Properties properties = new Properties();
        properties.load(sr);
        return properties;
    }

    /**
     * Gets data element descriptor.
     * @throws IOException
     */
    public Properties getDescriptor(DataElementPath path, String name) throws BiosoftNetworkException, IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        map.put(AccessProtocol.KEY_DE, name);

        String result = requestString(AccessProtocol.DB_GET_DESCRIPTOR, map);
        if( "null".equals(result) )
            return null;

        StringReader sr = new StringReader(result);
        Properties properties = new Properties();
        properties.load(sr);
        
        return properties;
    }
    
    public void createNewFolder(DataElementPath path, Class<? extends FolderCollection> clazz)
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.getParentPath().toString());
        map.put(Connection.KEY_DE, path.getName());
        map.put(CLASS_NAME, clazz.getName());
        request(AccessProtocol.DB_GENERIC_NEW_FOLDER, map, false);
    }

    public void putAsFile(DataCollection parent, DataElement de) throws LoggedException
    {
        putAsFile( parent.getCompletePath(), de );
    }

    public void putAsFile(DataElementPath parentPath, DataElement de) throws LoggedException
    {
        try
        {
            FileItem file = saveAsFile(de);
            if(file.length() > 10*1024*1024)
                throw new Exception("Element is too big to transfer");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ApplicationUtils.copyStream(baos, new FileInputStream(file));
            String base64content = Base64.encodeBase64String( baos.toByteArray() );
            String clazz = de.getClass().getName();
            String transformer = file.getOriginalName();
            Map<String, String> map = new HashMap<>();
            map.put( Connection.KEY_DC, parentPath.toString() );
            map.put(Connection.KEY_DE, de.getName());
            map.put(CLASS_NAME, clazz);
            map.put(KEY_TRANSFORMER_NAME, transformer);
            map.put(KEY_BASE64_CONTENT, base64content);
            request(AccessProtocol.DB_PUT_AS_FILE, map, false);
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }

    }

    public void putAsProperties(DataCollection dc) throws BiosoftNetworkException
    {
        Properties properties = (Properties)dc.getInfo().getProperties().clone();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, dc.getName());
        properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, dc.getClass().getName());
        StringWriter writer = new StringWriter();
        try
        {
            properties.store(writer, "");
        }
        catch( IOException e )
        {
            throw new InternalException(e);
        }
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, dc.getCompletePath().getParentPath().toString());
        map.put(Connection.KEY_DE, dc.getName());
        map.put(AccessProtocol.KEY_PROPERTIES, writer.toString());
        request(AccessProtocol.DB_PUT_AS_PROPERTIES, map, false);
    }

    public void updateProperties(DataCollection dc)
    {
        StringWriter writer = new StringWriter();
        try
        {
            dc.getInfo().getProperties().store(writer, "");
        }
        catch( IOException e )
        {
            throw new InternalException(e);
        }
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, dc.getCompletePath().getParentPath().toString());
        map.put(Connection.KEY_DE, dc.getName());
        map.put(AccessProtocol.KEY_PROPERTIES, writer.toString());
        request(AccessProtocol.DB_UPDATE_PROPERTIES, map, false);
    }

    public Permission login(DataElementPath path, String username, String password) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        if(path != null)
            map.put(Connection.KEY_DC, path.toString());
        map.put(SecurityManager.USERNAME, username);
        map.put(SecurityManager.PASSWORD, password);
        String result = requestString(AccessProtocol.DB_LOGIN, map);
        if( "null".equals(result) )
            return new Permission(0, username, password, System.currentTimeMillis() + SecurityManager.timeLimit);
        Permission permission = new Permission(result);
        getConnection().setSessionId(permission.getSessionId());
        return permission;
    }

    /**
     * Returns permissions for the specified data collection.
     * 
     * @pending - null, then principal permissions will be returned.  
     */
    public Permission getPermissions(DataElementPath path, Permission principal) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
    	
        String result = requestString(AccessProtocol.DB_GET_PERMISSIONS, map);

        if( "null".equals(result) )
        	return principal;
//        	return new Permission(0, principal.getUserName(), principal.getSessionId(), principal.getExpirationTime());

        Permission permission = new Permission(result);
        return permission;
    }

    /**
     * Returns true if the data collection is protected
     *
     * @return
     * 0 - not protected
     * 1 - not protected, not mutable
     * 2 - public read, protected write
     * 3 - protected, read only
     * 4 - protected, read/write
     */
    public int checkProtected(DataElementPath path) throws BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.toString());
        byte[] bytes = request(AccessProtocol.DB_CHECK_PROTECTED, map, true);
        if( bytes != null && bytes.length > 0 )
        {
            return bytes[0];
        }
        return ProtectedDataCollection.PROTECTION_PROTECTED_READ_ONLY;//the most strong type by default
    }

    /**
     * Get name list with extended properties
     */
    public @Nonnull Map<String, Properties> getFlaggedNameList(String completeName) throws BiosoftNetworkException, JSONException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, completeName);
        map.put(KEY_EXTENDED, "true");
        String resultStr = requestString(AccessProtocol.DB_FLAGGED_LIST, map);
        Map<String, Properties> result = new HashMap<>();
        JSONObject resultObj = new JSONObject(resultStr);

        JSONArray names = resultObj.getJSONArray(PROPERTY_NAMES);
        for( int i = 0; i < names.length(); i++ )
        {
            JSONObject nameObj = names.getJSONObject(i);
            String name = nameObj.getString(PROPERTY_NAME);
            Properties props = new Properties();
            Iterator<?> iter = nameObj.keys();
            while( iter.hasNext() )
            {
                Object key = iter.next();
                if( !key.equals(PROPERTY_NAME) )
                {
                    props.put( key, nameObj.get( key.toString() ).toString() );
                }
            }
            result.put(name, props);
        }
        return result;
    }

    public File importDataCollection(String oldDCName, String newName) throws IOException, BiosoftNetworkException
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, oldDCName);
        if( newName != null )
            map.put(AccessProtocol.KEY_DE, newName);
        File importFile = TempFiles.file("importCollection.tmp");
        try (FileOutputStream fos = new FileOutputStream( importFile ))
        {
            connection.request( AccessProtocol.ACCESS_SERVICE, AccessProtocol.DB_EXPORT_DC, map, true, fos );
        }
        return importFile;
    }

    public boolean isAcceptable(DataElementPath path, Class<? extends DataElement> clazz)
    {
        Map<String, String> map = new HashMap<>();
        map.put(Connection.KEY_DC, path.getParentPath().toString());
        map.put(AccessProtocol.KEY_FROM, "0");
        map.put(AccessProtocol.KEY_TO, "0");
        map.put(AccessProtocol.ELEMENT_CLASS_NAME, clazz.getName());
        String resultStr = requestString(AccessProtocol.DB_FLAGGED_LIST, map);
        JSONObject resultObj;
        try
        {
            resultObj = new JSONObject(resultStr);
        }
        catch( JSONException e )
        {
            throw new BiosoftNetworkException(e, connection.getConnectionInfo());
        }
        return resultObj.optBoolean(AccessProtocol.PROPERTY_ENABLED);
    }
}
