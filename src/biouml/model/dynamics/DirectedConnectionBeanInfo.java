package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DirectedConnectionBeanInfo extends BeanInfoEx2<DirectedConnection>
{
    public DirectedConnectionBeanInfo()
    {
        super(DirectedConnection.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("outputPort");
        add("inputPort");
        add("function");
    }
}
