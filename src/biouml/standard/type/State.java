package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;

/**
 * State, stage or variant of biological system or its subunits.
 */
public class State extends Concept
{
    public State(DataCollection parent, String name)
    {
        super(parent, name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    @Override
    public String getType()
    {
        return TYPE_STATE;
    }

    // @todo
}