package biouml.plugins.virtualcell.core;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;

/**
 * Base class for biological entities.
 */
public class Entity extends DataElementSupport 
{
    /**
     *  Constructs data element.
     *
     *  @param name name of the entity
     *  @param pull pull to which this entity belongs
     */
    public Entity(String name, DataCollection<?> pull)
    {
    	super(name, pull);
    }

}
