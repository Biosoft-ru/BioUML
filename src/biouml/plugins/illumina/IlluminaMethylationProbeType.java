package biouml.plugins.illumina;

import ru.biosoft.analysis.type.ProbeTableType;

public class IlluminaMethylationProbeType extends ProbeTableType
{
    @Override
    public int getIdScore(String id)
    {
        if(id.matches("cg\\d{8}")) return SCORE_HIGH_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }
    
    @Override
    public String getObjectType()
    {
        return "Methylation probes";
    }
    
    @Override
    public String getSource()
    {
        return "Illumina";
    }
}
