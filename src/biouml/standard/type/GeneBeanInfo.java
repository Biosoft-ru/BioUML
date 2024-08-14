package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class GeneBeanInfo extends BiopolymerBeanInfo
{
    public GeneBeanInfo()
    {
        super(Gene.class);
    }

    protected GeneBeanInfo(Class<? extends Gene> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
    }
}
