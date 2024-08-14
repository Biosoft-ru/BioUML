package biouml.plugins.sabiork;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Substance;

/**
 * Utility functions
 */
public class SabiorkUtility
{
    public static final String PROTEIN_DC = "protein";
    public static final String SUBSTANCE_DC = "substance";
    public static final String REACTION_DC = "reaction";

    public static DataCollection getDataCollection(DataElement de, String name) throws Exception
    {
        DataElement data = Module.getModule(de).get(Module.DATA);
        if( data instanceof DataCollection )
        {
            DataElement result = ( (DataCollection)data ).get(name);
            if( result instanceof DataCollection )
            {
                return (DataCollection)result;
            }
        }
        return null;
    }

    public static ServiceProvider getServiceProvider(String className)
    {
        if( Diagram.class.getName().equals(className) )
        {
            return new PathwayProvider();
        }
        else if( Substance.class.getName().equals(className) )
        {
            return new SubstanceProvider();
        }
        else if( Protein.class.getName().equals(className) )
        {
            return new ProteinProvider();
        }
        else if( Reaction.class.getName().equals(className) )
        {
            return new ReactionProvider();
        }
        return null;
    }
}
