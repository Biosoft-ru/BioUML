package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.util.FormulaEditor;

public class ConstraintBeanInfo extends BeanInfoEx2<Constraint>
{
    public ConstraintBeanInfo()
    {
        super(Constraint.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("formula", FormulaEditor.class);
        add("message");
        add("comment");
    }
}
