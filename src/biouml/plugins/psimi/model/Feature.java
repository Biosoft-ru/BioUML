package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Feature extends Concept
{
    public Feature(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    protected Concept featureType;
    protected Concept featureDetectionMethod;
    protected Integer[] experimentRefList;
    protected Concept[] featureRangeList;
    
    public Integer[] getExperimentRefList()
    {
        return experimentRefList;
    }

    public void setExperimentRefList(Integer[] experimentRefList)
    {
        this.experimentRefList = experimentRefList;
    }

    public Concept getFeatureDetectionMethod()
    {
        return featureDetectionMethod;
    }

    public void setFeatureDetectionMethod(Concept featureDetectionMethod)
    {
        this.featureDetectionMethod = featureDetectionMethod;
    }

    public Concept[] getFeatureRangeList()
    {
        return featureRangeList;
    }

    public void setFeatureRangeList(Concept[] featureRangeList)
    {
        this.featureRangeList = featureRangeList;
    }

    public Concept getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType(Concept featureType)
    {
        this.featureType = featureType;
    }
}
