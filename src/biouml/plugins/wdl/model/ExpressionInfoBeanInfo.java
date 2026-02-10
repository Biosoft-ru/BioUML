package biouml.plugins.wdl.model;

import com.developmentontheedge.beans.BeanInfoEx;

public class ExpressionInfoBeanInfo extends BeanInfoEx
{
    public ExpressionInfoBeanInfo()
    {
        super( ExpressionInfo.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("name");
        add("type");
        add("expression");
    }
}
