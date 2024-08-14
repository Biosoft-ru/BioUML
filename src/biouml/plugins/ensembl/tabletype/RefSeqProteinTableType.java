package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-refseq.gif")
public class RefSeqProteinTableType extends ProteinTableType
{
    @Override
    public String getSource()
    {
        return "RefSeq";
    }

    @Override
    public int getIdScore(String id)
    {
        if( id.matches("(AP|NP|XP|YP)\\_\\d{6}(\\.\\d+|)")
                || id.matches("(NP|XP|YP)\\_\\d{9}(\\.\\d+|)") || id.matches("ZP\\_\\d{8}(\\.\\d+|)"))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
