package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.TranscriptTableType;

@ClassIcon("resources/transcripts-ensembl.gif")
public class EnsemblTranscriptTableType extends TranscriptTableType implements EnsemblSpeciesPredictor
{
    @Override
    public String getSource()
    {
        return "Ensembl";
    }

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("ENST\\d{11}(\\.\\d++)?") || id.matches("ENS[A-Z]{3}T\\d{11}(\\.\\d++)?")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSampleID()
    {
        return "ENST00000380152";
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
