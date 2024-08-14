package biouml.model.dynamics;

import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author tolstyh
 * BeanInfo for {@link State} role
 */
public class StateBeanInfo extends BeanInfoEx2<State>
{
    public StateBeanInfo()
    {
        super(State.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("start");
        add("onEntryAssignment");
        add("onExitAssignment");
    }
}
