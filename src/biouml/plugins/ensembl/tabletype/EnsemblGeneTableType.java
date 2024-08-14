package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.GeneTableType;

@ClassIcon("resources/genes-ensembl.gif")
public class EnsemblGeneTableType extends GeneTableType implements EnsemblSpeciesPredictor
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches("ENSG\\d{11}(\\.\\d++)?") || id.matches("ENS[A-Z]{3}G\\d{11}(\\.\\d++)?")) return SCORE_HIGH_SPECIFIC;
        if(id.matches( "FBgn[0-9]{7}" ))
            return SCORE_HIGH_SPECIFIC;//drosophila id format
        if(id.matches( "AT[1-9CM]G[0-9]{5}" ) )//arabidopsis thaliana, aslo has not specific "ENSRNA[0-9]{9}"
            return SCORE_HIGH_SPECIFIC;
        if(id.matches( "WBGene[0-9]{8}" ))//nematoda
            return SCORE_HIGH_SPECIFIC;
        
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "Ensembl";
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
