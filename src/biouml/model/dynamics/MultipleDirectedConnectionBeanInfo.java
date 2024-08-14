package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class MultipleDirectedConnectionBeanInfo extends BeanInfoEx2<MultipleDirectedConnection>
{
    public MultipleDirectedConnectionBeanInfo()
    {
        super(MultipleDirectedConnection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("connections");
    }
}
