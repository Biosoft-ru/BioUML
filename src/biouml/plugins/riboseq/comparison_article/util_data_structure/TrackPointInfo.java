package biouml.plugins.riboseq.comparison_article.util_data_structure;

public class TrackPointInfo
{
    public int point;

    public final String chrName;
    public final boolean strandPlus;
    public final int clusterLength;

    public TrackPointInfo(String chrName, int initCodonPosition, boolean strandPlus, int clusterLength)
    {
        this.chrName = chrName;
        this.point = initCodonPosition;
        this.strandPlus = strandPlus;
        this.clusterLength = clusterLength;
    }

    public TrackPointInfo(String chrName, int initCodonPosition, boolean strandPlus)
    {
        this.chrName = chrName;
        this.point = initCodonPosition;
        this.strandPlus = strandPlus;
        this.clusterLength = 0;
    }
}
