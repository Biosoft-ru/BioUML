package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CellDefinitionVisualizerPropertiesBeanInfo extends BeanInfoEx2<CellDefinitionVisualizerProperties>
{
    public CellDefinitionVisualizerPropertiesBeanInfo()
    {
        super( CellDefinitionVisualizerProperties.class );
    }

    @Override
    public void initProperties()
    {
        property( "cellType" ).tags( bean -> bean.getCellTypes() ).add();
        add("priority");
        property( "type" ).tags( bean -> bean.getTypes() ).add();
        property( "signal" ).tags( bean -> bean.getSignals() ).add();
        property( "color1" ).tags( bean -> bean.getColorSchemes()).add();
        property( "color2" ).tags( bean -> bean.getColorSchemes()).add();
        add( "min" );
        add( "max" );
    }

}