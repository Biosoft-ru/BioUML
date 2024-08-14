package biouml.plugins.riboseq.comparison_article.util_data_structure;

import java.util.List;

public class ArticleGenePointAdditionInfo
{
    private static final String CHR_PREFIX = "chr";

    public final String chrName;
    public final boolean strandPlus;
    public final int exonCount;
    public final List<Integer> exonStarts;
    public final List<Integer> exonEnds;

    public ArticleGenePointAdditionInfo(String chrName, boolean strandPlus, int exonCount, List<Integer> exonStarts, List<Integer> exonEnds)
    {
        this.strandPlus = strandPlus;
        this.exonCount = exonCount;
        this.exonStarts = exonStarts;
        this.exonEnds = exonEnds;

        if( chrName.startsWith( CHR_PREFIX ) )
        {
            this.chrName = chrName;
        }
        else
        {
            this.chrName = CHR_PREFIX + chrName;
        }
    }
}
