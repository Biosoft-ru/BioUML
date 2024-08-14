package biouml.plugins.expression;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FluxPropertiesBeanInfo extends BeanInfoEx2<FluxProperties>
{
    public FluxPropertiesBeanInfo()
    {
        super(FluxProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(ColumnNameSelector.registerNumericSelector("column", beanClass, "table"));
        add("min");
        add("max");
        add("maxWidth");
        addHidden(new PropertyDescriptorEx("table", beanClass, "getTable", null));
    }
}
