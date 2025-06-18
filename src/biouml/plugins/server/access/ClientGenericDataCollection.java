package biouml.plugins.server.access;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.file.AbstractFileTransformer;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementInvalidTypeException;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.generic.DataElementFileTypeDriver;
import ru.biosoft.access.generic.DataElementInfo;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.server.Connection;
import ru.biosoft.util.TempFiles;

public class ClientGenericDataCollection extends ClientDataCollection<DataElement>
        implements FolderCollection, FileBasedCollection<DataElement>
{
    /** Indicates that ClientGenericCollection can get direct access to files without sending them via internet. */
    public static final String FILE_DIRECT_ACCESS = AccessProtocol.SERVER_PREFIX + "file-direct-access";

	public ClientGenericDataCollection(DataCollection<?> parent, Properties properties) throws Exception 
	{
		super(parent, properties);
	}

    @Override
    protected DataElement doGet(String name) throws Exception
    {
        if( name == null )
            return null;

        if( !canMethodAccess("get") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
     
        Properties childDescriptor = connection.getDescriptor( serverDCname, name );
        //System.out.println("\r\n" + name + " DESCRIPTOR:  " + childDescriptor);
        if( childDescriptor == null )
        	return null;

        if( DataElementFileTypeDriver.class.getName()
        		.equals( childDescriptor.getProperty(DataElementInfo.DRIVER_CLASS) ) )
        {
        	if( getInfo().getProperty(FILE_DIRECT_ACCESS) != null )
        		return getFromFile(name, childDescriptor);
        	else
                return (DataElement)connection.getFileElement( serverDCname.getChildPath( name ) );
        }
        
        if( "true".equals(childDescriptor.getProperty(DataCollectionConfigConstants.IS_LEAF) )
                || childDescriptor.getProperty( DataElementFileTypeDriver.TRANSFORMER_CLASS ) != null )
        {
            return (DataElement)connection.getFileElement( serverDCname.getChildPath( name ) );
        }
        else
        {
        	Properties properties = getChildCollectionProperties(name);
        	if( properties != null )
        	{
                DataElement loadedCollection = CollectionFactory.createCollection( this, properties );
        		return loadedCollection;
        	}
        }
        
        return null;
    }	
	
    protected DataElement getFromFile(String name, Properties descriptor) throws Exception
    {
        File file = new File(descriptor.getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY));
        if( ! file.exists() )
		{
        	// @pending get it from server
        	System.out.println("Can not find file: " + file.getCanonicalPath());
        	throw new Exception("Can not find file: " + file.getCanonicalPath());
		}
    	
        String transformerClass = descriptor.getProperty( DataElementFileTypeDriver.TRANSFORMER_CLASS );
        Transformer transformer = (ClassLoading.loadSubClass(transformerClass, Transformer.class)).newInstance();
        if(transformer instanceof AbstractFileTransformer)
        {
            return (DataElement) ( (AbstractFileTransformer)transformer ).load( file, name, this );
        }
        else
        {
        	throw new Exception("Incorrect transformer for data element file. It should be derived form AbstractFileTransformer." +
        					    "\n file=" + file.getCanonicalPath() + ", transformer=" + transformerClass);
        }
    }

    @Override
    protected void doPut(DataElement de, boolean isNew) throws Exception
    {
        if( !canMethodAccess( "put" ) )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        if( connection.canExportToFile( de.getClass() ) )
        {
            connection.putAsFile( serverDCname, de );
            if( nameList != null && !nameList.contains( de.getName() ) )
                nameList.add( de.getName() );
        }
        else
            super.doPut( de, isNew );
    }

    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        if( !canMethodAccess( "put" ) )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        if( name == null || name.isEmpty() )
            throw new MissingParameterException( "Name" );
        if( contains( name ) )
            throw new DataElementExistsException( getCompletePath().getChildPath( name ) );
        if( !name.trim().equals( name ) )
            throw new ParameterNotAcceptableException(
                    new IllegalArgumentException( "Name should not start or end with white-space characters." ), "Name", name );

        String serverClassName = ClientDataCollectionResolver.getServerClassName( this.getClass().getName() );
        if( serverClassName != null )
        {
            Map<String, String> map = new HashMap<>();
            map.put( Connection.KEY_DC, serverDCname.toString() );
            map.put( Connection.KEY_DE, name );
            map.put( AccessProtocol.CLASS_NAME, serverClassName );
            String responce = connection.requestString( AccessProtocol.DB_GENERIC_NEW_FOLDER, map );
            if( "ok".equals( responce ) )
            {
                if( nameList != null && !nameList.contains( name ) )
                    nameList.add( name );
                try
                {
                    DataElement de = get( name );
                    if( de != null )
                    {
                        if( de instanceof DataCollection )
                        {
                            return (DataCollection)de;
                        }
                        else
                            throw new DataElementInvalidTypeException( getCompletePath(), this.getClass().getName() );
                    }
                    else
                        throw new DataElementCreateException( getCompletePath().getChildPath( name ) );
                }
                catch( Exception e )
                {
                    throw ExceptionRegistry.translateException( e );
                }

            }
        }
        else
            throw new DataElementInvalidTypeException( getCompletePath(), this.getClass().getName() );
        return null;
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        if( connection.canExportToFile( clazz ) )
            return true;
        return false;
    }

    @Override
    public boolean isFileAccepted(File file)
    {
        return true;
    }

    @Override
    public File getChildFile(String name)
    {
        try
        {
            return TempFiles.file( "client_generic_" + name );
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }
}
