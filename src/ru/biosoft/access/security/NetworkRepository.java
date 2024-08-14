package ru.biosoft.access.security;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.exception.ProductNotAvailableException;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * NetworkRepository creates hierarchical shared ru.biosoft.access.core.DataCollection. The information about used
 * in a tree nodes is extracted from files, which are organized in a tree.
 * NetworkRepository supports all functionality of {@link LocalRepository} and also allows to filter
 * collections by user groups.
 */
public class NetworkRepository extends FilteredDataCollection<DataCollection<?>> implements Repository
{
    public static final String PRODUCT_PROPERTY_PREFIX = "product.";
    private Map<String, Pattern> products;
    
    public NetworkRepository(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        initProducts();
    }

    private void initProducts()
    {
        products = new LinkedHashMap<>();
        for(Entry<Object, Object> entry : getInfo().getProperties().entrySet())
        {
            String key = entry.getKey().toString();
            if(key.startsWith(PRODUCT_PROPERTY_PREFIX))
            {
                String value = entry.getValue().toString();
                String productName = key.substring(PRODUCT_PROPERTY_PREFIX.length());
                try
                {
                    products.put(productName, Pattern.compile(value));
                }
                catch( PatternSyntaxException e )
                {
                    log.warning(getCompletePath()+": pattern syntax error for product "+productName+": "+e.getMessage());
                }
            }
        }
    }

    @Override
    public DataCollection createDataCollection(String name, Properties properties, String subDir, File[] files, CreateDataCollectionController controller) throws Exception
    {
        return ( (LocalRepository)primaryCollection ).createDataCollection(name, properties, subDir, files, controller);
    }

    @Override
    public DataCollection<?> get(String name) throws Exception
    {
        Permission permission = SecurityManager.getPermissions(DataElementPath.create(this, name));
        if( !permission.isInfoAllowed() )
        {
            StreamEx.ofKeys(products, val -> val.matcher(name).matches()).findAny().ifPresent(key -> {
                throw new ProductNotAvailableException(key);
            });
            return null;
        }
        return primaryCollection.get(name);
    }

    @Override
    public boolean contains(String name)
    {
        DataElement de = null;
        try
        {
            de = get(name);
        }
        catch( Exception e )
        {
        }
        return de != null;
    }
    
    @Override
    public void remove(String name) throws Exception
    {
        Permission perm = SecurityManager.getPermissions( getCompletePath() );
        if( !perm.isWriteAllowed() && !perm.isDeleteAllowed() )
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(), "Write or Delete" );
        DataElementPath childPath = getCompletePath().getChildPath( name );
        Permission childPerm = SecurityManager.getPermissions( childPath );
        if(!childPerm.isDeleteAllowed())
            throw new RepositoryAccessDeniedException( childPath, SecurityManager.getSessionUser(), "Delete" );
        super.remove( name );
    }

    @Override
    protected List<String> getFilteredNames()
    {
        List<String> filteredNameList = new ArrayList<>();
        Filter filter = getFilter();
        for(DataCollection<?> dc : primaryCollection)
        {
            if( filter.isAcceptable(dc) )
            {
                filteredNameList.add(dc.getName());
            }
        }
        return filteredNameList;
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        DataElement element = null;
        try
        {
            element = get(name);
        }
        catch( Exception e )
        {
        }
        if(element == null) return null;
        return new DataElementDescriptor(DataCollectionUtils.getPrimaryElementType(element), false);
    }

    @Override
    public void updateRepository()
    {
        ( (LocalRepository)primaryCollection ).updateRepository();
    }
    
    @Override
    public void release(String name)
    {
    	primaryCollection.release(name);
    }

    @Override
    protected void initNames(FunctionJobControl jobControl)
    {
    }

    @Override
    public boolean isMutable()
    {
        Permission permission = SecurityManager.getPermissions(getCompletePath());
        return permission.isAllowed(Permission.WRITE) && primaryCollection.isMutable();
    }
}
