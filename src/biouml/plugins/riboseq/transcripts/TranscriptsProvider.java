package biouml.plugins.riboseq.transcripts;

import java.util.Collection;
import java.util.List;

public abstract class TranscriptsProvider
{
    public abstract List<Transcript> getTranscripts();
    
    protected boolean onlyProteinCoding;
    public boolean isOnlyProteinCoding()
    {
        return onlyProteinCoding;
    }
    public void setOnlyProteinCoding(boolean value)
    {
        this.onlyProteinCoding = value;
    }
    
    protected boolean loadCDS = true;
    public boolean isLoadCDS()
    {
        return loadCDS;
    }
    public void setLoadCDS(boolean value)
    {
        this.loadCDS = value;
    }
    
    protected Collection<String> subset;
    public Collection<String> getSubset()
    {
        return subset;
    }
    public void setSubset(Collection<String> subset)
    {
        this.subset = subset;
    }
}
