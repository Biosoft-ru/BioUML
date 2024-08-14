package biouml.plugins.ensembl.tabletype;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.type.TranscriptTableType;

@ClassIcon("resources/transcripts-refseq.gif")
public class RefSeqTranscriptTableType extends TranscriptTableType
{
    @Override
    public String getSource()
    {
        return "RefSeq";
    }

    @Override
    public int getIdScore(String id)
    {
        if( id.matches("(NM|NR|XM|XR)\\_\\d{6}(\\.\\d+|)")
                || id.matches("(NM|XM)\\_\\d{9}(\\.\\d+|)"))
            return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
}
