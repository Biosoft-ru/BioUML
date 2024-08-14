package biouml.plugins.expression;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.ColorEditor;

import ru.biosoft.table.columnbeans.ColumnNamesSelector;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 */
public class OutsideFillPropertiesBeanInfo extends BeanInfoEx2<OutsideFillProperties>
{
    public OutsideFillPropertiesBeanInfo()
    {
        super(OutsideFillProperties.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(ColumnNamesSelector.registerNumericSelector("columns", beanClass, "table"));
        add("fillWidth");
        add("min");
        add("color1", ColorEditor.class);
        add("max");
        add("color2", ColorEditor.class);
        add("useZeroColor");
        property( "colorZero" ).hidden( "isZeroHidden" ).editor( ColorEditor.class ).add();
        property( "zeroLevel" ).hidden( "isZeroHidden" ).add();
        property( "colorNan" ).editor( ColorEditor.class ).add();
        addHidden(new PropertyDescriptorEx("table", beanClass, "getTable", null));
        add( "useGradientFill" );
    }
}
