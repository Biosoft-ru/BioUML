package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class Confidence extends Concept
{
    public Confidence(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private String confidenceValue;
    
    public String getConfidenceValue()
    {
        return confidenceValue;
    }

    public void setConfidenceValue(String confidenceValue)
    {
        this.confidenceValue = confidenceValue;
    }
    
}
