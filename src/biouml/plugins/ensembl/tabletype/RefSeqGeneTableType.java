package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-refseq.gif")
public class RefSeqGeneTableType extends GeneTableType
{
    @Override
    public String getSource()
    {
        return "RefSeq";
    }

    @Override
    public int getIdScore(String id)
    {
        if( id.matches("(AC|NC|NG|NS|NT|NW)\\_\\d{6}(\\.\\d+|)")
                || id.matches("NW\\_\\d{9}(\\.\\d+|)")
                || id.matches("NZ\\_[A-Z]{4}\\d{8}(\\.\\d+|)") )
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
