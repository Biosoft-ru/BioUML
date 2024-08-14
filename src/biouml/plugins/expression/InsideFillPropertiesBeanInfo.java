package biouml.plugins.expression;

import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.ColorEditor;

/**
 * @author lan
 *
 */
public class InsideFillPropertiesBeanInfo extends BeanInfoEx2<InsideFillProperties>
{
    public InsideFillPropertiesBeanInfo()
    {
        super(InsideFillProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(ColumnNameSelector.registerNumericSelector("column", beanClass, "table"));
        add("min");
        add("color1", ColorEditor.class);
        add("max");
        add("color2", ColorEditor.class);
        add("useZeroColor");
        property( "colorZero" ).hidden( "isZeroHidden" ).editor( ColorEditor.class ).add();
        property( "zeroLevel" ).hidden( "isZeroHidden" ).add();
        property( "colorNan" ).editor( ColorEditor.class ).add();
        addHidden(new PropertyDescriptorEx("table", beanClass, "getTable", null));
    }
}
