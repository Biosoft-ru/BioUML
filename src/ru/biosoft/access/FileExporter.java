package ru.biosoft.access;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Export {@link FileDataElement} like a simple binary file
 */
public class FileExporter implements DataElementExporter
{
    @Override
    public int accept(DataElement de)
    {
        try
        {
            //TODO: try to avoid de type check
            if( ! ( de instanceof ZipHtmlDataCollection ) && getElementFile( de ) != null )
                return ACCEPT_MEDIUM_PRIORITY;
        }
        catch( Exception e )
        {
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport(de, file, null);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        try
        {
            File sourceFile = getElementFile(de);
            ApplicationUtils.linkOrCopyFile(file, sourceFile, jobControl);
        }
        catch( Exception e )
        {
            if( file != null )
                file.delete();
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
            }
            else
                throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }
    
    protected File getElementFile(DataElement de) throws Exception
    {
        if( de instanceof FileDataElement )
            return ( (FileDataElement)de ).getFile();
        DataCollection parent = DataCollectionUtils.fetchPrimaryCollection(de.getOrigin(), Permission.READ);
        if( parent != null )
        {
            DataCollection primaryDC = parent;
            if( parent instanceof GenericDataCollection )
            {
                primaryDC = ( (GenericDataCollection)parent ).getTypeSpecificCollection(de.getClass());
            }
            else if( parent instanceof TransformedDataCollection )
            {
                primaryDC = ( (TransformedDataCollection)parent ).getPrimaryCollection();
            }
            
            if( primaryDC instanceof FileBasedCollection )
            {
                File file = ((FileBasedCollection)primaryDC).getChildFile(de.getName());
                if(file.exists())
                    return file;
            }
        }
        return null;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( FileDataElement.class, DataElement.class );
    }
}
