package biouml.plugins.physicell;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class UserParametersBeanInfo extends BeanInfoEx2<UserParameters>
{
    public UserParametersBeanInfo()
    {
        super( UserParameters.class );
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add( "parameters" );
    }
}