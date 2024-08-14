package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MultipleUndirectedConnectionBeanInfo extends BeanInfoEx2<MultipleUndirectedConnection>
{
    public MultipleUndirectedConnectionBeanInfo()
    {
        super(MultipleUndirectedConnection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("connections");
    }
}
