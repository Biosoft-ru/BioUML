package biouml.standard.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;

/**
 * Biological process is a biological goal that requires more than one function.
 * Examples of broad biological proceses are: "cell growth and maintenance",
 * "signal transduction", examples of more specific processes are:
 * "pirimidine metabolism" or "cAMP biosynthesis".
 *
 * @pending define its specific properties.
 */
@ClassIcon( "resources/process.gif" )
public class Process extends Concept
{
    public Process(DataCollection parent, String name)
    {
        super(parent, name);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Properties
    //

    @Override
    public String getType()
    {
        return TYPE_PROCESS;
    }

    // @todo
}