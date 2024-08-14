package biouml.plugins.physicell.ode;

import ru.biosoft.util.bean.BeanInfoEx2;

public class IntracellularPropertiesBeanInfo extends BeanInfoEx2<IntracellularProperties>
{
    public IntracellularPropertiesBeanInfo()
    {
        super( IntracellularProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "diagram" );
        property( "variables" ).hidden( "isDiagramNull" )
                .childDisplayName( beanClass.getMethod( "getPhenotypeVariableTitle", new Class[] {Integer.class, Object.class} ) ).add();
        property( "engine" ).hidden( "isDiagramNull" ).add();
    }
}