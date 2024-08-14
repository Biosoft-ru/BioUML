package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class Evidence extends Concept
{
    public Evidence(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private Confidence confidence;
    
    public Confidence getConfidence()
    {
        return confidence;
    }

    public void setConfidence(Confidence confidence)
    {
        this.confidence = confidence;
    }
    
    private String evidenceCode;

    public String getEvidenceCode()
    {
        return evidenceCode;
    }

    public void setEvidenceCode(String evidenceCode)
    {
        this.evidenceCode = evidenceCode;
    }
    
}
