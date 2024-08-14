package biouml.plugins.physicell;

import java.beans.IntrospectionException;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SubstratePropertiesBeanInfo extends BeanInfoEx2<SubstrateProperties>
{
    public SubstratePropertiesBeanInfo()
    {
        super( SubstrateProperties.class );
    }

    @Override
    public void initProperties() throws SecurityException, NoSuchMethodException, IntrospectionException
    {
        addReadOnly( "name", "isCompleted" );
        add( "initialCondition" );
        add( "decayRate" );
        add( "diffusionCoefficient" );
        add( "dirichletCondition" );
        add( "dirichletValue" );
    }
}