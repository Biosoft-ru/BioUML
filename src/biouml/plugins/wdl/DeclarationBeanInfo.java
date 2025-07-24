package biouml.plugins.wdl;

import com.developmentontheedge.beans.BeanInfoEx;

public class DeclarationBeanInfo extends BeanInfoEx
{
    public DeclarationBeanInfo()
    {
        super( Declaration.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add("type");
        add("expression");
    }
}
