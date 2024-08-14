package biouml.plugins.simulation.document;

import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author axec
 *
 */
public class InputParameterBeanInfo extends BeanInfoEx2<InputParameter>
{
    public InputParameterBeanInfo()
    {
        super( InputParameter.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name");
        add("value");       
        add("valueStep");
        addReadOnly( "defaultValue" );
        addReadOnly( "title");
        addReadOnly( "type" );
    }
}