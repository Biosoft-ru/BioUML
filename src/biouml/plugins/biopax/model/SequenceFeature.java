package biouml.plugins.biopax.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

public class SequenceFeature extends Concept
{
    public SequenceFeature(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private SequenceInterval featureLocation;
    
    public SequenceInterval getFeatureLocation()
    {
        return featureLocation;
    }

    public void setFeatureLocation(SequenceInterval featureLocation)
    {
        this.featureLocation = featureLocation;
    }

    private String featureType;
    
    public String getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType(String featureType)
    {
        this.featureType = featureType;
    }
    
}
