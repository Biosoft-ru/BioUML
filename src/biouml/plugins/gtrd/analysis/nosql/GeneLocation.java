package biouml.plugins.gtrd.analysis.nosql;

public class GeneLocation implements Cloneable
{
    String ensemblGeneId;
    String geneSymbol;
    String chr;
    int from, to;//one based both inclusive
    
    @Override
    public GeneLocation clone()
    {
        try
        {
            return (GeneLocation)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            throw new AssertionError();
        }
    }
}
