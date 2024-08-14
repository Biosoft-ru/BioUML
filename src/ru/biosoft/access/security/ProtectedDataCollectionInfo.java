package ru.biosoft.access.security;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.RepositoryAccessDeniedException;

/**
 * Data collection info with permission checks. Wraps all methods with permission check.
 */
public class ProtectedDataCollectionInfo extends DataCollectionInfo
{
    DataCollectionInfo dci;

    public ProtectedDataCollectionInfo(DataCollection dc, Properties properties, Logger log)
    {
        super( dc, properties );
        //super(dc, properties, log);
    }

    public ProtectedDataCollectionInfo(DataCollection dc, DataCollectionInfo primary)
    {
        super( dc, null );
        this.dci = primary;
    }

    protected void check(String method) throws RepositoryAccessDeniedException
    {
        SecurityManager.check(dc.getCompletePath(), method);
    }

    @Override
    public String getDisplayName()
    {
        check("getDisplayName");
        return dci.getDisplayName();
    }
    
    @Override
    public void setDisplayName(String value)
    {
        check("setDisplayName");
        dci.setDisplayName(value);
    }

    @Override
    public String getDescription()
    {
        check("getDescription");
        return dci.getDescription();
    }
    
    @Override
    public void setDescription(String description)
    {
        check("setDescription");
        dci.setDescription(description);
    }

    @Override
    public boolean isVisible()
    {
        check("isVisible");
        return dci.isVisible();
    }
    
    @Override
    public void setVisible(boolean f)
    {
        check("setVisible");
        dci.setVisible(f);
    }

    @Override
    public boolean isChildrenLeaf()
    {
        check("isVisibleChildren");
        return dci.isChildrenLeaf();
    }
    
    @Override
    public void setChildrenLeaf(boolean f)
    {
        check("setVisibleChildren");
        dci.setChildrenLeaf(f);
    }

    @Override
    public ImageIcon getNodeImage()
    {
        check("getNodeImage");
        return dci.getNodeImage();
    }
    
    @Override
    public void setNodeImage(ImageIcon img)
    {
        check("setNodeImage");
        dci.setNodeImage(img);
    }

    @Override
    public ImageIcon getChildrenNodeImage()
    {
        check("getChildrenNodeImage");
        return dci.getChildrenNodeImage();
    }
    
    @Override
    public void setChildrenNodeImage(ImageIcon img)
    {
        check("setChildrenNodeImage");
        dci.setChildrenNodeImage(img);
    }

    @Override
    public Comparator getComparator()
    {
        check("getComparator");
        return dci.getComparator();
    }
    
    @Override
    public void setComparator(Comparator value)
    {
        check("setComparator");
        dci.setComparator(value);
    }

    @Override
    public boolean isLateChildrenInitialization()
    {
        check("isLateChildrenInitialization");
        return dci.isLateChildrenInitialization();
    }
    
    @Override
    public void setLateChildrenInitialization(boolean lateChildrenInitialization)
    {
        check("setLateChildrenInitialization");
        dci.setLateChildrenInitialization(lateChildrenInitialization);
    }

    @Override
    public String getError()
    {
        check("getError");
        return dci.getError();
    }
    
    @Override
    public void setError(String error)
    {
        check("setError");
        dci.setError(error);
    }

    @Override
    public void setQuerySystem(QuerySystem system)
    {
        check("setQuerySystem");
        dci.setQuerySystem(system);
    }
    
    @Override
    public QuerySystem getQuerySystem()
    {
        check("getQuerySystem");
        return dci.getQuerySystem();
    }
    
    @Override
    public boolean isQuerySystemInitialized()
    {
        check("isQuerySystemInitialized");
        return dci.isQuerySystemInitialized();
    }

    @Override
    public List<File> getUsedFiles()
    {
        check("getUsedFiles");
        return dci.getUsedFiles();
    }
    
    @Override
    public void addUsedFile(File file)
    {
        //this method should not be protected
        dci.addUsedFile(file);
    }

    @Override
    public Properties getProperties()
    {
        if( dc == null )
        {
            //this code using when primary data collection not initialized yet
            return dci.getProperties();
        }
        check("getProperties");
        return dci.getProperties();
    }
    
    @Override
    public String getProperty(String key)
    {
        check("getProperty");
        return dci.getProperty(key);
    }

    @Override
    public void writeProperty(String key, String value) throws Exception
    {
        check( "writeProperty" );
        dci.writeProperty( key, value );
    }

    @Override
    public <T> Class<? extends T> getPropertyClass(String key, Class<T> superClass) throws DataElementReadException
    {
        check( "getProperty" );
        return dci.getPropertyClass( key, superClass );
    }
}
