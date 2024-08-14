package biouml.plugins.reactome;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-reactome.gif")
public class ReactomeProteinTableType extends ProteinTableType
{
    @Override
    public String getSource()
    {
        return "Reactome";
    }

    @Override
    public int getIdScore(String id)
    {
        if( ReactomeIDMatcher.matches( id ) )
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
