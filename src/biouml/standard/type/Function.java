package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * Molecular function is an activity or task performed by a gene product or other
 * biological entity. It often corresponds to something (such as enzymatic activity)
 * that can be measured <i>in vitro</i>.
 *
 * @pending define its specific properties.
 */
@ClassIcon( "resources/function.gif" )
public class Function extends Concept
{
    public Function(DataCollection parent, String name)
    {
        super(parent, name);
    }

    @Override
    public String getType()
    {
        return TYPE_FUNCTION;
    }
}