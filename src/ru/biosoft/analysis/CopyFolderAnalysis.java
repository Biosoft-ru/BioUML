package ru.biosoft.analysis;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 *
 */
public class CopyFolderAnalysis extends AnalysisMethodSupport<CopyFolderAnalysis.CopyFolderAnalysisParameters>
{
    public CopyFolderAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new CopyFolderAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Creating list of elements to copy...");
        Map<ru.biosoft.access.core.DataElementPath, DataElementPath> items = new LinkedHashMap<>();
        DataCollection<DataElement> dataCollection = parameters.getFromFolder().getDataCollection();
        if( DataCollectionUtils.checkPrimaryElementType( dataCollection, CloneableDataElement.class ) )
            items.put( parameters.getFromFolder(), parameters.getToFolder() );
        populateList(dataCollection, parameters.getToFolder(), items);
        jobControl.forCollection(items.entrySet(), element -> {
            log.info(element.getKey()+"\n-> "+element.getValue());
            try
            {
                copyElement(element.getKey(), element.getValue());
            }
            catch( Exception e )
            {
                log.warning(e.getMessage());
            }
            return true;
        });
        return parameters.writeAnalysisInfo ? parameters.getToFolder().optDataCollection() : null;
    }

    protected void copyElement(DataElementPath from, DataElementPath to) throws Exception
    {
        DataElement element = DataCollectionUtils.fetchPrimaryElement(from.optDataElement(), Permission.READ);
        if(element == null)
        {
            throw new Exception("Unable to read source element; skipping");
        }
        try
        {
            DataCollectionUtils.createFoldersForPath(to);
        }
        catch( Exception e )
        {
            throw new Exception("Unable to create folder "+to.getParentPath()+": "+e);
        }
        DataCollection<?> parent = to.optParentCollection();
        if(parent == null)
        {
            throw new Exception("Unable to read folder "+to.getParentPath());
        }
        Method cloneMethod;
        try
        {
            cloneMethod = element.getClass().getMethod("clone", DataCollection.class, String.class);
        }
        catch( Exception e1 )
        {
            throw new Exception("Unable to copy: element doesn't support copying");
        }
        DataElement result;
        try
        {
            result = (DataElement)cloneMethod.invoke(element, parent, to.getName());
        }
        catch( Exception e1 )
        {
            throw new Exception("Copying error: " + e1.getMessage());
        }
        try
        {
            if( result != null )
                CollectionFactoryUtils.save( result );
        }
        catch( Exception e1 )
        {
            throw new Exception("Storing error: " + e1.getMessage());
        }
    }

    private void populateList(DataCollection<? extends DataElement> dataCollection, DataElementPath targetPath, Map<ru.biosoft.access.core.DataElementPath, DataElementPath> items)
    {
        for( ru.biosoft.access.core.DataElement de : dataCollection )
        {
            if( DataCollectionUtils.checkPrimaryElementType( de, FolderCollection.class ) )
            {
                if( DataCollectionUtils.checkPrimaryElementType( de, CloneableDataElement.class ) )
                    items.put( de.getCompletePath(), targetPath.getChildPath( de.getName() ) );
                populateList( (DataCollection<? extends DataElement>)de, targetPath.getChildPath( de.getName() ), items );
            }
            else
            {
                items.put( de.getCompletePath(), targetPath.getChildPath( de.getName() ) );
            }
        }
    }

    @Override
    public Object[] getAnalysisResults()
    {
        if( parameters.writeAnalysisInfo )
            return super.getAnalysisResults();
        return null;
    }

    public static class CopyFolderAnalysisParameters extends AbstractAnalysisParameters
    {
        private static final long serialVersionUID = 1L;
        private DataElementPath fromFolder, toFolder;
        private boolean writeAnalysisInfo = true;

        @PropertyName("Source folder")
        @PropertyDescription("Folder containing data you want to copy")
        public DataElementPath getFromFolder()
        {
            return fromFolder;
        }

        public void setFromFolder(DataElementPath fromFolder)
        {
            Object oldValue = this.fromFolder;
            this.fromFolder = fromFolder;
            firePropertyChange("fromFolder", oldValue, fromFolder);
        }

        @PropertyName("Target folder")
        @PropertyDescription("Target path where to put the result")
        public DataElementPath getToFolder()
        {
            return toFolder;
        }

        public void setToFolder(DataElementPath toFolder)
        {
            Object oldValue = this.toFolder;
            this.toFolder = toFolder;
            firePropertyChange("toFolder", oldValue, toFolder);
        }

        public void setWriteAnalysisInfo(boolean writeAnalysisInfo)
        {
            this.writeAnalysisInfo = writeAnalysisInfo;
        }
    }

    public static class CopyFolderAnalysisParametersBeanInfo extends BeanInfoEx2<CopyFolderAnalysisParameters>
    {
        public CopyFolderAnalysisParametersBeanInfo()
        {
            super(CopyFolderAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "fromFolder" ).inputElement( FolderCollection.class ).add();
            property( "toFolder" ).outputElement( ru.biosoft.access.core.DataCollection.class ).add();
        }
    }
}
