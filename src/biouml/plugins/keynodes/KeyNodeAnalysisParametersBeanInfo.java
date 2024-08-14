package biouml.plugins.keynodes;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.UserHubCollection;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.type.ProteinTableType;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class KeyNodeAnalysisParametersBeanInfo extends BeanInfoEx2<KeyNodeAnalysisParameters>
{
    public KeyNodeAnalysisParametersBeanInfo()
    {
        this( KeyNodeAnalysisParameters.class );
    }

    public KeyNodeAnalysisParametersBeanInfo(Class<? extends KeyNodeAnalysisParameters> beanClass)
    {
        super( beanClass );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "sourcePath" ).inputElement( TableDataCollection.class )
                .value( DataElementPathEditor.ICON_ID, KeyNodeAnalysisParameters.class.getMethod( "getIcon" ) ).add();

        property( ColumnNameSelector.registerNumericSelector( "weightColumn", beanClass, "sourcePath" ) ).expert().add();

        property( new PropertyDescriptorEx( "isInputSizeLimited", beanClass, "isInputSizeLimited", "setInputSizeLimited" ) ).expert().add();

        property( "inputSizeLimit" ).expert().add();

        property( "direction" ).editor( DirectionEditor.class ).hidden().add();

        add( "maxRadius" );

        add( "scoreCutoff" );

        property( "bioHub" ).simple().editor( BioHubSelector.class ).structureChanging().add();
        property( "customHubCollection" ).inputElement( UserHubCollection.class ).hidden( "isCustomHubCollectionHidden" ).add();

        property( "relationSign" ).hidden( "isRelationSignHidden" ).tags( KeyNodeAnalysisParameters.RELATION_INCREASE,
                KeyNodeAnalysisParameters.RELATION_DECREASE, KeyNodeAnalysisParameters.RELATION_BOTH ).add();

        add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );

        add( "calculatingFDR" );
        addHidden( "FDRcutoff", "isFDRParametersHidden" );
        addHidden( "ZScoreCutoff", "isFDRParametersHidden" );
        addHidden( "seed", "isFDRParametersHidden" );

        addExpert( "penalty" );

        add( "decorators" );

        addExpert( "isoformFactor" );

        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sourcePath$ $direction$ $maxRadius$" )
                .value( DataElementPathEditor.ICON_ID, ClassLoading.getResourceLocation( getClass(), "resources/keynodes.gif" ) ).add();
    }

    protected Class<? extends ReferenceType> getInputTableType()
    {
        return ProteinTableType.class;
    }

}
