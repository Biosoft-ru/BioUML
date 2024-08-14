package biouml.plugins.riboseq.comparison_article.util_data_structure;

public class GeneralPointInfo
{
    public final int point;
    public final String chrName;
    public final boolean strandPlus;
    public final int length;

    public GeneralPointInfo(int point, String chrName, boolean strandPlus, int length)
    {
        this.point = point;
        this.chrName = chrName;
        this.strandPlus = strandPlus;
        this.length = length;
    }

    public GeneralPointInfo(int point, String chrName, boolean strandPlus)
    {
        this.point = point;
        this.chrName = chrName;
        this.strandPlus = strandPlus;
        this.length = 0;
    }
}
