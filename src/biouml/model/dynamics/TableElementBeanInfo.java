package biouml.model.dynamics;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import biouml.model.dynamics.TableElement.SplineType;
import ru.biosoft.util.bean.BeanInfoEx2;

public class TableElementBeanInfo extends BeanInfoEx2<TableElement>
{
    public TableElementBeanInfo() throws Exception
    {
        super(TableElement.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("variables", beanClass,"getVariables()", null );
        pde.setReadOnly( true );
        add(pde);
        add("tablePath");
        add("formula");
        add("cycled");
        add("splineType", SplineTypeEditor.class);
    }

    public static class SplineTypeEditor extends StringTagEditorSupport
    {
        public SplineTypeEditor()
        {
			super(SplineType.getSplineTypes().toArray(new String[0]));
        }
    }
}