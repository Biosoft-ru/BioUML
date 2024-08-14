package ru.biosoft.access.security;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.exception.LoggedException;

/**
 * Data collection with permission checks
 */
@CodePrivilege(CodePrivilegeType.REPOSITORY)
public class ProtectedDataCollection extends DerivedDataCollection<DataElement, DataElement> implements ProtectedElement
{
    public static final int PROTECTION_NOT_APPLICABLE = -1;
    public static final int PROTECTION_NOT_PROTECTED = 0;
    public static final int PROTECTION_PUBLIC_READ_ONLY = 1;
    public static final int PROTECTION_PUBLIC_READ_PROTECTED_WRITE = 2;
    public static final int PROTECTION_PROTECTED_READ_ONLY = 3;
    public static final int PROTECTION_PROTECTED_READ_WRITE = 4;

    public static final int PERMISSION_READ_ONLY = 16;
    public static final int PERMISSION_READ_WRITE = 0;

    public static final String PROTECTION_STATUS = "protectionStatus";

    protected void check(String method) throws RepositoryAccessDeniedException
    {
        SecurityManager.check(primaryCollection.getCompletePath(), method);
    }

    public ProtectedDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }

    @Override
    public int getSize()
    {
        check("getSize");
        return primaryCollection.getSize();
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        check("getDataElementType");
        return primaryCollection.getDataElementType();
    }

    @Override
    public boolean isMutable()
    {
        Permission permission = SecurityManager.getPermissions(getCompletePath());
        return permission.isAllowed(Permission.WRITE) && primaryCollection.isMutable();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        check("getNameList");
        return primaryCollection.getNameList();
    }

    @Override
    public DataCollectionInfo getInfo()
    {
        if( primaryCollection == null )
        {
            //this code using when primary data collection not initialized yet
            return super.getInfo();
        }
        check("getInfo");
        return new ProtectedDataCollectionInfo(this, primaryCollection.getInfo());
    }

    @Override
    public boolean contains(DataElement element)
    {
        check("contains");
        return primaryCollection.contains(element);
    }

    @Override
    public boolean contains(String name)
    {
        check("contains");
        return primaryCollection.contains(name);
    }

    @Override
    public DataElement get(String name) throws Exception
    {
        Permission permission = SecurityManager.getPermissions(primaryCollection.getCompletePath());
        if(permission.isMethodAllowed("get"))
        {
            return primaryCollection.get(name);
        } else if(permission.isInfoAllowed())
        {
            Permission childPermission = SecurityManager.getPermissions(primaryCollection.getCompletePath().getChildPath(name));
            if(childPermission.isInfoAllowed()) return primaryCollection.get(name);
        }
        throw new RepositoryAccessDeniedException( primaryCollection.getCompletePath(), SecurityManager.getSessionUser(), "Info" );
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        check("getDescriptor");
        return primaryCollection.getDescriptor(name);
    }

    @Override
    public DataElement put(DataElement element) throws DataElementPutException
    {
        check("put");
        return primaryCollection.put(element);
    }

    @Override
    public void remove(String name) throws Exception, UnsupportedOperationException
    {
        check("remove");
        DataElementPath childPath = getCompletePath().getChildPath( name );
        Permission childPerm = SecurityManager.getPermissions( childPath );
        if(!childPerm.isDeleteAllowed())
            throw new RepositoryAccessDeniedException( childPath, SecurityManager.getSessionUser(), "Delete" );
        primaryCollection.remove(name);
    }

    @Override
    public @Nonnull Iterator<DataElement> iterator()
    {
        check("iterator");
        return primaryCollection.iterator();
    }

    @Override
    public void close() throws Exception
    {
        check("close");
        primaryCollection.close();
    }

    @Override
    public void release(String name)
    {
        check("release");
        primaryCollection.release(name);
    }

    @Override
    public DataElement getFromCache(String dataElementName)
    {
        check("getFromCache");
        return primaryCollection.getFromCache(dataElementName);
    }

    @Override
    public void removeFromCache(String dataElementName)
    {
        check("removeFromCache");
        super.removeFromCache( dataElementName );
    }

    @Override
    public void addDataCollectionListener(DataCollectionListener listener)
    {
        check("addDataCollectionListener");
        primaryCollection.addDataCollectionListener(listener);
    }

    @Override
    public void removeDataCollectionListener(DataCollectionListener listener)
    {
        check("removeDataCollectionListener");
        primaryCollection.removeDataCollectionListener(listener);
    }

    @Override
    public void propagateElementWillChange(DataCollection source, DataCollectionEvent primaryEvent)
    {
        check("propagateElementWillChange");
        primaryCollection.propagateElementWillChange(source, primaryEvent);
    }
    
    @Override
    public void propagateElementChanged(DataCollection source, DataCollectionEvent primaryEvent)
    {
        check("propagateElementChanged");
        primaryCollection.propagateElementChanged(source, primaryEvent);
    }

    @Override
    public boolean isPropagationEnabled()
    {
        check("isPropagationEnabled");
        return primaryCollection.isPropagationEnabled();
    }

    @Override
    public void setPropagationEnabled(boolean propagationEnabled)
    {
        check("setPropagationEnabled");
        primaryCollection.setPropagationEnabled(propagationEnabled);
    }

    @Override
    public boolean isNotificationEnabled()
    {
        check("isNotificationEnabled");
        return primaryCollection.isNotificationEnabled();
    }

    @Override
    public void setNotificationEnabled(boolean isEnabled)
    {
        check("setNotificationEnabled");
        primaryCollection.setNotificationEnabled(isEnabled);
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        check("reinitialize");
        primaryCollection.reinitialize();
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        check("isAcceptable");
        return primaryCollection.isAcceptable(clazz);
    }

    @Override
    public DataElement getUnprotectedElement(int access) throws java.lang.SecurityException
    {
        if(access == 0) return primaryCollection;
        Permission permission = SecurityManager.getPermissions(getCompletePath());
        if( !permission.isAllowed(access) )
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        return primaryCollection;
    }
    
    @Override
    public DataCollection<DataElement> getPrimaryCollection()
    {
        throw new java.lang.SecurityException("Direct access to primary collection is disabled");
    }
}
