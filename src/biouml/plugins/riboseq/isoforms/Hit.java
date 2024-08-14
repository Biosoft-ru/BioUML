package biouml.plugins.riboseq.isoforms;

public class Hit
{
    public Hit(int pos, int transcriptId, boolean forwardStrand)
    {
        this.pos = pos;
        this.transcriptId = transcriptId;
        this.forwardStrand = forwardStrand;
    }
    
    Hit()
    {}

    int pos;
    //zero based pos of 5' read end
    public int getPos()
    {
        return pos;
    }
    
    int transcriptId;
    //The first transcript is 0
    public int getTranscriptId()
    {
        return transcriptId;
    }
    
    boolean forwardStrand;
    public boolean isForwardStrand()
    {
        return forwardStrand;
    }
}
