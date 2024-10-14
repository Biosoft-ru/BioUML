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
        property( "type" ).tags( bean -> bean.getTypes() ).add();
        property( "signal" ).tags( bean -> bean.getSignals() ).add();
        add( "color1" );
        add( "color2" );//.hidden( "isOneColor" ).add();
        add( "min" );//.hidden( "isOneColor" ).add();
        add( "min" );//.hidden( "isOneColor" ).add();
    }

}