package biouml.plugins.biopax.model;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import biouml.standard.type.Concept;

/**
 * Class to cover EntityFeature class of BioPAX level 3
 *
 */
public class EntityFeature extends Concept
{

    public EntityFeature(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
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

    private Concept[] featureLocation;
    public void setFeatureLocation(Concept[] featureLocations)
    {
        featureLocation = featureLocations;

    }
    public Concept[] getFeatureLocation()
    {
        return featureLocation;
    }

    private EntityFeature[] memberFeature;
    public EntityFeature[] getMemberFeature()
    {
        return memberFeature;
    }
    public void setMemberFeature(EntityFeature[] members)
    {
        memberFeature = members;
    }

    @Override
    public String toString()
    {
        String location = ( featureLocation == null ) ? "" : StreamEx.of( featureLocation ).map( Concept::getName ).joining( "\n\t" );
        String type = ( featureType == null ) ? "" : DataElementPath.create(featureType).getDataElement(OpenControlledVocabulary.class).getTerm();
        return name + ", type: " + type + ", location [" + location + "]";
    }
}
