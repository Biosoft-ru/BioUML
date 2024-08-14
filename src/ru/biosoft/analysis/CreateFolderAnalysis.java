package ru.biosoft.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author anna
 */
@ClassIcon("resources/create-folder.gif")
public class CreateFolderAnalysis extends AnalysisMethodSupport<CreateFolderAnalysis.CreateFolderParameters>
{
    public CreateFolderAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new CreateFolderParameters());
    }

    @Override
    public DataCollection justAnalyzeAndPut() throws Exception
    {
        return DataCollectionUtils.createSubCollection(parameters.getFolderName());
    }

    public static class CreateFolderParameters extends AbstractAnalysisParameters
    {
        private DataElementPath folderName;

        @PropertyName("Folder name")
        @PropertyDescription("Path to the new folder")
        public DataElementPath getFolderName()
        {
            return folderName;
        }

        public void setFolderName(DataElementPath folderName)
        {
            Object oldValue = this.folderName;
            this.folderName = folderName;
            firePropertyChange("folderName", oldValue, folderName);
        }
    }

    public static class CreateFolderParametersBeanInfo extends BeanInfoEx2<CreateFolderParameters>
    {
        public CreateFolderParametersBeanInfo()
        {
            super(CreateFolderParameters.class);
        }
        @Override
        public void initProperties() throws Exception
        {
            property("folderName").outputElement( FolderCollection.class ).add();
        }
    }

    @Override
    public double estimateWeight()
    {
        return 0;
    }
}
