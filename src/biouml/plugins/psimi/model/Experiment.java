package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Experiment extends Concept
{
    public Experiment(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    protected Concept interactionDetectionMethod;
    protected Concept participantIdentificationMethod;
    protected Concept featureDetectionMethod;
    protected Organism[] hostOrganismList;
    protected Confidence[] confidenceList;
    
    public Concept getFeatureDetectionMethod()
    {
        return featureDetectionMethod;
    }

    public void setFeatureDetectionMethod(Concept featureDetectionMethod)
    {
        this.featureDetectionMethod = featureDetectionMethod;
    }

    public Concept getInteractionDetectionMethod()
    {
        return interactionDetectionMethod;
    }

    public void setInteractionDetectionMethod(Concept interactionDetectionMethod)
    {
        this.interactionDetectionMethod = interactionDetectionMethod;
    }

    public Concept getParticipantIdentificationMethod()
    {
        return participantIdentificationMethod;
    }

    public void setParticipantIdentificationMethod(Concept participantIdentificationMethod)
    {
        this.participantIdentificationMethod = participantIdentificationMethod;
    }

    public Organism[] getHostOrganismList()
    {
        return hostOrganismList;
    }

    public void setHostOrganismList(Organism[] hostOrganismList)
    {
        this.hostOrganismList = hostOrganismList;
    }

    public Confidence[] getConfidenceList()
    {
        return confidenceList;
    }

    public void setConfidenceList(Confidence[] confidenceList)
    {
        this.confidenceList = confidenceList;
    }
    
}
