package biouml.plugins.sbml;

import biouml.model.dynamics.EModelBeanInfo;
import biouml.model.dynamics.VariableRoleBeanInfo.SubstanceUnitsEditor;
import one.util.streamex.StreamEx;

public class SbmlEModelBeanInfo extends EModelBeanInfo
{
    public SbmlEModelBeanInfo()
    {
        super(SbmlEModel.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        add("substanceUnits", SubstanceUnitsEditor.class);
        add("timeUnits", SubstanceUnitsEditor.class);
        add("volumeUnits", SubstanceUnitsEditor.class);
        add("areaUnits", SubstanceUnitsEditor.class);
        add("lengthUnits", SubstanceUnitsEditor.class);
        add("extentUnits", SubstanceUnitsEditor.class);
        property( "conversionFactor" )
                .tags( emodel -> StreamEx.of( emodel.getParameters().names() ).append( SbmlEModel.CONVERSION_FACTOR_UNDEFINED ) ).add();
    }
}
