package biouml.plugins.mirbase;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

//Union of MiRBaseStemLoopMiRNA and MiRBaseMatureMiRNA, usefull for affymetrix miRNA arrays that have probes for both stem-loops and mature miRNAs.
public class MiRBaseName extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        if( id.contains( "-mir-" ) || id.contains( "-let-" ) || id.contains( "-miR-" ) )
            return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "miRNA";
    }
    
    @Override
    public String getSource()
    {
        return "miRBase";
    }

}
