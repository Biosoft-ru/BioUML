package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.util.FormulaEditor;

/**
 * Bean info for {@link Event} role
 */
public class EventBeanInfo extends BeanInfoEx2<Event>
{
    public EventBeanInfo()
    {
        super(Event.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("trigger", FormulaEditor.class);
        add("priority", FormulaEditor.class);
        add("useValuesFromTriggerTime");
        add("triggerPersistent");
        add("triggerInitialValue");
        add("delay");
        add("timeUnits");
        add("triggerMessage");
        add("eventAssignment");
        add("comment");
    }
}
