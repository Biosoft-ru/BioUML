package biouml.plugins.riboseq.ingolia.asite;

public class ASiteOffsetBuilderParameters
{
    private int transcriptOverhangs;
    private boolean strandSpecific;

    public ASiteOffsetBuilderParameters(BuildASiteOffsetTableParameters parameters)
    {
        transcriptOverhangs = parameters.getTranscriptOverhangs();
        strandSpecific = parameters.isStrandSpecific();
    }

    public ASiteOffsetBuilderParameters(int transcriptOverhangs, boolean strandSpecific)
    {
        this.transcriptOverhangs = transcriptOverhangs;
        this.strandSpecific = strandSpecific;
    }

    public int getTranscriptOverhangs()
    {
        return transcriptOverhangs;
    }

    public boolean isStrandSpecific()
    {
        return strandSpecific;
    }
}
