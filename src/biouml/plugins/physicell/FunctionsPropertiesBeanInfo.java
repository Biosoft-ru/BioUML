package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

import java.beans.IntrospectionException;

import one.util.streamex.StreamEx;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.physicell.core.CellFunctions.Function;
import ru.biosoft.physicell.core.standard.FunctionRegistry;

public class FunctionsPropertiesBeanInfo extends BeanInfoEx2<FunctionsProperties>
{
    public FunctionsPropertiesBeanInfo()
    {
        super( FunctionsProperties.class );
    }

    @Override
    public void initProperties() throws IntrospectionException, NoSuchMethodException
    {
        property( "phenotypeUpdate" ).tags( toNames( FunctionRegistry.getUpdatePhenotypeFunctions() ) ).add();
        property( "phenotypeUpdateCustom" ).hidden( "isDefaultPhenotype" ).inputElement( ScriptDataElement.class ).add();
        property( "volumeUpdate" ).tags( toNames( FunctionRegistry.getVolumeFunctions() ) ).add();
        property( "volumeUpdateCustom" ).hidden( "isDefaultVolume" ).inputElement( ScriptDataElement.class ).add();
        property( "customRule" ).tags( toNames( FunctionRegistry.getCustomRules() ) ).add();
        property( "customRuleCustom" ).hidden( "isDefaultRule" ).inputElement( ScriptDataElement.class ).add();
        property( "velocityUpdate" ).tags( toNames( FunctionRegistry.getVelocityFunctions() ) ).add();
        property( "velocityUpdateCustom" ).hidden( "isDefaultVelocity" ).inputElement( ScriptDataElement.class ).add();
        property( "migrationUpdate" ).tags( toNames( FunctionRegistry.getUpdateMigrationFunctions() ) ).add();
        property( "migrationUpdateCustom" ).hidden( "isDefaultMigration" ).inputElement( ScriptDataElement.class ).add();
        property( "membraneInteraction" ).tags( toNames( FunctionRegistry.getMembraneInteractionFunctions() ) ).add();
        property( "membraneInteractionCustom" ).hidden( "isDefaultMBInteraction" ).inputElement( ScriptDataElement.class ).add();
        property( "membraneDistance" ).tags( toNames( FunctionRegistry.getDistanceCalculatorFunctions() ) ).add();
        property( "membraneDistanceCustom" ).hidden( "isDefaultMBDistance" ).inputElement( ScriptDataElement.class ).add();
        property( "orientation" ).tags( toNames( FunctionRegistry.getOrientationFunctions() ) ).add();
        property( "orientationCustom" ).hidden( "isDefaultOrientation" ).inputElement( ScriptDataElement.class ).add();
        property( "contact" ).tags( toNames( FunctionRegistry.getContactFunctions() ) ).add();
        property( "contactCustom" ).hidden( "isDefaultContact" ).inputElement( ScriptDataElement.class ).add();
        property( "instantiate" ).tags( toNames( FunctionRegistry.getIntsnatiateFunctions() ) ).add();
        property( "instantiateCustom" ).hidden( "isDefaultInstantiate" ).inputElement( ScriptDataElement.class ).add();
        property( "division" ).tags( toNames( FunctionRegistry.getDivisionFunctions() ) ).add();
        property( "divisionCustom" ).hidden( "isDefaultDivision" ).inputElement( ScriptDataElement.class ).add();
    }

    private String[] toNames(Function[] functions)
    {
        return StreamEx.of( functions ).map( f -> f.getName() ).prepend( PhysicellConstants.NOT_SELECTED )
                .append( PhysicellConstants.CUSTOM ).toArray( String[]::new );
    }
}