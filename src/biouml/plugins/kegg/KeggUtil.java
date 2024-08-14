package biouml.plugins.kegg;

import java.util.HashSet;
import java.util.Set;

import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class KeggUtil
{
    /**
     * Method counts number of modifiers which will be represented on the diagram
     */
    public static int getModifiers(Node node)
    {
        Reaction reaction = (Reaction)node.getKernel();
        int result = 0;
        DatabaseReference[] dbrefs = reaction.getDatabaseReferences();
        if( dbrefs != null )
        {
            for( int i = 0; i < dbrefs.length; i++ )
            {
                //TODO: remove this HACK
                if( dbrefs[i].getDatabaseName().equals("MIR:00000004") && dbrefs[i].getId().indexOf(":") == -1 )
                {
                    result++;
                    if( result >= 4 )
                        return result;
                }
            }
        }

        Set<String> titles = new HashSet<>();
        if( result == 0 ) //no db ref found
        {
            for( Edge edge : node.edges().filter(e -> e.getKernel() instanceof SpecieReference) )
            {
                if( ( (SpecieReference)edge.getKernel() ).isReactantOrProduct() )
                    continue;
                Node modifier = edge.getOtherEnd(node);

                String titleStr = modifier.getAttributes().getValueAsString("EC");

                if( titleStr != null && !titleStr.isEmpty())
                {
                    if( titles.contains(titleStr) )
                        continue;

                    titles.add(titleStr);
                }

                result++;
                if( result >= 4 )
                    break;
            }
        }
        return result;
    }
}
