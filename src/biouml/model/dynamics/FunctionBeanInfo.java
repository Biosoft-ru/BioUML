package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.util.FormulaEditor;

/**
 * BeanInfo for {@link Function} role
 */
public class FunctionBeanInfo extends BeanInfoEx2<Function>
{
    public FunctionBeanInfo()
    {
        super(Function.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("name");
        add("rightHandSide", FormulaEditor.class);
        add("formula", FormulaEditor.class);
        add("comment");        
    }
}