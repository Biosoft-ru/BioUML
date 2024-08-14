package biouml.model.dynamics;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.model.util.FormulaEditor;

/**
 * @author tolstyh
 * BeanInfo for assignment object
 */
public class AssignmentBeanInfo extends BeanInfoEx
{
    public AssignmentBeanInfo()
    {
        super( Assignment.class, true );
    }

    @Override
    public void initProperties() throws Exception
    {
        add("variable");
        add("math", FormulaEditor.class);
    }
}
