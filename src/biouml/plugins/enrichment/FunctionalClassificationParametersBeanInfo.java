package biouml.plugins.enrichment;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.enrichment.FunctionalClassificationParameters.BioHubSelector;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class FunctionalClassificationParametersBeanInfo extends BeanInfoEx2<FunctionalClassificationParameters>
{
    public FunctionalClassificationParametersBeanInfo()
    {
        super( FunctionalClassificationParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("sourcePath", beanClass, TableDataCollection.class, EnsemblGeneTableType.class));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        property( "bioHub" ).simple().editor( BioHubSelector.class ).add();
        property( "repositoryHubRoot" ).inputElement( FolderCollection.class ).hidden( "isRepositoryHubRootHidden" ).add();
        addHidden( DataElementPathEditor.registerInput( "referenceCollection", beanClass, TableDataCollection.class,
                EnsemblGeneTableType.class, true ), "isRepositoryHubRootHidden" );
        addHidden(new PropertyDescriptorEx("hubShortName", beanClass, "getHubShortName", null));
        add("minHits");
        addExpert( "onlyOverrepresented" );
        add("pvalueThreshold");
        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sourcePath$ $hubShortName$" )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/classify.gif" ) ).add();
    }
}
