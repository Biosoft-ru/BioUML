package biouml.plugins.expression;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.table.columnbeans.ColumnNameSelector;

/**
 * @author lan
 *
 */
public class PvalPropertiesBeanInfo extends BeanInfoEx
{
    public PvalPropertiesBeanInfo()
    {
        super( PvalProperties.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(ColumnNameSelector.registerNumericSelector("column", beanClass, "table"));
        add("cutoff1");
        add("cutoff2");
        add("cutoff3");
        addHidden(new PropertyDescriptorEx("table", beanClass, "getTable", null));
    }
}
