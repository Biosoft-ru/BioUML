package biouml.plugins.wdl.diagram;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ExpressionPropertiesBeanInfo extends BeanInfoEx2<ExpressionProperties>
{
    public ExpressionPropertiesBeanInfo()
    {
        super( ExpressionProperties.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("type");
        add("variable");
        add("rhs");
    }
}