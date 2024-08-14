package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.util.FormulaEditor;

/**
 * BeanInfo for {@link Transition} role
 */
public class TransitionBeanInfo extends BeanInfoEx2<Transition>
{
    public TransitionBeanInfo()
    {
        super(Transition.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("when", FormulaEditor.class);
        add("after", FormulaEditor.class);
        add("assignments");
    }
}
