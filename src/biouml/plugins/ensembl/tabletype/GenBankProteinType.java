package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon ( "resources/proteins-genbank.gif" )
public class GenBankProteinType extends ProteinTableType
{
    @Override
    public String getSource()
    {
        return "GenBank";
    }

    @Override
    public int getIdScore(String id)
    {
        // According to http://www.ncbi.nlm.nih.gov/Sequin/acc.html
        if( id.matches("[A-EHI][A-Z][A-Z]\\d{5}(\\.\\d+|)") )
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
