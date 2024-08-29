package biouml.plugins.physicell;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class UserParameterBeanInfo extends BeanInfoEx2<UserParameter>
{
    public UserParameterBeanInfo()
    {
        super( UserParameter.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add( "name" );
        //        add( "type" );
        add( "value" );
        add("description");
    }
}