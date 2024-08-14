package biouml.standard;

import biouml.standard.type.Base;
import biouml.workbench.graphsearch.QueryEngineSupport;

public abstract class PathwayQueryEngine extends QueryEngineSupport
{
    final public static int MAX_REACTIONS_VALUE = 20;

    protected boolean isSmallMolecule(Base base, Object obj)
    {
        Integer num;
        try
        {
            num = (Integer)obj;
        }
        catch( Exception e )
        {
            return false;
        }

        int n = num.intValue();
        if( n > MAX_REACTIONS_VALUE )
        {
            return true;
        }

        return false;
    }
}
