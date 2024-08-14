package biouml.plugins.research;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DataElementRefBeanInfo extends BeanInfoEx2<DataElementRef>
{
    public DataElementRefBeanInfo()
    {
        super(DataElementRef.class);
    }


    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerOutput("path", beanClass, null));
    }
}