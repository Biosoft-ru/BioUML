package biouml.plugins.state.analyses;

import com.developmentontheedge.beans.BeanInfoEx;

public class StateChangeBeanInfo extends BeanInfoEx
{
    public StateChangeBeanInfo()
    {
        super( StateChange.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( "elementId", DiagramElementIdSelector.class );
        add( "elementProperty", DiagramElementPropertySelector.class );
        add( "propertyValue" );
    }
}
