package ru.biosoft.bsastats;

import ru.biosoft.util.bean.BeanInfoEx2;

public class TaskProcessorBeanInfo extends BeanInfoEx2<TaskProcessor>
{
    protected TaskProcessorBeanInfo(Class<? extends TaskProcessor> beanClass)
    {
        super( beanClass );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add("enabled");
   }
}
