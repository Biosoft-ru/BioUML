package biouml.plugins.enrichment;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramClassificationParametersBeanInfo extends BeanInfoEx2<DiagramClassificationParameters>
{
    public DiagramClassificationParametersBeanInfo()
    {
        super(DiagramClassificationParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("sourcePath", beanClass, TableDataCollection.class));
        property("type").tags( DiagramClassificationParameters.TYPES ).add();
        add(DataElementPathEditor.registerInput("repositoryHubRoot", beanClass, FolderCollection.class));
        add(DataElementPathEditor.registerInput("referenceCollection", beanClass, TableDataCollection.class, true));
        add("minHits");
        property("onlyOverrepresented").expert().add();
        add("pvalueThreshold");
        property("outputTable").outputElement( TableDataCollection.class ).auto( "$sourcePath$ $repositoryHubRoot/name$" ).add();
        findPropertyDescriptor( "outputTable" ).setValue( DataElementPathEditor.ICON_ID,
                ClassLoading.getResourceLocation( getClass(), "resources/classify.gif" ) );
    }
}
