package biouml.plugins.illumina;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.TranscriptTableType;

@ClassIcon("resources/transcripts-illumina.gif")
public class IlluminaTranscriptType extends TranscriptTableType
{

    @Override
    public int getIdScore(String id)
    {
        if(id.matches("ILMN_\\d{1,6}")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getSource()
    {
        return "Illumina";
    }
}
