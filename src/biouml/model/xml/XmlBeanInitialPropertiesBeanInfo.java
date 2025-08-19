package biouml.model.xml;

import com.developmentontheedge.beans.BeanInfoEx;

public class XmlBeanInitialPropertiesBeanInfo extends BeanInfoEx
{

    public XmlBeanInitialPropertiesBeanInfo()
    {
        super( XmlBeanInitialProperties.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        addReadOnly( "name", "isNameReadOnly" );
        add( "attributes" );
    }

}
