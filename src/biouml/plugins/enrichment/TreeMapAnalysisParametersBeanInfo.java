package biouml.plugins.enrichment;

import biouml.standard.type.Species;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

public class TreeMapAnalysisParametersBeanInfo extends BeanInfoEx2<TreeMapAnalysisParameters>
{
    public TreeMapAnalysisParametersBeanInfo()
    {
        super( TreeMapAnalysisParameters.class );
    }
    @Override
    protected void initProperties() throws Exception
    {
        property( "sourcePath" ).inputElement( TableDataCollection.class ).add();
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );
        add( ColumnNameSelector.registerNumericSelector( "pvalueColumn", beanClass, "sourcePath" ) );
        add( "similarity" );
        add( "displayLimit" );
        add( "representativeOnly" );
        property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$sourcePath$ TreeMap" ).add();
    }
}
