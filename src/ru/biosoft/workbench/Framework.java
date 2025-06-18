package ru.biosoft.workbench;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.exception.ExceptionRegistry;

public class Framework
{
    protected Framework() {}

    private static final Map<String, DataCollection<?>> repositoryMap = new HashMap<>();

    public static void initRepository(String path) throws Exception
    {
        if( !repositoryMap.containsKey( path ) )
        {
            DataCollection<?> dc = null;
            try
            {
                dc = CollectionFactory.createRepository( path );
            }
            catch (Exception e)
            {
                File file = new File( path, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
                if( !file.exists() )
                {
                    dc = GenericFileDataCollection.initGenericFileDataCollection( null, new File( path ) );
                }
            }
            if( dc != null )
                repositoryMap.put( path, dc );
        }
    }

    public static void initRepository(String[] paths) throws Exception
    {
        for( String path : paths )
        {
            try
            {
                initRepository(path);
            }
            catch( Exception e )
            {
                System.out.println("Unable to init "+path+": "+ExceptionRegistry.log(e));
            }
        }
    }

    /**
     * Close all root collections
     */
    public static void closeRepositories() throws Exception
    {
        for( DataCollection<?> dc : repositoryMap.values() )
            dc.close();
    }

    public static Set<String> getRepositoryPaths()
    {
        return repositoryMap.keySet();
    }
}
