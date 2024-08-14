package ru.biosoft.analysiscore;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class AnalysesGroupBeanInfo extends BeanInfoEx
{
    public AnalysesGroupBeanInfo()
    {
        super( AnalysesGroup.class, true );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null));
        add("description");
        add("related");
    }
}
