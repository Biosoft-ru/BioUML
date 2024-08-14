package biouml.plugins.riboseq.isoforms;

public class TranscriptSequence
{
    String name;
    byte[] seq;
    
    public TranscriptSequence(String name, byte[] seq)
    {
        this.name = name;
        this.seq = seq;
    }
}
