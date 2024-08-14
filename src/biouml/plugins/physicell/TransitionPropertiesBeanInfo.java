package biouml.plugins.physicell;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TransitionPropertiesBeanInfo extends BeanInfoEx2<TransitionProperties>
{
    public TransitionPropertiesBeanInfo()
    {
        super( TransitionProperties.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        property( "title" ).hidden().implicit().add();
        add( "rate" );
        add( "fixed" );
        addHidden( "to" );
        addHidden( "from" );
    }
}