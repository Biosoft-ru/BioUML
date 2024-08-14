package biouml.plugins.mirbase;

import ru.biosoft.access.biohub.ReferenceTypeSupport;

//miRBase accessions like MI0017425, MIMAT0019941
public class MiRBaseAccession extends ReferenceTypeSupport
{
    @Override
    public int getIdScore(String id)
    {
        if( id.matches( "MI\\d{7}" ) || id.matches( "MIMAT\\d{7}" ) )
            return SCORE_MEDIUM_SPECIFIC;
        return SCORE_NOT_THIS_TYPE;
    }

    @Override
    public String getObjectType()
    {
        return "accession-miRNA";
    }
    
    @Override
    public String getSource()
    {
        return "miRBase";
    }

}
