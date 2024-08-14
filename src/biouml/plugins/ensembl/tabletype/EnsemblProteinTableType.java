package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.ProteinTableType;

@ClassIcon("resources/proteins-ensembl.gif")
public class EnsemblProteinTableType extends ProteinTableType implements EnsemblSpeciesPredictor
{
    @Override
    public String getSource()
    {
        return "Ensembl";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("ENSP\\d{11}(\\.\\d++)?") || id.matches("ENS[A-Z]{3}P\\d{11}(\\.\\d++)?")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSampleID()
    {
        return "ENSP00000439902";
    }

    @Override
    public String predictSpecies(String[] ids)
    {
        return predict( ids );
    }

    @Override
    public String preprocessId(String id)
    {
        return id.replaceFirst( "\\.\\d++", "" );
    }
}
