package biouml.plugins.physicell;

import java.util.stream.Stream;

import ru.biosoft.util.bean.BeanInfoEx2;

public class RulePropertiesBeanInfo extends BeanInfoEx2<RuleProperties>
{
    public RulePropertiesBeanInfo()
    {
        super( RuleProperties.class );
    }

    @Override
    public void initProperties()
    {
        property( "signal" ).tags( bean -> bean.getAvailableSignals() ).add();
        property( "direction" ).tags( bean -> Stream.of( bean.getAvailableDirections() ) ).add();
        property( "behavior" ).tags( bean -> bean.getAvailableBehaviors() ).add();
        //        add( "baseValue" );
        add( "saturationValue" );
        add( "halfMax" );
        add( "hillPower" );
        add( "applyToDead" );
    }
}