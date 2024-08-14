package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.util.FormulaEditor;

import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class EquationBeanInfo extends BeanInfoEx2<Equation>
{
    public EquationBeanInfo()
    {
        super(Equation.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        addHidden("variable", "isAlgebraic");
        add("formula", FormulaEditor.class);        
        add("type", EquationTypeEditor.class);
        add("comment");
    }

    public static class EquationTypeEditor extends StringTagEditorSupport
    {
        public EquationTypeEditor()
        {
            super(new String[] {Equation.TYPE_RATE, Equation.TYPE_SCALAR, Equation.TYPE_ALGEBRAIC, Equation.TYPE_INITIAL_ASSIGNMENT});
        }
    }
}
