package biouml.plugins.microarray;

import java.util.List;

public class MicroarrayLink
{
    /**
     * Link to microarray
     */
    private String microarray;
    public String getMicroarray()
    {
        return microarray;
    }
    public void setMicroarray(String microarray)
    {
        this.microarray = microarray;
    }
    
    /**
     * List of genes
     */
    private List<String> genes;
    public List<String> getGenes()
    {
        return genes;
    }
    public void setGenes(List<String> genes)
    {
        this.genes = genes;
    }
    
    /**
     *  Comment from gene hub
     */
    private String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }
}
