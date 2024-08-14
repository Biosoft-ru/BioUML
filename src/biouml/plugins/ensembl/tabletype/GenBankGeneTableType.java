package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon ( "resources/genes-genbank.gif" )
public class GenBankGeneTableType extends GeneTableType
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
        // C12345 are left for KEGG
        if( id.matches("[ABD-NR-Z]\\d{5}") || id.matches("[A-H][A-Z]\\d{6}") )
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
