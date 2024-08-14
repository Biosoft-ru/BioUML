package biouml.plugins.enrichment;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class SaveClassificationHitsParametersBeanInfo extends BeanInfoEx2<SaveClassificationHitsParameters>
{
    public SaveClassificationHitsParametersBeanInfo()
    {
        super( SaveClassificationHitsParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "sourcePath" ).inputElement( TableDataCollection.class ).add();
        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );
        add( ColumnNameSelector.registerNumericSelector( "pValueColumn", beanClass, "sourcePath" ) );
        add( "pValueThreshold" );
        add( "maxGroupSize" );
        add( "minHits" );

        addExpert( "minCategories" );
        addExpert( "maxCategories" );
        property( "obligateCategories" ).editor( CategoriesMultiSelector.class ).hideChildren().expert().add();
        property( "outputPath" ).outputElement( FolderCollection.class ).auto( "$sourcePath$ hits" ).add();
    }

    public static class CategoriesMultiSelector extends GenericMultiSelectEditor
    {
        @Override
        public Object[] getAvailableValues()
        {
            DataElementPath sourcePath = ( (SaveClassificationHitsParameters)getBean() ).getSourcePath();
            if( sourcePath == null || sourcePath.optDataElement( TableDataCollection.class ) == null )
            {
                return new String[0];
            }
            else
            {
                return sourcePath.getDataElement( TableDataCollection.class ).getNameList().toArray( new String[0] );
            }
        }
    }
}
