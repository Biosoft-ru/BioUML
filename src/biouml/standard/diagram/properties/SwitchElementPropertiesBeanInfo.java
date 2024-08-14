package biouml.standard.diagram.properties;

import ru.biosoft.util.bean.BeanInfoEx2;

public class SwitchElementPropertiesBeanInfo extends BeanInfoEx2<SwitchElementProperties>
{
    public SwitchElementPropertiesBeanInfo()
    {
        super( SwitchElementProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "name" );       
    }
}