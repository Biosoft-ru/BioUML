package ru.biosoft.bsastats;

public class AdapterMatch
{
    private int offset;
    private double score;

    public AdapterMatch(int offset, double score)
    {
        super();
        this.offset = offset;
        this.score = score;
    }

    /**
     * Offset in sequence where adapter starts or sequence.length if no match found
     */
    public int getOffset()
    {
        return offset;
    }
    
    /**
     * Score of alignment
     */
    public double getScore()
    {
        return score;
    }
    
}
