package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-uniprot.gif")
public class UniprotProteinTableType extends ProteinTableType
{
    @Override
    public String getSource()
    {
        return "UniProt";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("([A-NR-Z][0-9][A-Z][A-Z0-9][A-Z0-9][0-9]|[OPQ][0-9][A-Z0-9][A-Z0-9][A-Z0-9][0-9])(_[A-Z]+|\\-\\d+|)")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
    
    @Override
    public String getURL(String id)
    {
        return "https://www.uniprot.org/uniprot/" + id;
    }
}
