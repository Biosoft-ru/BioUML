package biouml.plugins.mirbase;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

//Union of MiRBaseStemLoopMiRNA and MiRBaseMatureMiRNA, usefull for affymetrix miRNA arrays that have probes for both stem-loops and mature miRNAs.
public class MiRBaseMixture extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        if( id.matches( "MI\\d{7}" ) || id.matches( "MIMAT\\d{7}" ) || id.contains( "-mir-" ) || id.contains( "-let-" ) )
            return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "mixture-miRNA";
    }
    
    @Override
    public String getSource()
    {
        return "miRBase";
    }

}
