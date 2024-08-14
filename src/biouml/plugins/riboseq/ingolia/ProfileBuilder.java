package biouml.plugins.riboseq.ingolia;

import java.util.List;
import java.util.Map;

import biouml.plugins.riboseq.ingolia.asite.ASiteOffsetBuilder;

public class ProfileBuilder
{
    private boolean onlyPositiveStrand = true;
    private Map<Integer, Integer> aSiteOffsetTable = ASiteOffsetBuilder.STANDARD_A_SITE_OFFSET_TABLE;
    
    public boolean isOnlyPositiveStrand()
    {
        return onlyPositiveStrand;
    }
    public void setOnlyPositiveStrand(boolean onlyPositiveStrand)
    {
        this.onlyPositiveStrand = onlyPositiveStrand;
    }
    
    public Map<Integer, Integer> getASiteOffsetTable()
    {
        return aSiteOffsetTable;
    }
    public void setASiteOffsetTable(Map<Integer, Integer> aSiteOffsetTable)
    {
        this.aSiteOffsetTable = aSiteOffsetTable;
    }
    
    public int[] computeProfile(List<AlignmentOnTranscript> aligns, int transcriptLength)
    {
        int[] result = new int[transcriptLength];
        for(AlignmentOnTranscript a : aligns)
        {
            if(onlyPositiveStrand && !a.isPositiveStrand())
                continue;
            Integer aSiteOffset = aSiteOffsetTable.get( a.getLength() );
            if(aSiteOffset == null)
                continue;
            int aSitePosition;
            if(a.isPositiveStrand())
                aSitePosition = a.getFrom() + aSiteOffset;
            else
                aSitePosition = a.getTo() - aSiteOffset;
            if(aSitePosition < 0 || aSitePosition >= transcriptLength)
                continue;
            result[aSitePosition]++;
        }
        return result;
    }
    
}
