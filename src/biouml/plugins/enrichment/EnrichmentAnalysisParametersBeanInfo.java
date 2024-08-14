package biouml.plugins.enrichment;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.enrichment.FunctionalClassificationParameters.BioHubSelector;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class EnrichmentAnalysisParametersBeanInfo extends BeanInfoEx2<EnrichmentAnalysisParameters>
{
    public EnrichmentAnalysisParametersBeanInfo()
    {
        super( EnrichmentAnalysisParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput("sourcePath", beanClass, TableDataCollection.class, EnsemblGeneTableType.class));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
        add(ColumnNameSelector.registerNumericSelector("columnName", beanClass, "sourcePath", false));
        property( "bioHub" ).simple().editor( BioHubSelector.class ).add();
        property( "repositoryHubRoot" ).inputElement( FolderCollection.class ).hidden( "isRepositoryHubRootHidden" ).add();
        addHidden(DataElementPathEditor.registerInput("referenceCollection", beanClass, TableDataCollection.class, EnsemblGeneTableType.class, true), "isRepositoryHubRootHidden");
        addHidden(new PropertyDescriptorEx("hubShortName", beanClass, "getHubShortName", null));
        add("minHits");
        property( "onlyOverrepresented" ).expert().add();
        property( "permutationsCount" ).expert().add();
        add("pvalueThreshold");
        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sourcePath$ GSEA $hubShortName$" )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/classify.gif" ) ).add();
    }
}
